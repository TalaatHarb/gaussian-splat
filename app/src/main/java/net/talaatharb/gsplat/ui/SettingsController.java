package net.talaatharb.gsplat.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.talaatharb.gsplat.model.AppSettings;
import net.talaatharb.gsplat.model.ReconstructionBackend;
import net.talaatharb.gsplat.service.PythonEnvService;

import java.io.File;
import java.io.IOException;

public class SettingsController {

    @FXML private ComboBox<ReconstructionBackend> reconstructionBackendCombo;
    @FXML private TextField ffmpegPathField;
    @FXML private TextField colmapPathField;
    @FXML private TextField vggtRepoPathField;
    @FXML private CheckBox vggtBundleAdjustmentCheck;
    @FXML private TextField pythonPathField;
    @FXML private TextField gsRepoPathField;
    @FXML private TextField condaPathField;
    @FXML private TextField condaEnvField;
    @FXML private Spinner<Integer> gpuSpinner;
    @FXML private Label statusLabel;

    private final PythonEnvService envService = new PythonEnvService();

    @FXML
    public void initialize() {
        gpuSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 7, 0));
        reconstructionBackendCombo.setItems(FXCollections.observableArrayList(ReconstructionBackend.values()));
        reconstructionBackendCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateReconstructionFieldState(newVal));

        AppSettings settings = envService.loadSettings();
        applyToFields(settings);
    }

    @FXML
    private void onAutoDetect() {
        AppSettings detected = envService.autoDetect();
        applyToFields(detected);
        statusLabel.setText("Auto-detection complete.");
    }

    private void applyToFields(AppSettings settings) {
        reconstructionBackendCombo.setValue(settings.getReconstructionBackend());
        if (settings.getFfmpegPath() != null) ffmpegPathField.setText(settings.getFfmpegPath());
        if (settings.getColmapPath() != null) colmapPathField.setText(settings.getColmapPath());
        if (settings.getVggtRepoPath() != null) vggtRepoPathField.setText(settings.getVggtRepoPath());
        vggtBundleAdjustmentCheck.setSelected(settings.isVggtUseBundleAdjustment());
        if (settings.getPythonPath() != null) pythonPathField.setText(settings.getPythonPath());
        if (settings.getGaussianSplattingRepoPath() != null) gsRepoPathField.setText(settings.getGaussianSplattingRepoPath());
        if (settings.getCondaPath() != null) condaPathField.setText(settings.getCondaPath());
        if (settings.getCondaEnvName() != null) condaEnvField.setText(settings.getCondaEnvName());
        gpuSpinner.getValueFactory().setValue(settings.getGpuDevice());
        updateReconstructionFieldState(settings.getReconstructionBackend());
    }

    public void saveSettings() {
        AppSettings settings = new AppSettings();
        settings.setReconstructionBackend(reconstructionBackendCombo.getValue());
        settings.setFfmpegPath(ffmpegPathField.getText());
        settings.setColmapPath(colmapPathField.getText());
        settings.setVggtRepoPath(vggtRepoPathField.getText());
        settings.setVggtUseBundleAdjustment(vggtBundleAdjustmentCheck.isSelected());
        settings.setPythonPath(pythonPathField.getText());
        settings.setGaussianSplattingRepoPath(gsRepoPathField.getText());
        settings.setCondaPath(condaPathField.getText());
        settings.setCondaEnvName(condaEnvField.getText());
        settings.setGpuDevice(gpuSpinner.getValue());

        try {
            envService.saveSettings(settings);
            statusLabel.setText("Settings saved.");
        } catch (IOException e) {
            statusLabel.setText("Error saving settings: " + e.getMessage());
        }
    }

    @FXML private void onBrowseFFmpeg() { browseExecutable(ffmpegPathField, "Select FFmpeg"); }
    @FXML private void onBrowseColmap() { browseExecutable(colmapPathField, "Select COLMAP"); }
    @FXML private void onBrowsePython() { browseExecutable(pythonPathField, "Select Python"); }
    @FXML private void onBrowseConda() { browseExecutable(condaPathField, "Select Conda"); }
    @FXML private void onBrowseVggtRepo() { browseDirectory(vggtRepoPathField, "Select VGGT Repository"); }
    @FXML private void onBrowseGsRepo() { browseDirectory(gsRepoPathField, "Select Gaussian Splatting Repository"); }

    @FXML
    private void browseDirectory(TextField field, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File dir = chooser.showDialog(field.getScene().getWindow());
        if (dir != null) {
            field.setText(dir.getAbsolutePath());
        }
    }

    private void updateReconstructionFieldState(ReconstructionBackend backend) {
        ReconstructionBackend selected = backend == null ? ReconstructionBackend.COLMAP : backend;
        boolean useVggt = selected == ReconstructionBackend.VGGT;
        colmapPathField.setDisable(useVggt);
        vggtRepoPathField.setDisable(!useVggt);
        vggtBundleAdjustmentCheck.setDisable(!useVggt);
    }

    private void browseExecutable(TextField field, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        File file = chooser.showOpenDialog(field.getScene().getWindow());
        if (file != null) {
            field.setText(file.getAbsolutePath());
        }
    }
}
