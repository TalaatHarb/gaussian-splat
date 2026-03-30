package net.talaatharb.gsplat.model;

public class AppSettings {

    private String ffmpegPath;
    private String colmapPath;
    private String pythonPath;
    private String gaussianSplattingRepoPath;
    private String condaPath;
    private String condaEnvName;
    private int gpuDevice = 0;

    public AppSettings() {}

    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }

    public String getColmapPath() { return colmapPath; }
    public void setColmapPath(String colmapPath) { this.colmapPath = colmapPath; }

    public String getPythonPath() { return pythonPath; }
    public void setPythonPath(String pythonPath) { this.pythonPath = pythonPath; }

    public String getGaussianSplattingRepoPath() { return gaussianSplattingRepoPath; }
    public void setGaussianSplattingRepoPath(String gaussianSplattingRepoPath) { this.gaussianSplattingRepoPath = gaussianSplattingRepoPath; }

    public String getCondaPath() { return condaPath; }
    public void setCondaPath(String condaPath) { this.condaPath = condaPath; }

    public String getCondaEnvName() { return condaEnvName; }
    public void setCondaEnvName(String condaEnvName) { this.condaEnvName = condaEnvName; }

    public int getGpuDevice() { return gpuDevice; }
    public void setGpuDevice(int gpuDevice) { this.gpuDevice = gpuDevice; }
}
