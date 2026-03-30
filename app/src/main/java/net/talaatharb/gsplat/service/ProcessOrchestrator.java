package net.talaatharb.gsplat.service;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ProcessOrchestrator {

    public static class ProcessResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public boolean isSuccess() { return exitCode == 0; }
    }

    private Process currentProcess;
    private volatile boolean cancelled;

    public CompletableFuture<ProcessResult> run(List<String> command, Path workDir,
                                                 Map<String, String> envVars,
                                                 Consumer<String> stdoutHandler,
                                                 Consumer<String> stderrHandler) {
        return CompletableFuture.supplyAsync(() -> {
            cancelled = false;
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();

            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                if (workDir != null) {
                    pb.directory(workDir.toFile());
                }
                if (envVars != null) {
                    pb.environment().putAll(envVars);
                }

                currentProcess = pb.start();

                // Read stdout in a thread
                Thread stdoutThread = Thread.ofVirtual().start(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(currentProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stdoutBuf.append(line).append("\n");
                            if (stdoutHandler != null) {
                                String finalLine = line;
                                Platform.runLater(() -> stdoutHandler.accept(finalLine));
                            }
                        }
                    } catch (IOException e) {
                        // Stream closed
                    }
                });

                // Read stderr in a thread
                Thread stderrThread = Thread.ofVirtual().start(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(currentProcess.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stderrBuf.append(line).append("\n");
                            if (stderrHandler != null) {
                                String finalLine = line;
                                Platform.runLater(() -> stderrHandler.accept(finalLine));
                            }
                        }
                    } catch (IOException e) {
                        // Stream closed
                    }
                });

                int exitCode = currentProcess.waitFor();
                stdoutThread.join();
                stderrThread.join();

                return new ProcessResult(exitCode, stdoutBuf.toString(), stderrBuf.toString());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ProcessResult(-1, stdoutBuf.toString(), "Process interrupted");
            } catch (IOException e) {
                return new ProcessResult(-1, "", "Failed to start process: " + e.getMessage());
            } finally {
                currentProcess = null;
            }
        });
    }

    public void cancel() {
        cancelled = true;
        Process proc = currentProcess;
        if (proc != null && proc.isAlive()) {
            proc.destroyForcibly();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isRunning() {
        Process proc = currentProcess;
        return proc != null && proc.isAlive();
    }

    /**
     * Build a platform-appropriate command to run a program with an activated conda environment.
     */
    public static List<String> buildCondaCommand(String condaPath, String envName, List<String> command) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            // cmd /c "conda activate envName && python ..."
            String joined = String.join(" ", command);
            return List.of("cmd", "/c", condaPath + " activate " + envName + " && " + joined);
        } else {
            String joined = String.join(" ", command);
            return List.of("bash", "-c", "source " + condaPath + " activate " + envName + " && " + joined);
        }
    }

    /**
     * Build a command that uses a specific Python interpreter directly.
     */
    public static List<String> buildPythonCommand(String pythonPath, List<String> args) {
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add(pythonPath);
        cmd.addAll(args);
        return cmd;
    }
}
