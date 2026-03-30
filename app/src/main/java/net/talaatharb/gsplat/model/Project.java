package net.talaatharb.gsplat.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Project {

    public enum Status {
        NEW, FRAMES_EXTRACTED, COLMAP_COMPLETE, TRAINING, TRAINED, ERROR
    }

    private String name;
    private String basePath;
    private Status status;
    private TrainingConfig trainingConfig;
    private List<String> inputImages;
    private String inputVideo;
    private double videoFps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Project() {
        this.status = Status.NEW;
        this.trainingConfig = new TrainingConfig();
        this.inputImages = new ArrayList<>();
        this.videoFps = 2.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Project(String name, String basePath) {
        this();
        this.name = name;
        this.basePath = basePath;
    }

    // Derived paths
    public Path getInputDir() { return Path.of(basePath, "input"); }
    public Path getColmapDir() { return Path.of(basePath, "colmap"); }
    public Path getOutputDir() { return Path.of(basePath, "output"); }
    public Path getSplatsDir() { return Path.of(basePath, "splats"); }
    public Path getProjectFile() { return Path.of(basePath, "project.json"); }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public TrainingConfig getTrainingConfig() { return trainingConfig; }
    public void setTrainingConfig(TrainingConfig trainingConfig) { this.trainingConfig = trainingConfig; }

    public List<String> getInputImages() { return inputImages; }
    public void setInputImages(List<String> inputImages) { this.inputImages = inputImages; }

    public String getInputVideo() { return inputVideo; }
    public void setInputVideo(String inputVideo) { this.inputVideo = inputVideo; }

    public double getVideoFps() { return videoFps; }
    public void setVideoFps(double videoFps) { this.videoFps = videoFps; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
