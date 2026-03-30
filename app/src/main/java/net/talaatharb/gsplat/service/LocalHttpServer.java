package net.talaatharb.gsplat.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class LocalHttpServer {

    private static final Map<String, String> MIME_TYPES = Map.of(
            ".html", "text/html",
            ".js", "application/javascript",
            ".css", "text/css",
            ".json", "application/json",
            ".ply", "application/octet-stream",
            ".ksplat", "application/octet-stream",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".splat", "application/octet-stream"
    );

    private HttpServer server;
    private int port;

    /**
     * Start the server on a random available port.
     * Serves resources from classpath /web/ and files from the filesystem via /files/ prefix.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        // Serve embedded web resources from classpath
        server.createContext("/web/", new ClasspathHandler("/web/"));

        // Serve local files (splats, etc.)
        server.createContext("/files/", new FileHandler());

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() { return port; }
    public String getBaseUrl() { return "http://127.0.0.1:" + port; }
    public boolean isRunning() { return server != null; }

    /**
     * Serves files from the classpath (embedded web viewer assets).
     */
    private static class ClasspathHandler implements HttpHandler {
        private final String prefix;

        ClasspathHandler(String prefix) { this.prefix = prefix; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // Prevent directory traversal
            if (path.contains("..")) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                byte[] data = is.readAllBytes();
                addCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", getMimeType(path));
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            }
        }
    }

    /**
     * Serves files from the filesystem. The URL path after /files/ is treated as an absolute path.
     * Only serves files from directories that have been explicitly registered, or any .ply/.ksplat file.
     */
    private static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            // Strip the /files/ prefix
            String filePath = requestPath.substring("/files/".length());

            // Decode URL-encoded path
            filePath = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8);

            // Prevent directory traversal
            if (filePath.contains("..")) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            Path path = Path.of(filePath);
            if (!Files.exists(path) || Files.isDirectory(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] data = Files.readAllBytes(path);
            addCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", getMimeType(filePath));
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String getMimeType(String path) {
        String lower = path.toLowerCase();
        for (var entry : MIME_TYPES.entrySet()) {
            if (lower.endsWith(entry.getKey())) return entry.getValue();
        }
        return "application/octet-stream";
    }
}
