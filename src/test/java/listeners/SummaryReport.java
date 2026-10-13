package listeners;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects one row per executed test and writes a single self-contained HTML summary
 * (execution details, results table, failure/skip details with screenshots).
 */
public final class SummaryReport {

    public enum Status { PASS, FAIL, SKIP }

    public record Row(String id, String scenario, String method, Status status,
                      long durationMs, String message, String screenshot) {
    }

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm:ss a", Locale.ENGLISH);

    private final List<Row> rows = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger counter = new AtomicInteger();
    private final String idPrefix;

    public SummaryReport(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public void add(String scenario, String method, Status status, long durationMs,
                    String message, String screenshot) {
        String id = String.format("%s%03d", idPrefix, counter.incrementAndGet());
        rows.add(new Row(id, scenario, method, status, durationMs, message, screenshot));
    }

    public Path write(Path file, String suiteName, LocalDateTime start, LocalDateTime end,
                      String browser, String url, String detailedReportFileName) throws IOException {
        List<Row> snapshot;
        synchronized (rows) {
            snapshot = new ArrayList<>(rows);
        }
        long passed = snapshot.stream().filter(r -> r.status() == Status.PASS).count();
        long failed = snapshot.stream().filter(r -> r.status() == Status.FAIL).count();
        long skipped = snapshot.stream().filter(r -> r.status() == Status.SKIP).count();
        int total = snapshot.size();
        long passRate = total == 0 ? 0 : Math.round(passed * 100.0 / total);

        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Test Execution Summary</title>
                <style>
                  :root { --text:#1f1f1f; --muted:#6b6b6b; --line:#e8e8e8; --bg:#fafafa; --card:#fff;
                          --pass:#1a7f37; --pass-bg:#dcf5e3; --fail:#c62828; --fail-bg:#fde2e2;
                          --skip:#c2410c; --skip-bg:#feeadb; }
                  * { box-sizing:border-box; }
                  body { margin:0; background:var(--bg); color:var(--text);
                         font:15px/1.6 "Segoe UI", system-ui, -apple-system, sans-serif; }
                  main { max-width:960px; margin:32px auto; background:var(--card); padding:32px 40px;
                         border:1px solid var(--line); border-radius:8px; }
                  h1 { font-size:24px; margin:0 0 4px; } h2 { font-size:17px; margin:32px 0 12px; }
                  .sub { color:var(--muted); margin:0 0 24px; }
                  .tiles { display:grid; grid-template-columns:repeat(5,1fr); gap:12px; }
                  .tile { border:1px solid var(--line); border-radius:8px; padding:12px 16px; }
                  .tile b { display:block; font-size:24px; } .tile span { color:var(--muted); font-size:13px; }
                  .bar { display:flex; height:8px; border-radius:4px; overflow:hidden; margin-top:16px; background:var(--line); }
                  .bar i { display:block; }
                  .details p { margin:6px 0; }
                  hr { border:0; border-top:1px solid var(--line); margin:24px 0 0; }
                  table { width:100%; border-collapse:collapse; }
                  th { text-align:left; font-weight:600; padding:10px 8px; border-bottom:1px solid var(--line); }
                  td { padding:10px 8px; border-bottom:1px solid var(--line); vertical-align:top; }
                  td.num { font-variant-numeric:tabular-nums; white-space:nowrap; }
                  td small { color:var(--muted); font-family:Consolas, monospace; }
                  .badge { display:inline-block; font-size:12px; font-weight:700; padding:2px 10px; border-radius:999px; }
                  .PASS { color:var(--pass); background:var(--pass-bg); }
                  .FAIL { color:var(--fail); background:var(--fail-bg); }
                  .SKIP { color:var(--skip); background:var(--skip-bg); }
                  .fail-card { border:1px solid var(--line); border-radius:8px; padding:16px 20px; margin-bottom:16px; }
                  .fail-card h3 { margin:0 0 6px; font-size:16px; }
                  .reason.FAIL, .reason.SKIP { background:none; }
                  pre { white-space:pre-wrap; word-break:break-word; background:#f5f5f5; padding:10px 12px;
                        border-radius:6px; font-size:12.5px; margin:10px 0 0; }
                  .shot img { max-width:100%; border:1px solid var(--line); border-radius:6px; margin-top:10px; }
                  .empty { color:var(--muted); }
                  @media (max-width:700px) { main { padding:20px 16px; margin:0; border-radius:0; }
                    .tiles { grid-template-columns:repeat(2,1fr); } }
                </style></head><body><main>
                """);

        html.append("<h1>Test Execution Summary</h1>")
            .append("<p class=\"sub\">").append(esc(suiteName)).append("</p>");

        html.append("<div class=\"tiles\">")
            .append(tile(String.valueOf(total), "Total"))
            .append(tile(String.valueOf(passed), "Passed"))
            .append(tile(String.valueOf(failed), "Failed"))
            .append(tile(String.valueOf(skipped), "Skipped"))
            .append(tile(passRate + "%", "Pass rate"))
            .append("</div>");
        if (total > 0) {
            html.append("<div class=\"bar\">")
                .append(segment(passed, total, "var(--pass)"))
                .append(segment(failed, total, "var(--fail)"))
                .append(segment(skipped, total, "#f59e0b"))
                .append("</div>");
        }

        html.append("<h2>Execution details</h2><div class=\"details\">")
            .append("<p>Start: ").append(start.format(DISPLAY)).append("</p>")
            .append("<p>End: ").append(end.format(DISPLAY)).append("</p>")
            .append("<p>Duration: ").append(formatDuration(Duration.between(start, end).toMillis())).append("</p>")
            .append("<p>Application: ").append(esc(url)).append("</p>")
            .append("<p>Browser: ").append(esc(capitalize(browser)))
            .append(" &middot; Framework: Selenium + Java + TestNG</p>")
            .append("<p>Detailed report (steps, stack traces, screenshots): <a href=\"")
            .append(esc(detailedReportFileName)).append("\" target=\"_blank\">")
            .append(esc(detailedReportFileName)).append("</a></p>")
            .append("</div><hr>");

        html.append("<h2>Test case results</h2>");
        if (snapshot.isEmpty()) {
            html.append("<p class=\"empty\">No tests were executed.</p>");
        } else {
            html.append("<table><thead><tr><th>ID</th><th>Scenario</th><th>Status</th></tr></thead><tbody>");
            for (Row r : snapshot) {
                html.append("<tr><td class=\"num\">").append(r.id()).append("</td>")
                    .append("<td title=\"").append(esc(r.method())).append(" (")
                    .append(formatDuration(r.durationMs())).append(")\">")
                    .append(esc(r.scenario())).append("</td>")
                    .append("<td class=\"num\"><span class=\"badge ").append(r.status()).append("\">")
                    .append(statusLabel(r.status())).append("</span></td></tr>");
            }
            html.append("</tbody></table>");
        }

        List<Row> problems = snapshot.stream().filter(r -> r.status() != Status.PASS).toList();
        html.append("<h2>Failure details</h2>");
        if (problems.isEmpty()) {
            html.append("<p class=\"empty\">No failed or skipped tests.</p>");
        }
        for (Row r : problems) {
            String color = r.status() == Status.FAIL ? "var(--fail)" : "var(--skip)";
            html.append("<div class=\"fail-card\"><h3>").append(r.id()).append(" &mdash; ")
                .append(esc(r.scenario())).append("</h3>")
                .append("<p style=\"margin:0 0 6px;color:var(--muted);font-size:13px\">")
                .append(esc(r.method())).append(" &middot; ").append(formatDuration(r.durationMs())).append("</p>")
                .append("<div style=\"color:").append(color).append("\">")
                .append(r.status()).append(" &middot; ").append(esc(firstLine(r.message()))).append("</div>");
            if (r.message() != null && r.message().contains("\n")) {
                html.append("<pre>").append(esc(r.message())).append("</pre>");
            }
            if (r.screenshot() != null) {
                String src = esc(r.screenshot().replace('\\', '/'));
                html.append("<div class=\"shot\"><a href=\"").append(src).append("\" target=\"_blank\">")
                    .append("<img src=\"").append(src).append("\" alt=\"Screenshot for ")
                    .append(r.id()).append("\"></a></div>");
            }
            html.append("</div>");
        }

        html.append("</main></body></html>");

        Files.createDirectories(file.getParent());
        Files.writeString(file, html.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /** Plain-text version of the results table, for the console. */
    public String toConsoleTable() {
        List<Row> snapshot;
        synchronized (rows) {
            snapshot = new ArrayList<>(rows);
        }
        int width = Math.max("Scenario".length(),
                snapshot.stream().mapToInt(r -> r.scenario().length()).max().orElse(0));
        String format = "%-8s | %-" + width + "s | %s%n";
        String rule = "-".repeat(8) + "-+-" + "-".repeat(width) + "-+-" + "-".repeat(9) + System.lineSeparator();
        StringBuilder out = new StringBuilder(System.lineSeparator());
        out.append(String.format(format, "ID", "Scenario", "Status")).append(rule);
        for (Row r : snapshot) {
            // plain text: the Windows console cannot render the emoji used in the HTML
            out.append(String.format(format, r.id(), r.scenario(), r.status().name()));
        }
        return out.toString();
    }

    private static String statusLabel(Status status) {
        return switch (status) {
            case PASS -> "✅ PASS";
            case FAIL -> "❌ FAIL";
            case SKIP -> "⚠️ SKIP";
        };
    }

    private static String tile(String value, String label) {
        return "<div class=\"tile\"><b>" + value + "</b><span>" + label + "</span></div>";
    }

    private static String segment(long count, int total, String color) {
        if (count == 0) {
            return "";
        }
        return "<i style=\"width:" + (count * 100.0 / total) + "%;background:" + color + "\"></i>";
    }

    private static String formatDuration(long ms) {
        if (ms < 60_000) {
            return String.format(Locale.ENGLISH, "%.1fs", ms / 1000.0);
        }
        return String.format(Locale.ENGLISH, "%dm %02ds", ms / 60_000, (ms / 1000) % 60);
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "No reason recorded";
        }
        String line = text.strip().lines().findFirst().orElse("");
        return line.length() > 220 ? line.substring(0, 220) + "..." : line;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
