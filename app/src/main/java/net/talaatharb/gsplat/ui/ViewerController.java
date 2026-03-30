package net.talaatharb.gsplat.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import net.talaatharb.gsplat.service.LocalHttpServer;

import java.io.File;
import java.io.IOException;

public class ViewerController {

    @FXML private WebView webView;
    @FXML private Slider transparencySlider;
    @FXML private Slider pointScaleSlider;
    @FXML private Slider cameraSpeedSlider;
    @FXML private Label fileInfoLabel;

    private WebEngine webEngine;
    private LocalHttpServer httpServer;
    private String loadedFilePath;

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();

        // Start local HTTP server and load viewer
        try {
            httpServer = new LocalHttpServer();
            httpServer.start();
            String viewerUrl = httpServer.getBaseUrl() + "/web/viewer.html";
            webEngine.load(viewerUrl);
        } catch (IOException e) {
            fileInfoLabel.setText("Failed to start viewer server: " + e.getMessage());
        }

        // Bind slider events
        transparencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            executeViewerScript("window.viewerBridge.setTransparency(" + newVal.doubleValue() + ")");
        });

        pointScaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            executeViewerScript("window.viewerBridge.setPointScale(" + newVal.doubleValue() + ")");
        });
    }

    @FXML
    private void onOpenSplat() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Splat File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gaussian Splat Files", "*.ply", "*.ksplat", "*.splat"),
                new FileChooser.ExtensionFilter("PLY Files", "*.ply"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(webView.getScene().getWindow());
        if (file != null) {
            loadSplatFile(file.getAbsolutePath());
        }
    }

    /**
     * Load a splat file into the viewer. Can be called from other controllers.
     */
    public void loadSplatFile(String absolutePath) {
        if (httpServer == null || !httpServer.isRunning()) {
            fileInfoLabel.setText("Server not running");
            return;
        }
        loadedFilePath = absolutePath;

        // URL-encode the file path for the local server
        String encodedPath = absolutePath.replace("\\", "/");
        String fileUrl = httpServer.getBaseUrl() + "/files/" + encodedPath;

        executeViewerScript("window.viewerBridge.loadSplat('" + escapeJs(fileUrl) + "')");
        fileInfoLabel.setText(new File(absolutePath).getName());
    }

    @FXML
    private void onResetCamera() {
        executeViewerScript("window.viewerBridge.resetCamera()");
    }

    @FXML
    private void onScreenshot() {
        // TODO: Implement WebView snapshot
        fileInfoLabel.setText("Screenshot not yet implemented");
    }

    private void executeViewerScript(String script) {
        if (webEngine != null) {
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                // WebView may not be ready yet
            }
        }
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }
}
