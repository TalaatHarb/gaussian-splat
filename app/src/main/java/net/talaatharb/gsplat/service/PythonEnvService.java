package net.talaatharb.gsplat.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.talaatharb.gsplat.model.AppSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PythonEnvService {

    private static final Path SETTINGS_DIR = Path.of(System.getProperty("user.home"), ".gsplat");
    private static final Path SETTINGS_FILE = SETTINGS_DIR.resolve("settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public AppSettings loadSettings() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                String json = Files.readString(SETTINGS_FILE);
                return GSON.fromJson(json, AppSettings.class);
            }
        } catch (IOException e) {
            // Fall through to defaults
        }
        return new AppSettings();
    }

    public void saveSettings(AppSettings settings) throws IOException {
        Files.createDirectories(SETTINGS_DIR);
        Files.writeString(SETTINGS_FILE, GSON.toJson(settings));
    }

    /**
     * Try to find an executable on the system PATH.
     */
    public String findOnPath(String executableName) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        String[] suffixes = isWindows ? new String[]{".exe", ".cmd", ".bat", ""} : new String[]{""};
        for (String dir : pathEnv.split(isWindows ? ";" : ":")) {
            for (String suffix : suffixes) {
                Path candidate = Path.of(dir, executableName + suffix);
                if (Files.isExecutable(candidate)) {
                    return candidate.toString();
                }
            }
        }
        return null;
    }

    /**
     * Detect conda environments by running `conda info --envs`.
     */
    public List<String> detectCondaEnvironments() {
        List<String> envs = new ArrayList<>();
        String condaPath = findOnPath("conda");
        if (condaPath == null) return envs;

        try {
            ProcessBuilder pb = new ProcessBuilder(condaPath, "info", "--envs");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            proc.waitFor();

            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                // Format: "envname    /path/to/env" or "* /path/to/base"
                String[] parts = line.split("\\s+");
                if (parts.length >= 1 && !parts[0].equals("*")) {
                    envs.add(parts[0]);
                }
            }
        } catch (IOException | InterruptedException e) {
            // Conda not available or failed
        }
        return envs;
    }

    /**
     * Auto-detect tool paths and return a populated settings object.
     */
    public AppSettings autoDetect() {
        AppSettings settings = loadSettings();

        if (settings.getFfmpegPath() == null || settings.getFfmpegPath().isBlank()) {
            String ffmpeg = findOnPath("ffmpeg");
            if (ffmpeg != null) settings.setFfmpegPath(ffmpeg);
        }

        if (settings.getColmapPath() == null || settings.getColmapPath().isBlank()) {
            String colmap = findOnPath("colmap");
            if (colmap != null) settings.setColmapPath(colmap);
        }

        if (settings.getPythonPath() == null || settings.getPythonPath().isBlank()) {
            String python = findOnPath("python");
            if (python == null) python = findOnPath("python3");
            if (python != null) settings.setPythonPath(python);
        }

        if (settings.getCondaPath() == null || settings.getCondaPath().isBlank()) {
            String conda = findOnPath("conda");
            if (conda != null) settings.setCondaPath(conda);
        }

        return settings;
    }
}
