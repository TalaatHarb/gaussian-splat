package net.talaatharb.gsplat.service;

import javafx.beans.property.*;
import net.talaatharb.gsplat.model.AppSettings;
import net.talaatharb.gsplat.model.Project;
import net.talaatharb.gsplat.model.ReconstructionBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PipelineService {

    public enum PipelineStage {
        IDLE, FRAME_EXTRACTION, COLMAP_FEATURES, COLMAP_MATCHING, COLMAP_MAPPING, VGGT_RECONSTRUCTION, TRAINING, COMPLETE, ERROR
    }

    private final StringProperty currentStage = new SimpleStringProperty(PipelineStage.IDLE.name());
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    private FFmpegService ffmpegService;
    private ColmapService colmapService;
    private VggtService vggtService;
    private TrainingService trainingService;
    private ReconstructionBackend reconstructionBackend = ReconstructionBackend.COLMAP;
    private boolean vggtUseBundleAdjustment;

    private volatile boolean cancelled;

    public void configure(AppSettings settings) {
        ffmpegService = null;
        colmapService = null;
        vggtService = null;
        trainingService = null;
        reconstructionBackend = settings.getReconstructionBackend();
        vggtUseBundleAdjustment = settings.isVggtUseBundleAdjustment();

        if (hasText(settings.getFfmpegPath())) {
            ffmpegService = new FFmpegService(settings.getFfmpegPath());
        }
        if (hasText(settings.getColmapPath())) {
            colmapService = new ColmapService(settings.getColmapPath());
        }
        if (hasText(settings.getPythonPath()) && hasText(settings.getVggtRepoPath())) {
            vggtService = new VggtService(settings.getPythonPath(), settings.getVggtRepoPath());
            if (hasText(settings.getCondaPath())) vggtService.setCondaPath(settings.getCondaPath());
            if (hasText(settings.getCondaEnvName())) vggtService.setCondaEnvName(settings.getCondaEnvName());
            vggtService.setGpuDevice(settings.getGpuDevice());
        }
        if (hasText(settings.getPythonPath()) && hasText(settings.getGaussianSplattingRepoPath())) {
            trainingService = new TrainingService(settings.getPythonPath(), settings.getGaussianSplattingRepoPath());
            if (hasText(settings.getCondaPath())) trainingService.setCondaPath(settings.getCondaPath());
            if (hasText(settings.getCondaEnvName())) trainingService.setCondaEnvName(settings.getCondaEnvName());
            trainingService.setGpuDevice(settings.getGpuDevice());
        }
    }

    /**
     * Run the full pipeline: video→frames→COLMAP→training.
     * If the project has images instead of video, skips the FFmpeg step.
     */
    public CompletableFuture<Void> runFullPipeline(Project project, Consumer<String> logHandler) {
        cancelled = false;
        running.set(true);

        return CompletableFuture.runAsync(() -> {
            try {
                Path inputDir = project.getInputDir();
                Path colmapDir = project.getColmapDir();
                Path outputDir = project.getOutputDir();

                // Step 1: Frame extraction (if video input)
                if (project.getInputVideo() != null && !project.getInputVideo().isBlank()) {
                    if (ffmpegService == null) {
                        updateStage(PipelineStage.ERROR, 0, "FFmpeg is not configured.");
                        return;
                    }
                    updateStage(PipelineStage.FRAME_EXTRACTION, 0.0, "Extracting frames from video...");
                    if (cancelled) return;

                    Files.createDirectories(inputDir);
                    var result = ffmpegService.extractFrames(
                            project.getInputVideo(), inputDir, project.getVideoFps(), logHandler).join();

                    if (!result.isSuccess()) {
                        updateStage(PipelineStage.ERROR, 0, "Frame extraction failed.");
                        return;
                    }
                    project.setStatus(Project.Status.FRAMES_EXTRACTED);
                }

                if (cancelled) return;

                // Step 2: Reconstruction pipeline
                Files.createDirectories(colmapDir);
                stageReconstructionImages(inputDir, colmapDir, logHandler);
                prepareReconstructionOutputs(colmapDir);
                if (cancelled) return;

                ProcessOrchestrator.ProcessResult reconstructionResult = runReconstruction(colmapDir, logHandler);
                if (!reconstructionResult.isSuccess()) {
                    String backendName = reconstructionBackend == ReconstructionBackend.VGGT ? "VGGT" : "COLMAP";
                    updateStage(PipelineStage.ERROR, 0.2, backendName + " pipeline failed.");
                    return;
                }
                project.setStatus(Project.Status.COLMAP_COMPLETE);

                if (cancelled) return;

                // Step 3: Training
                if (trainingService == null) {
                    updateStage(PipelineStage.ERROR, 0.5, "Training is not configured.");
                    return;
                }
                updateStage(PipelineStage.TRAINING, 0.5, "Training Gaussian Splat model...");
                Files.createDirectories(outputDir);

                // The COLMAP output with images is the source for training
                var trainResult = trainingService.startTraining(
                        colmapDir, outputDir, project.getTrainingConfig(), logHandler).join();

                if (!trainResult.isSuccess()) {
                    updateStage(PipelineStage.ERROR, 0.5, "Training failed.");
                    return;
                }
                project.setStatus(Project.Status.TRAINED);

                updateStage(PipelineStage.COMPLETE, 1.0, "Pipeline complete!");

            } catch (Exception e) {
                updateStage(PipelineStage.ERROR, 0, "Pipeline error: " + e.getMessage());
                if (logHandler != null) logHandler.accept("ERROR: " + e.getMessage());
            } finally {
                running.set(false);
            }
        });
    }

    public void cancel() {
        cancelled = true;
        if (ffmpegService != null) ffmpegService.cancel();
        if (colmapService != null) colmapService.cancel();
        if (vggtService != null) vggtService.cancel();
        if (trainingService != null) trainingService.cancel();
    }

    private ProcessOrchestrator.ProcessResult runReconstruction(Path workspaceDir, Consumer<String> logHandler) throws IOException {
        return switch (reconstructionBackend) {
            case VGGT -> {
                if (vggtService == null) {
                    yield missingToolResult("VGGT is selected, but Python or the VGGT repository path is not configured.");
                }
                updateStage(PipelineStage.VGGT_RECONSTRUCTION, 0.2, "Running VGGT reconstruction...");
                var result = vggtService.runFullPipeline(workspaceDir, vggtUseBundleAdjustment, logHandler).join();
                if (result.isSuccess()) {
                    normalizeVggtSparseOutput(workspaceDir);
                }
                yield result;
            }
            case COLMAP -> {
                if (colmapService == null) {
                    yield missingToolResult("COLMAP is selected, but its executable path is not configured.");
                }
                updateStage(PipelineStage.COLMAP_FEATURES, 0.2, "Running COLMAP...");
                Files.createDirectories(workspaceDir.resolve("sparse"));
                yield colmapService.runFullPipeline(workspaceDir.resolve("images"), workspaceDir, logHandler).join();
            }
        };
    }

    private void stageReconstructionImages(Path inputDir, Path workspaceDir, Consumer<String> logHandler) throws IOException {
        Path imagesDir = workspaceDir.resolve("images");
        recreateDirectory(imagesDir);
        if (!Files.isDirectory(inputDir)) {
            throw new IOException("Project input directory does not exist: " + inputDir);
        }

        try (Stream<Path> files = Files.list(inputDir)) {
            var imageFiles = files.filter(Files::isRegularFile).sorted().toList();
            if (imageFiles.isEmpty()) {
                throw new IOException("No input images found in " + inputDir);
            }
            for (Path source : imageFiles) {
                Path target = imagesDir.resolve(source.getFileName().toString());
                try {
                    Files.createLink(target, source);
                } catch (IOException | UnsupportedOperationException e) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            if (logHandler != null) {
                logHandler.accept("Prepared " + imageFiles.size() + " image(s) in " + imagesDir);
            }
        }
    }

    private void normalizeVggtSparseOutput(Path workspaceDir) throws IOException {
        Path sparseDir = workspaceDir.resolve("sparse");
        Path sparseModelDir = sparseDir.resolve("0");
        if (Files.exists(sparseModelDir.resolve("cameras.bin"))) {
            return;
        }

        Path cameras = sparseDir.resolve("cameras.bin");
        if (!Files.exists(cameras)) {
            return;
        }

        Files.createDirectories(sparseModelDir);
        moveIfExists(sparseDir.resolve("cameras.bin"), sparseModelDir.resolve("cameras.bin"));
        moveIfExists(sparseDir.resolve("images.bin"), sparseModelDir.resolve("images.bin"));
        moveIfExists(sparseDir.resolve("points3D.bin"), sparseModelDir.resolve("points3D.bin"));
        moveIfExists(sparseDir.resolve("cameras.txt"), sparseModelDir.resolve("cameras.txt"));
        moveIfExists(sparseDir.resolve("images.txt"), sparseModelDir.resolve("images.txt"));
        moveIfExists(sparseDir.resolve("points3D.txt"), sparseModelDir.resolve("points3D.txt"));
    }

    private void prepareReconstructionOutputs(Path workspaceDir) throws IOException {
        Files.deleteIfExists(workspaceDir.resolve("database.db"));
        recreateDirectory(workspaceDir.resolve("sparse"));
    }

    private void moveIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.equals(directory)) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        }
        Files.createDirectories(directory);
    }

    private ProcessOrchestrator.ProcessResult missingToolResult(String message) {
        return new ProcessOrchestrator.ProcessResult(-1, "", message);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void updateStage(PipelineStage stage, double progress, String message) {
        javafx.application.Platform.runLater(() -> {
            currentStage.set(stage.name());
            overallProgress.set(progress);
            statusMessage.set(message);
        });
    }

    public StringProperty currentStageProperty() { return currentStage; }
    public DoubleProperty overallProgressProperty() { return overallProgress; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public BooleanProperty runningProperty() { return running; }
}
