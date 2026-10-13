package base;

import java.time.Duration;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import utils.ConfigReader;
import utils.WaitUtils;

/**
 * Creates and quits the browser. One driver per thread so tests can run in parallel later.
 * Selenium Manager (built into Selenium 4) downloads the matching msedgedriver automatically.
 */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver initDriver() {
        String browser = ConfigReader.get("browser").toLowerCase();
        boolean headless = ConfigReader.getBoolean("headless");

        WebDriver driver = switch (browser) {
            case "edge" -> createEdge(headless);
            case "chrome" -> createChrome(headless);
            default -> throw new IllegalArgumentException("Unsupported browser in config.properties: " + browser);
        };

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("pageLoadTimeout")));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO); // explicit waits only
        if (!headless) {
            driver.manage().window().maximize();
        }
        DRIVER.set(driver);

        String url = ConfigReader.get("baseUrl");
        LOG.info("Started {} (headless={}), opening {}", browser, headless, url);
        driver.get(url);
        WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
        return driver;
    }

    /** 1 = allow. Lets the app read location (e.g. for Check in) without a browser popup Selenium cannot click. */
    private static final Map<String, Object> BROWSER_PREFS =
            Map.of("profile.default_content_setting_values.geolocation", 1);

    private static WebDriver createEdge(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.setExperimentalOption("prefs", BROWSER_PREFS);
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        return new EdgeDriver(options);
    }

    private static WebDriver createChrome(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", BROWSER_PREFS);
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        return new ChromeDriver(options);
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                WaitUtils.pause(ConfigReader.getInt("beforeCloseDelayMs")); // let the final screen be seen
                driver.quit();
                LOG.info("Browser closed");
            } finally {
                DRIVER.remove();
            }
        }
    }
}
