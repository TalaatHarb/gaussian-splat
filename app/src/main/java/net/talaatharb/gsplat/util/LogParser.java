package net.talaatharb.gsplat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    // Typical 3DGS training output patterns:
    // "ITER 7000 | Loss: 0.0234 | PSNR: 25.43"
    // "[ITER 7000] loss = 0.0234 psnr = 25.43"
    // Various formats from different implementations
    private static final Pattern ITER_PATTERN = Pattern.compile(
            "(?:ITER|iter|Iteration|iteration)[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOSS_PATTERN = Pattern.compile(
            "(?:loss|Loss)[=:\\s]+([0-9.eE+-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PSNR_PATTERN = Pattern.compile(
            "(?:psnr|PSNR)[=:\\s]+([0-9.eE+-]+)", Pattern.CASE_INSENSITIVE);

    public record TrainingMetrics(int iteration, double loss, double psnr) {}

    public static TrainingMetrics parseTrainingLine(String line) {
        if (line == null || line.isBlank()) return null;

        int iteration = 0;
        double loss = 0;
        double psnr = 0;

        Matcher iterMatcher = ITER_PATTERN.matcher(line);
        if (iterMatcher.find()) {
            iteration = Integer.parseInt(iterMatcher.group(1));
        }

        Matcher lossMatcher = LOSS_PATTERN.matcher(line);
        if (lossMatcher.find()) {
            try {
                loss = Double.parseDouble(lossMatcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        Matcher psnrMatcher = PSNR_PATTERN.matcher(line);
        if (psnrMatcher.find()) {
            try {
                psnr = Double.parseDouble(psnrMatcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (iteration > 0 || loss > 0 || psnr > 0) {
            return new TrainingMetrics(iteration, loss, psnr);
        }
        return null;
    }
}
