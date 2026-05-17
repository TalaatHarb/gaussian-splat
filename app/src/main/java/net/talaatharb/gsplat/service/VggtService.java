package net.talaatharb.gsplat.service;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class VggtService {

    private final ProcessOrchestrator orchestrator = new ProcessOrchestrator();
    private final StringProperty statusMessage = new SimpleStringProperty("");

    private String pythonPath;
    private String vggtRepoPath;
    private String condaPath;
    private String condaEnvName;
    private int gpuDevice;

    public VggtService(String pythonPath, String vggtRepoPath) {
        this.pythonPath = pythonPath;
        this.vggtRepoPath = vggtRepoPath;
    }

    public void setPythonPath(String pythonPath) { this.pythonPath = pythonPath; }
    public void setVggtRepoPath(String vggtRepoPath) { this.vggtRepoPath = vggtRepoPath; }
    public void setCondaPath(String condaPath) { this.condaPath = condaPath; }
    public void setCondaEnvName(String condaEnvName) { this.condaEnvName = condaEnvName; }
    public void setGpuDevice(int gpuDevice) { this.gpuDevice = gpuDevice; }

    public CompletableFuture<ProcessOrchestrator.ProcessResult> runFullPipeline(
            Path sceneDir, boolean useBundleAdjustment, Consumer<String> logHandler) {

        Path repoDir = Path.of(vggtRepoPath == null ? "" : vggtRepoPath);
        Path demoScript = repoDir.resolve("demo_colmap.py");
        if (!Files.isRegularFile(demoScript)) {
            return CompletableFuture.completedFuture(new ProcessOrchestrator.ProcessResult(
                    -1, "", "VGGT demo_colmap.py not found at " + demoScript));
        }

        statusMessage.set("Running VGGT reconstruction...");

        List<String> args = new java.util.ArrayList<>();
        args.add(pythonPath);
        args.add("demo_colmap.py");
        args.add("--scene_dir");
        args.add(sceneDir.toString());
        if (useBundleAdjustment) {
            args.add("--use_ba");
        }

        List<String> command = args;
        if (condaPath != null && !condaPath.isBlank() && condaEnvName != null && !condaEnvName.isBlank()) {
            command = ProcessOrchestrator.buildCondaCommand(condaPath, condaEnvName, args);
        }

        Map<String, String> env = new HashMap<>();
        env.put("CUDA_VISIBLE_DEVICES", String.valueOf(gpuDevice));

        return orchestrator.run(command, repoDir, env, logHandler, logHandler)
                .thenApply(result -> {
                    statusMessage.set(result.isSuccess()
                            ? "VGGT reconstruction complete."
                            : "VGGT reconstruction failed.");
                    return result;
                });
    }

    public void cancel() { orchestrator.cancel(); }
    public StringProperty statusMessageProperty() { return statusMessage; }
}
