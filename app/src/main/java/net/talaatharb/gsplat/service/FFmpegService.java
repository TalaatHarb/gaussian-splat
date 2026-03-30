package net.talaatharb.gsplat.service;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegService {

    private static final Pattern FRAME_PATTERN = Pattern.compile("frame=\\s*(\\d+)");
    private static final Pattern DURATION_PATTERN = Pattern.compile("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");

    private final ProcessOrchestrator orchestrator = new ProcessOrchestrator();
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("");

    private String ffmpegPath;
    private double totalDurationSeconds;

    public FFmpegService(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    /**
     * Extract frames from a video file at the specified FPS.
     */
    public CompletableFuture<ProcessOrchestrator.ProcessResult> extractFrames(
            String videoPath, Path outputDir, double fps,
            Consumer<String> logHandler) {

        List<String> command = List.of(
                ffmpegPath,
                "-i", videoPath,
                "-vf", "fps=" + fps,
                "-q:v", "2",
                outputDir.resolve("frame_%04d.png").toString()
        );

        progress.set(0);
        statusMessage.set("Extracting frames...");
        totalDurationSeconds = 0;

        Consumer<String> stderrParser = line -> {
            if (logHandler != null) logHandler.accept(line);
            parseFfmpegProgress(line);
        };

        return orchestrator.run(command, null, Map.of(), logHandler, stderrParser)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        progress.set(1.0);
                        statusMessage.set("Frame extraction complete.");
                    } else {
                        statusMessage.set("Frame extraction failed (exit code " + result.getExitCode() + ")");
                    }
                    return result;
                });
    }

    /**
     * Probe a video file for metadata using ffprobe.
     */
    public CompletableFuture<ProcessOrchestrator.ProcessResult> probeVideo(String videoPath, Consumer<String> logHandler) {
        // Use ffprobe if available, otherwise ffmpeg -i will output info to stderr
        String probePath = ffmpegPath.replace("ffmpeg", "ffprobe");
        List<String> command = List.of(
                probePath,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format", "-show_streams",
                videoPath
        );

        return orchestrator.run(command, null, Map.of(), logHandler, logHandler);
    }

    private void parseFfmpegProgress(String line) {
        // Extract total duration on first encounter
        if (totalDurationSeconds == 0) {
            Matcher durMatcher = DURATION_PATTERN.matcher(line);
            if (durMatcher.find()) {
                int hours = Integer.parseInt(durMatcher.group(1));
                int minutes = Integer.parseInt(durMatcher.group(2));
                int seconds = Integer.parseInt(durMatcher.group(3));
                totalDurationSeconds = hours * 3600.0 + minutes * 60.0 + seconds;
            }
        }

        // Parse frame count for progress estimation
        Matcher frameMatcher = FRAME_PATTERN.matcher(line);
        if (frameMatcher.find() && totalDurationSeconds > 0) {
            int frame = Integer.parseInt(frameMatcher.group(1));
            // Rough estimate: assume ~30fps source
            double estimatedProgress = (frame / 30.0) / totalDurationSeconds;
            javafx.application.Platform.runLater(() ->
                    progress.set(Math.min(estimatedProgress, 0.99)));
        }
    }

    public void cancel() { orchestrator.cancel(); }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty statusMessageProperty() { return statusMessage; }
}
