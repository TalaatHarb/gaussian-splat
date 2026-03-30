package net.talaatharb.gsplat.service;

import javafx.beans.property.*;
import net.talaatharb.gsplat.model.AppSettings;
import net.talaatharb.gsplat.model.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PipelineService {

    public enum PipelineStage {
        IDLE, FRAME_EXTRACTION, COLMAP_FEATURES, COLMAP_MATCHING, COLMAP_MAPPING, TRAINING, COMPLETE, ERROR
    }

    private final StringProperty currentStage = new SimpleStringProperty(PipelineStage.IDLE.name());
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    private FFmpegService ffmpegService;
    private ColmapService colmapService;
    private TrainingService trainingService;

    private volatile boolean cancelled;

    public void configure(AppSettings settings) {
        if (settings.getFfmpegPath() != null) {
            ffmpegService = new FFmpegService(settings.getFfmpegPath());
        }
        if (settings.getColmapPath() != null) {
            colmapService = new ColmapService(settings.getColmapPath());
        }
        if (settings.getPythonPath() != null && settings.getGaussianSplattingRepoPath() != null) {
            trainingService = new TrainingService(settings.getPythonPath(), settings.getGaussianSplattingRepoPath());
            if (settings.getCondaPath() != null) trainingService.setCondaPath(settings.getCondaPath());
            if (settings.getCondaEnvName() != null) trainingService.setCondaEnvName(settings.getCondaEnvName());
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

                // Step 2: COLMAP pipeline
                updateStage(PipelineStage.COLMAP_FEATURES, 0.2, "Running COLMAP...");
                Files.createDirectories(colmapDir);
                Files.createDirectories(colmapDir.resolve("sparse"));

                var colmapResult = colmapService.runFullPipeline(inputDir, colmapDir, logHandler).join();
                if (!colmapResult.isSuccess()) {
                    updateStage(PipelineStage.ERROR, 0.2, "COLMAP pipeline failed.");
                    return;
                }
                project.setStatus(Project.Status.COLMAP_COMPLETE);

                if (cancelled) return;

                // Step 3: Training
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
        if (trainingService != null) trainingService.cancel();
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
