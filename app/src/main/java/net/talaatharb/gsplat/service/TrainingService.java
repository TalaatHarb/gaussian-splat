package net.talaatharb.gsplat.service;

import javafx.beans.property.*;
import net.talaatharb.gsplat.model.TrainingConfig;
import net.talaatharb.gsplat.util.LogParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TrainingService {

    private final ProcessOrchestrator orchestrator = new ProcessOrchestrator();
    private final IntegerProperty currentIteration = new SimpleIntegerProperty(0);
    private final DoubleProperty loss = new SimpleDoubleProperty(0);
    private final DoubleProperty psnr = new SimpleDoubleProperty(0);
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("");

    private String pythonPath;
    private String gsRepoPath;
    private String condaPath;
    private String condaEnvName;
    private int gpuDevice;

    public TrainingService(String pythonPath, String gsRepoPath) {
        this.pythonPath = pythonPath;
        this.gsRepoPath = gsRepoPath;
    }

    public void setPythonPath(String pythonPath) { this.pythonPath = pythonPath; }
    public void setGsRepoPath(String gsRepoPath) { this.gsRepoPath = gsRepoPath; }
    public void setCondaPath(String condaPath) { this.condaPath = condaPath; }
    public void setCondaEnvName(String condaEnvName) { this.condaEnvName = condaEnvName; }
    public void setGpuDevice(int gpuDevice) { this.gpuDevice = gpuDevice; }

    /**
     * Start training with the given config and source data path.
     */
    public CompletableFuture<ProcessOrchestrator.ProcessResult> startTraining(
            Path sourcePath, Path outputPath, TrainingConfig config,
            Consumer<String> logHandler) {

        List<String> args = buildTrainingArgs(sourcePath, outputPath, config);
        List<String> command;

        if (condaPath != null && condaEnvName != null && !condaEnvName.isBlank()) {
            command = ProcessOrchestrator.buildCondaCommand(condaPath, condaEnvName, args);
        } else {
            command = args;
        }

        Map<String, String> env = new HashMap<>();
        env.put("CUDA_VISIBLE_DEVICES", String.valueOf(gpuDevice));

        currentIteration.set(0);
        loss.set(0);
        psnr.set(0);
        progress.set(0);
        statusMessage.set("Training started...");

        int totalIterations = config.getIterations();

        Consumer<String> outputParser = line -> {
            if (logHandler != null) logHandler.accept(line);
            parseTrainingOutput(line, totalIterations);
        };

        return orchestrator.run(command, Path.of(gsRepoPath), env, outputParser, outputParser)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        progress.set(1.0);
                        statusMessage.set("Training complete.");
                    } else {
                        statusMessage.set("Training failed (exit code " + result.getExitCode() + ")");
                    }
                    return result;
                });
    }

    /**
     * Resume training from a checkpoint.
     */
    public CompletableFuture<ProcessOrchestrator.ProcessResult> resumeTraining(
            Path sourcePath, Path outputPath, TrainingConfig config,
            String checkpointPath, Consumer<String> logHandler) {

        config.setCheckpointPath(checkpointPath);
        return startTraining(sourcePath, outputPath, config, logHandler);
    }

    private List<String> buildTrainingArgs(Path sourcePath, Path outputPath, TrainingConfig config) {
        List<String> args = new ArrayList<>();
        args.add(pythonPath);
        args.add("train.py");
        args.add("-s");
        args.add(sourcePath.toString());
        args.add("-m");
        args.add(outputPath.toString());
        args.add("--iterations");
        args.add(String.valueOf(config.getIterations()));
        args.add("--position_lr_init");
        args.add(String.valueOf(config.getPositionLrInit()));
        args.add("--position_lr_final");
        args.add(String.valueOf(config.getPositionLrFinal()));
        args.add("--scaling_lr");
        args.add(String.valueOf(config.getScalingLr()));
        args.add("--rotation_lr");
        args.add(String.valueOf(config.getRotationLr()));
        args.add("--opacity_lr");
        args.add(String.valueOf(config.getOpacityLr()));
        args.add("--densify_grad_threshold");
        args.add(String.valueOf(config.getDensifyGradThreshold()));
        args.add("--densify_from_iter");
        args.add(String.valueOf(config.getDensifyFromIter()));
        args.add("--densify_until_iter");
        args.add(String.valueOf(config.getDensifyUntilIter()));
        args.add("--densification_interval");
        args.add(String.valueOf(config.getDensificationInterval()));

        if (config.isAntialiasing()) {
            args.add("--antialiasing");
        }
        if (config.isEval()) {
            args.add("--eval");
        }
        if (!"default".equals(config.getOptimizerType())) {
            args.add("--optimizer_type");
            args.add(config.getOptimizerType());
        }
        if (config.getCheckpointPath() != null && !config.getCheckpointPath().isBlank()) {
            args.add("--start_checkpoint");
            args.add(config.getCheckpointPath());
        }

        return args;
    }

    private void parseTrainingOutput(String line, int totalIterations) {
        LogParser.TrainingMetrics metrics = LogParser.parseTrainingLine(line);
        if (metrics != null) {
            javafx.application.Platform.runLater(() -> {
                if (metrics.iteration() > 0) {
                    currentIteration.set(metrics.iteration());
                    progress.set((double) metrics.iteration() / totalIterations);
                }
                if (metrics.loss() > 0) loss.set(metrics.loss());
                if (metrics.psnr() > 0) psnr.set(metrics.psnr());
            });
        }
    }

    public void cancel() { orchestrator.cancel(); }
    public boolean isRunning() { return orchestrator.isRunning(); }

    public IntegerProperty currentIterationProperty() { return currentIteration; }
    public DoubleProperty lossProperty() { return loss; }
    public DoubleProperty psnrProperty() { return psnr; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty statusMessageProperty() { return statusMessage; }
}
