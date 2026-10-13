package listeners;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import base.DriverFactory;
import listeners.SummaryReport.Status;
import utils.ConfigReader;
import utils.DateUtils;
import utils.ScreenshotUtil;

/**
 * Produces two reports per run and screenshots failures:
 *  - reports/TestSummary_<time>.html  : one-page summary (opened automatically at the end)
 *  - reports/ExtentReport_<time>.html : detailed ExtentReports view
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LogManager.getLogger(TestListener.class);
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private static ExtentReports extent;
    private static SummaryReport summary;
    private static Path reportDir;
    private static Path extentPath;
    private static Path summaryPath;
    private static LocalDateTime suiteStart;

    // ===================== suite =====================

    @Override
    public void onStart(ISuite suite) {
        suiteStart = LocalDateTime.now();
        String stamp = DateUtils.fileTimestamp();
        reportDir = Paths.get(ConfigReader.get("reportDir")).toAbsolutePath();
        extentPath = reportDir.resolve("ExtentReport_" + stamp + ".html");
        summaryPath = reportDir.resolve("TestSummary_" + stamp + ".html");

        ExtentSparkReporter spark = new ExtentSparkReporter(extentPath.toString());
        spark.config().setDocumentTitle("Aldun Automation Report");
        spark.config().setReportName(suite.getName());
        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Browser", ConfigReader.get("browser"));
        extent.setSystemInfo("URL", ConfigReader.get("baseUrl"));
        extent.setSystemInfo("Java", System.getProperty("java.version"));

        summary = new SummaryReport(ConfigReader.get("testIdPrefix"));
        LOG.info("Reports for this run: {} and {}", summaryPath, extentPath);
    }

    /**
     * Runs after every test and its @AfterMethod (browser quit) has finished,
     * so the report opens only once the test browser is closed.
     */
    @Override
    public void onFinish(ISuite suite) {
        if (extent == null) {
            return;
        }
        extent.flush();
        try {
            summary.write(summaryPath, suite.getName(), suiteStart, LocalDateTime.now(),
                    ConfigReader.get("browser"), ConfigReader.get("baseUrl"),
                    extentPath.getFileName().toString());
            LOG.info("Test results:{}", summary.toConsoleTable());
            LOG.info("Summary report written: {}", summaryPath);
        } catch (IOException e) {
            LOG.error("Could not write summary report {}", summaryPath, e);
            return;
        }
        if (ConfigReader.getBoolean("autoOpenReport")) {
            openInBrowser(summaryPath);
        }
    }

    // ===================== tests =====================

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("START  {}", displayName(result));
        CURRENT.set(extent.createTest(displayName(result), result.getMethod().getDescription()));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS   {}", displayName(result));
        CURRENT.get().pass("Passed");
        record(result, Status.PASS, null, null);
        CURRENT.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String name = displayName(result);
        String message = messageOf(result);
        LOG.error("FAIL   {} - {}", name, message);
        ExtentTest test = CURRENT.get();
        test.fail(result.getThrowable());

        String relativeShot = null;
        WebDriver driver = DriverFactory.getDriver();
        if (driver != null) {
            try {
                String shot = ScreenshotUtil.capture(driver, name);
                test.fail("Screenshot", MediaEntityBuilder.createScreenCaptureFromPath(shot).build());
                relativeShot = reportDir.relativize(Paths.get(shot)).toString();
                LOG.info("Screenshot saved: {}", shot);
            } catch (IOException | RuntimeException e) {
                LOG.error("Could not capture screenshot for {}", name, e);
                test.warning("Screenshot capture failed: " + e.getMessage());
            }
        }
        record(result, Status.FAIL, message, relativeShot);
        CURRENT.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String reason = messageOf(result);
        LOG.warn("SKIP   {} - {}", displayName(result), reason);
        ExtentTest test = CURRENT.get();
        if (test == null) { // skipped before onTestStart, e.g. setup failure
            test = extent.createTest(displayName(result), result.getMethod().getDescription());
        }
        test.skip("Skipped: " + reason);
        record(result, Status.SKIP, reason, null);
        CURRENT.remove();
    }

    // ===================== helpers =====================

    private static void record(ITestResult result, Status status, String message, String screenshot) {
        long duration = Math.max(0, result.getEndMillis() - result.getStartMillis());
        summary.add(scenarioOf(result), displayName(result), status, duration, message, screenshot);
    }

    /** Readable scenario: the DataProvider row name if any, otherwise the @Test description. */
    private static String scenarioOf(ITestResult result) {
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            return String.valueOf(params[0]);
        }
        String description = result.getMethod().getDescription();
        return (description == null || description.isBlank())
                ? result.getMethod().getMethodName() : description;
    }

    /** Method name plus the first DataProvider argument (the scenario), when present. */
    private static String displayName(ITestResult result) {
        Object[] params = result.getParameters();
        String base = result.getMethod().getMethodName();
        return (params == null || params.length == 0) ? base : base + " [" + params[0] + "]";
    }

    private static String messageOf(ITestResult result) {
        Throwable t = result.getThrowable();
        if (t == null) {
            return "";
        }
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    private static void openInBrowser(Path file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(file.toUri());
            } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "\"\"", file.toString()).start();
            } else {
                LOG.warn("Cannot auto-open report on this system. Open it manually: {}", file);
                return;
            }
            LOG.info("Opened report in the default browser: {}", file);
        } catch (IOException | RuntimeException e) {
            LOG.error("Could not open report automatically. Open it manually: {}", file, e);
        }
    }
}
