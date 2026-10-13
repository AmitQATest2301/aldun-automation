package utils;

import java.time.Duration;
import java.util.function.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Explicit-wait helpers. Timeout comes from config key "explicitWait". */
public class WaitUtils {

    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver) {
        this(driver, ConfigReader.getInt("explicitWait"));
    }

    public WaitUtils(WebDriver driver, int timeoutSeconds) {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        // The app re-renders often; an element replaced mid-check is simply looked up again
        this.wait.ignoring(org.openqa.selenium.StaleElementReferenceException.class);
    }

    /*
     * The app keeps earlier screens in the page (hidden) after navigating, so the FIRST match of
     * a locator can be an invisible copy from a previous screen (seen 2026-10-13: "Attender Phone No"
     * and "New interaction"). These waits therefore return the first VISIBLE match.
     */

    public WebElement waitForVisible(By locator) {
        return wait.until(driver -> driver.findElements(locator).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(driver -> driver.findElements(locator).stream()
                .filter(e -> e.isDisplayed() && e.isEnabled()).findFirst().orElse(null));
    }

    /** True once the element is gone or hidden. */
    public boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForUrlContaining(String fragment) {
        return wait.until(ExpectedConditions.urlContains(fragment));
    }

    public boolean waitForUrlNotContaining(String fragment) {
        return wait.until(ExpectedConditions.not(ExpectedConditions.urlContains(fragment)));
    }

    public boolean waitForAttribute(By locator, String attribute, String value) {
        return wait.until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }

    /**
     * Fixed pause. Use only where a deliberate delay is required (e.g. the
     * configurable pause after typing credentials); prefer the explicit waits above.
     */
    public static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pause interrupted", e);
        }
    }

    /** Waits for any custom condition (returns its first non-null / true value). */
    public <T> T until(Function<WebDriver, T> condition) {
        return wait.until(condition);
    }

    /** Waits until the page's visible text contains the given text (case-insensitive). */
    public boolean waitForPageText(String text) {
        String expected = text.toLowerCase();
        return wait.until(driver ->
                driver.findElement(By.tagName("body")).getText().toLowerCase().contains(expected));
    }

    /** Waits until the element's visible text is non-empty and returns it. */
    public String waitForNonEmptyText(By locator) {
        return wait.until(driver -> {
            String text = driver.findElement(locator).getText().trim();
            return text.isEmpty() ? null : text;
        });
    }
}
