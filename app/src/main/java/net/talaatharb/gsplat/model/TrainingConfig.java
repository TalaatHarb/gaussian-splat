package net.talaatharb.gsplat.model;

public class TrainingConfig {

    private int iterations = 30000;
    private double positionLrInit = 0.00016;
    private double positionLrFinal = 0.0000016;
    private double scalingLr = 0.005;
    private double rotationLr = 0.001;
    private double opacityLr = 0.05;
    private double densifyGradThreshold = 0.0002;
    private int densifyFromIter = 500;
    private int densifyUntilIter = 15000;
    private int densificationInterval = 100;
    private boolean antialiasing = false;
    private boolean eval = false;
    private String optimizerType = "default";
    private String checkpointPath;

    public TrainingConfig() {}

    // Getters and setters
    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }

    public double getPositionLrInit() { return positionLrInit; }
    public void setPositionLrInit(double positionLrInit) { this.positionLrInit = positionLrInit; }

    public double getPositionLrFinal() { return positionLrFinal; }
    public void setPositionLrFinal(double positionLrFinal) { this.positionLrFinal = positionLrFinal; }

    public double getScalingLr() { return scalingLr; }
    public void setScalingLr(double scalingLr) { this.scalingLr = scalingLr; }

    public double getRotationLr() { return rotationLr; }
    public void setRotationLr(double rotationLr) { this.rotationLr = rotationLr; }

    public double getOpacityLr() { return opacityLr; }
    public void setOpacityLr(double opacityLr) { this.opacityLr = opacityLr; }

    public double getDensifyGradThreshold() { return densifyGradThreshold; }
    public void setDensifyGradThreshold(double densifyGradThreshold) { this.densifyGradThreshold = densifyGradThreshold; }

    public int getDensifyFromIter() { return densifyFromIter; }
    public void setDensifyFromIter(int densifyFromIter) { this.densifyFromIter = densifyFromIter; }

    public int getDensifyUntilIter() { return densifyUntilIter; }
    public void setDensifyUntilIter(int densifyUntilIter) { this.densifyUntilIter = densifyUntilIter; }

    public int getDensificationInterval() { return densificationInterval; }
    public void setDensificationInterval(int densificationInterval) { this.densificationInterval = densificationInterval; }

    public boolean isAntialiasing() { return antialiasing; }
    public void setAntialiasing(boolean antialiasing) { this.antialiasing = antialiasing; }

    public boolean isEval() { return eval; }
    public void setEval(boolean eval) { this.eval = eval; }

    public String getOptimizerType() { return optimizerType; }
    public void setOptimizerType(String optimizerType) { this.optimizerType = optimizerType; }

    public String getCheckpointPath() { return checkpointPath; }
    public void setCheckpointPath(String checkpointPath) { this.checkpointPath = checkpointPath; }
}
