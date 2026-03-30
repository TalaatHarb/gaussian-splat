package net.talaatharb.gsplat.model;

import java.time.LocalDateTime;

public class SplatFile {

    public enum Format {
        PLY, KSPLAT
    }

    private String path;
    private Format format;
    private long sizeBytes;
    private LocalDateTime createdAt;
    private String sourceProject;

    public SplatFile() {}

    public SplatFile(String path, Format format) {
        this.path = path;
        this.format = format;
        this.createdAt = LocalDateTime.now();
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Format getFormat() { return format; }
    public void setFormat(Format format) { this.format = format; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSourceProject() { return sourceProject; }
    public void setSourceProject(String sourceProject) { this.sourceProject = sourceProject; }
}
