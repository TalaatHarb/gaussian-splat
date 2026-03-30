package net.talaatharb.gsplat.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import net.talaatharb.gsplat.model.AppSettings;
import net.talaatharb.gsplat.model.TrainingConfig;
import net.talaatharb.gsplat.service.PythonEnvService;
import net.talaatharb.gsplat.service.TrainingService;

import java.io.File;
import java.nio.file.Path;

public class TrainingPanelController {

    @FXML private TextField sourcePathField;
    @FXML private TextField outputPathField;
    @FXML private Spinner<Integer> iterationsSpinner;
    @FXML private TextField posLrInitField;
    @FXML private TextField scalingLrField;
    @FXML private ComboBox<String> optimizerCombo;
    @FXML private CheckBox antialiasingCheck;
    @FXML private CheckBox evalCheck;

    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label iterLabel;

    @FXML private LineChart<Number, Number> lossChart;
    @FXML private LineChart<Number, Number> psnrChart;
    @FXML private TextArea trainingLog;

    private XYChart.Series<Number, Number> lossSeries;
    private XYChart.Series<Number, Number> psnrSeries;
    private TrainingService trainingService;

    @FXML
    public void initialize() {
        iterationsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 100000, 30000, 1000));
        optimizerCombo.setItems(FXCollections.observableArrayList("default", "sparse_adam"));
        optimizerCombo.setValue("default");

        lossSeries = new XYChart.Series<>();
        lossSeries.setName("Loss");
        lossChart.getData().add(lossSeries);

        psnrSeries = new XYChart.Series<>();
        psnrSeries.setName("PSNR");
        psnrChart.getData().add(psnrSeries);
    }

    @FXML
    private void onBrowseSource() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Source (COLMAP output)");
        File dir = chooser.showDialog(sourcePathField.getScene().getWindow());
        if (dir != null) sourcePathField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");
        File dir = chooser.showDialog(outputPathField.getScene().getWindow());
        if (dir != null) outputPathField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onStartTraining() {
        String source = sourcePathField.getText();
        String output = outputPathField.getText();
        if (source == null || source.isBlank() || output == null || output.isBlank()) {
            statusLabel.setText("Please set source and output paths.");
            return;
        }

        TrainingConfig config = buildConfig();
        TrainingService service = getTrainingService();
        if (service == null) {
            statusLabel.setText("Python or GS repo path not configured. Check Settings.");
            return;
        }

        // Reset charts
        lossSeries.getData().clear();
        psnrSeries.getData().clear();
        trainingLog.clear();

        startBtn.setDisable(true);
        pauseBtn.setDisable(false);
        cancelBtn.setDisable(false);

        // Bind progress
        progressBar.progressProperty().bind(service.progressProperty());
        statusLabel.textProperty().bind(service.statusMessageProperty());

        service.currentIterationProperty().addListener((obs, oldVal, newVal) -> {
            iterLabel.setText(newVal.intValue() + " / " + config.getIterations());
        });

        service.lossProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                lossSeries.getData().add(new XYChart.Data<>(
                        service.currentIterationProperty().get(), newVal.doubleValue()));
            }
        });

        service.psnrProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                psnrSeries.getData().add(new XYChart.Data<>(
                        service.currentIterationProperty().get(), newVal.doubleValue()));
            }
        });

        service.startTraining(Path.of(source), Path.of(output), config, line -> {
            javafx.application.Platform.runLater(() -> {
                trainingLog.appendText(line + "\n");
            });
        }).thenRun(() -> javafx.application.Platform.runLater(() -> {
            startBtn.setDisable(false);
            pauseBtn.setDisable(true);
            cancelBtn.setDisable(true);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
        }));
    }

    @FXML
    private void onPauseTraining() {
        // Pause = cancel with preserved checkpoint
        if (trainingService != null) {
            trainingService.cancel();
            statusLabel.textProperty().unbind();
            statusLabel.setText("Training paused. Resume from last checkpoint.");
        }
    }

    @FXML
    private void onCancelTraining() {
        if (trainingService != null) {
            trainingService.cancel();
            statusLabel.textProperty().unbind();
            statusLabel.setText("Training cancelled.");
            startBtn.setDisable(false);
            pauseBtn.setDisable(true);
            cancelBtn.setDisable(true);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        }
    }

    private TrainingConfig buildConfig() {
        TrainingConfig config = new TrainingConfig();
        config.setIterations(iterationsSpinner.getValue());
        try { config.setPositionLrInit(Double.parseDouble(posLrInitField.getText())); } catch (NumberFormatException ignored) {}
        try { config.setScalingLr(Double.parseDouble(scalingLrField.getText())); } catch (NumberFormatException ignored) {}
        config.setOptimizerType(optimizerCombo.getValue());
        config.setAntialiasing(antialiasingCheck.isSelected());
        config.setEval(evalCheck.isSelected());
        return config;
    }

    private TrainingService getTrainingService() {
        if (trainingService != null) return trainingService;

        PythonEnvService envService = new PythonEnvService();
        AppSettings settings = envService.loadSettings();

        if (settings.getPythonPath() == null || settings.getGaussianSplattingRepoPath() == null) {
            return null;
        }

        trainingService = new TrainingService(settings.getPythonPath(), settings.getGaussianSplattingRepoPath());
        if (settings.getCondaPath() != null) trainingService.setCondaPath(settings.getCondaPath());
        if (settings.getCondaEnvName() != null) trainingService.setCondaEnvName(settings.getCondaEnvName());
        trainingService.setGpuDevice(settings.getGpuDevice());
        return trainingService;
    }
}
