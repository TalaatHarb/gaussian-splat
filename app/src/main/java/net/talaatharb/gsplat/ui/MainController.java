package net.talaatharb.gsplat.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import net.talaatharb.gsplat.model.Project;

import java.io.IOException;
import java.util.Optional;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private TreeView<String> projectTree;
    @FXML private TabPane centerTabs;
    @FXML private TextArea logConsole;
    @FXML private MenuBar menuBar;
    @FXML private ToolBar toolBar;

    private Tab viewerTab;
    private Tab trainingTab;

    @FXML
    public void initialize() {
        setupProjectTree();
        setupTabs();
        appendLog("Gaussian Splat Studio initialized.");
    }

    private void setupProjectTree() {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);
        projectTree.setRoot(root);
        projectTree.setShowRoot(true);
    }

    private void setupTabs() {
        // Viewer tab (loaded via FXML)
        try {
            viewerTab = new Tab("Viewer");
            viewerTab.setClosable(false);
            FXMLLoader viewerLoader = new FXMLLoader(getClass().getResource("/fxml/viewer.fxml"));
            viewerTab.setContent(viewerLoader.load());
            centerTabs.getTabs().add(viewerTab);
        } catch (IOException e) {
            appendLog("ERROR: Failed to load viewer: " + e.getMessage());
        }

        // Training tab
        try {
            trainingTab = new Tab("Training");
            trainingTab.setClosable(false);
            FXMLLoader trainingLoader = new FXMLLoader(getClass().getResource("/fxml/training-panel.fxml"));
            trainingTab.setContent(trainingLoader.load());
            centerTabs.getTabs().add(trainingTab);
        } catch (IOException e) {
            appendLog("ERROR: Failed to load training panel: " + e.getMessage());
        }
    }

    @FXML
    private void onNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project-wizard.fxml"));
            DialogPane dialogPane = loader.load();
            ProjectWizardController wizardController = loader.getController();

            Dialog<Project> dialog = new Dialog<>();
            dialog.setTitle("New Project");
            dialog.setDialogPane(dialogPane);

            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.FINISH) {
                    return wizardController.createProject();
                }
                return null;
            });

            Optional<Project> result = dialog.showAndWait();
            result.ifPresent(project -> {
                TreeItem<String> item = new TreeItem<>(project.getName());
                projectTree.getRoot().getChildren().add(item);
                appendLog("Created project: " + project.getName());
            });
        } catch (IOException e) {
            appendLog("ERROR: Failed to open project wizard: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            DialogPane dialogPane = loader.load();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Settings");
            dialog.setDialogPane(dialogPane);
            dialog.showAndWait();
        } catch (IOException e) {
            appendLog("ERROR: Failed to open settings: " + e.getMessage());
        }
    }

    @FXML
    private void onExit() {
        System.exit(0);
    }

    public void appendLog(String message) {
        javafx.application.Platform.runLater(() -> {
            logConsole.appendText(message + "\n");
            logConsole.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void showViewerTab() {
        centerTabs.getSelectionModel().select(viewerTab);
    }

    public void showTrainingTab() {
        centerTabs.getSelectionModel().select(trainingTab);
    }
}
