package net.talaatharb.gsplat.service;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ColmapService {

    public enum Stage {
        FEATURE_EXTRACTION, MATCHING, MAPPING, COMPLETE, ERROR
    }

    private final ProcessOrchestrator orchestrator = new ProcessOrchestrator();
    private final StringProperty currentStage = new SimpleStringProperty("Idle");
    private final StringProperty statusMessage = new SimpleStringProperty("");

    private String colmapPath;

    public ColmapService(String colmapPath) {
        this.colmapPath = colmapPath;
    }

    public void setColmapPath(String colmapPath) {
        this.colmapPath = colmapPath;
    }

    /**
     * Run the full COLMAP pipeline: feature extraction → matching → mapping.
     */
    public CompletableFuture<ProcessOrchestrator.ProcessResult> runFullPipeline(
            Path imagePath, Path workspacePath, Consumer<String> logHandler) {

        Path dbPath = workspacePath.resolve("database.db");
        Path sparsePath = workspacePath.resolve("sparse");

        return featureExtract(dbPath, imagePath, logHandler)
                .thenCompose(result -> {
                    if (!result.isSuccess()) return CompletableFuture.completedFuture(result);
                    return exhaustiveMatch(dbPath, logHandler);
                })
                .thenCompose(result -> {
                    if (!result.isSuccess()) return CompletableFuture.completedFuture(result);
                    return mapper(dbPath, imagePath, sparsePath, logHandler);
                })
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        currentStage.set(Stage.COMPLETE.name());
                        statusMessage.set("COLMAP pipeline complete.");
                    } else {
                        currentStage.set(Stage.ERROR.name());
                        statusMessage.set("COLMAP pipeline failed.");
                    }
                    return result;
                });
    }

    public CompletableFuture<ProcessOrchestrator.ProcessResult> featureExtract(
            Path dbPath, Path imagePath, Consumer<String> logHandler) {

        currentStage.set(Stage.FEATURE_EXTRACTION.name());
        statusMessage.set("Extracting features...");

        List<String> command = List.of(
                colmapPath, "feature_extractor",
                "--database_path", dbPath.toString(),
                "--image_path", imagePath.toString()
        );

        return orchestrator.run(command, null, Map.of(), logHandler, logHandler);
    }

    public CompletableFuture<ProcessOrchestrator.ProcessResult> exhaustiveMatch(
            Path dbPath, Consumer<String> logHandler) {

        currentStage.set(Stage.MATCHING.name());
        statusMessage.set("Matching features...");

        List<String> command = List.of(
                colmapPath, "exhaustive_matcher",
                "--database_path", dbPath.toString()
        );

        return orchestrator.run(command, null, Map.of(), logHandler, logHandler);
    }

    public CompletableFuture<ProcessOrchestrator.ProcessResult> mapper(
            Path dbPath, Path imagePath, Path outputPath, Consumer<String> logHandler) {

        currentStage.set(Stage.MAPPING.name());
        statusMessage.set("Running mapper...");

        List<String> command = List.of(
                colmapPath, "mapper",
                "--database_path", dbPath.toString(),
                "--image_path", imagePath.toString(),
                "--output_path", outputPath.toString()
        );

        return orchestrator.run(command, null, Map.of(), logHandler, logHandler);
    }

    public void cancel() { orchestrator.cancel(); }
    public StringProperty currentStageProperty() { return currentStage; }
    public StringProperty statusMessageProperty() { return statusMessage; }
}
