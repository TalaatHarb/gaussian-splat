package net.talaatharb.gsplat.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.talaatharb.gsplat.model.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectWizardController {

    @FXML private TextField projectNameField;
    @FXML private TextField projectLocationField;
    @FXML private RadioButton imagesRadio;
    @FXML private RadioButton videoRadio;
    @FXML private ToggleGroup mediaToggle;
    @FXML private VBox imagesPane;
    @FXML private VBox videoPane;
    @FXML private Label imageCountLabel;
    @FXML private TextField videoPathField;
    @FXML private Spinner<Double> fpsSpinner;
    @FXML private Label summaryLabel;

    private final List<File> selectedImages = new ArrayList<>();

    @FXML
    public void initialize() {
        fpsSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 60.0, 2.0, 0.5));

        mediaToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isImages = newVal == imagesRadio;
            imagesPane.setVisible(isImages);
            imagesPane.setManaged(isImages);
            videoPane.setVisible(!isImages);
            videoPane.setManaged(!isImages);
            updateSummary();
        });

        projectNameField.textProperty().addListener((obs, o, n) -> updateSummary());
        projectLocationField.textProperty().addListener((obs, o, n) -> updateSummary());
    }

    @FXML
    private void onBrowseLocation() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Project Location");
        File dir = chooser.showDialog(projectNameField.getScene().getWindow());
        if (dir != null) {
            projectLocationField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onAddImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Images");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.tiff", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        List<File> files = chooser.showOpenMultipleDialog(projectNameField.getScene().getWindow());
        if (files != null) {
            selectedImages.addAll(files);
            imageCountLabel.setText(selectedImages.size() + " image(s) selected");
            updateSummary();
        }
    }

    @FXML
    private void onClearImages() {
        selectedImages.clear();
        imageCountLabel.setText("No images selected");
        updateSummary();
    }

    @FXML
    private void onBrowseVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Video File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.webm"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(projectNameField.getScene().getWindow());
        if (file != null) {
            videoPathField.setText(file.getAbsolutePath());
            updateSummary();
        }
    }

    private void updateSummary() {
        String name = projectNameField.getText();
        String location = projectLocationField.getText();
        if (name == null || name.isBlank() || location == null || location.isBlank()) {
            summaryLabel.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Project: ").append(name).append("\n");
        sb.append("Location: ").append(Path.of(location, name)).append("\n");
        if (imagesRadio.isSelected()) {
            sb.append("Input: ").append(selectedImages.size()).append(" image(s)");
        } else {
            sb.append("Input: Video → extract frames at ").append(fpsSpinner.getValue()).append(" FPS");
        }
        summaryLabel.setText(sb.toString());
    }

    public Project createProject() {
        String name = projectNameField.getText().trim();
        String location = projectLocationField.getText().trim();
        if (name.isEmpty() || location.isEmpty()) return null;

        Path basePath = Path.of(location, name);
        Project project = new Project(name, basePath.toString());

        try {
            Files.createDirectories(project.getInputDir());
            Files.createDirectories(project.getColmapDir());
            Files.createDirectories(project.getOutputDir());
            Files.createDirectories(project.getSplatsDir());
        } catch (IOException e) {
            return null;
        }

        if (imagesRadio.isSelected()) {
            List<String> paths = selectedImages.stream().map(File::getAbsolutePath).toList();
            project.setInputImages(paths);
        } else {
            project.setInputVideo(videoPathField.getText());
            project.setVideoFps(fpsSpinner.getValue());
        }

        return project;
    }
}
