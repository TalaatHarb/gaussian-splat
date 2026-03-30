package net.talaatharb.gsplat.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for reading and merging PLY (Polygon File Format) files containing Gaussian splat data.
 */
public class PlyMerger {

    public record PlyHeader(
        int vertexCount,
        List<String> properties,
        int headerEndOffset,
        boolean isBinary
    ) {}

    /**
     * Parse the header of a PLY file.
     */
    public static PlyHeader parseHeader(Path plyFile) throws IOException {
        List<String> properties = new ArrayList<>();
        int vertexCount = 0;
        boolean isBinary = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(plyFile.toFile()))) {
            String line;
            int offset = 0;
            while ((line = reader.readLine()) != null) {
                offset += line.length() + 1; // +1 for newline
                line = line.trim();

                if (line.equals("end_header")) {
                    return new PlyHeader(vertexCount, properties, offset, isBinary);
                }
                if (line.startsWith("format binary")) {
                    isBinary = true;
                }
                if (line.startsWith("element vertex")) {
                    vertexCount = Integer.parseInt(line.split("\\s+")[2]);
                }
                if (line.startsWith("property")) {
                    properties.add(line);
                }
            }
        }
        throw new IOException("Invalid PLY file: no end_header found");
    }

    /**
     * Merge two PLY files by concatenating their vertex data.
     * Both files must have the same property layout.
     */
    public static void merge(Path ply1, Path ply2, Path output) throws IOException {
        PlyHeader header1 = parseHeader(ply1);
        PlyHeader header2 = parseHeader(ply2);

        // Verify compatible formats
        if (!header1.properties().equals(header2.properties())) {
            throw new IOException("PLY files have incompatible property layouts");
        }
        if (header1.isBinary() != header2.isBinary()) {
            throw new IOException("Cannot merge binary and ASCII PLY files");
        }

        int totalVertices = header1.vertexCount() + header2.vertexCount();

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(output))) {
            // Write new header
            writeHeader(out, totalVertices, header1.properties(), header1.isBinary());

            // Write vertex data from file 1
            byte[] data1 = Files.readAllBytes(ply1);
            out.write(data1, header1.headerEndOffset(), data1.length - header1.headerEndOffset());

            // Write vertex data from file 2
            byte[] data2 = Files.readAllBytes(ply2);
            out.write(data2, header2.headerEndOffset(), data2.length - header2.headerEndOffset());
        }
    }

    private static void writeHeader(OutputStream out, int vertexCount,
                                     List<String> properties, boolean binary) throws IOException {
        PrintWriter writer = new PrintWriter(out, false);
        writer.println("ply");
        writer.println(binary ? "format binary_little_endian 1.0" : "format ascii 1.0");
        writer.println("element vertex " + vertexCount);
        for (String prop : properties) {
            writer.println(prop);
        }
        writer.println("end_header");
        writer.flush();
    }
}
