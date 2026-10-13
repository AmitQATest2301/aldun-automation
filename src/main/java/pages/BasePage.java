package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import utils.ConfigReader;
import utils.WaitUtils;

/**
 * Shared helpers for all page objects so each page does not repeat wait/type/click code.
 * Holds no locators and no assertions.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WaitUtils wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    protected void click(By locator) {
        wait.waitForClickable(locator).click();
        WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
    }

    /**
     * Clears then types. The app is React Native Web, where WebElement.clear() does not
     * always update React state, so the field is cleared with Ctrl+A + Delete instead.
     */
    protected void type(By locator, String text) {
        String expected = text == null ? "" : text;
        // The app can reset a field it is still initialising (seen 2026-10-13: the email typed
        // right after page load came back empty), so check the value and retype if needed.
        for (int attempt = 1; attempt <= TYPE_ATTEMPTS; attempt++) {
            WebElement field = wait.waitForVisible(locator);
            field.click();
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            if (!expected.isEmpty()) {
                field.sendKeys(expected);
            }
            WaitUtils.pause(TYPE_SETTLE_MS);
            if (expected.equals(wait.waitForVisible(locator).getDomProperty("value"))) {
                return;
            }
        }
        throw new IllegalStateException("Field " + locator + " did not keep the typed value after "
                + TYPE_ATTEMPTS + " attempts");
    }

    private static final int TYPE_ATTEMPTS = 3;
    /** Short settle time so the app can react before the typed value is checked. */
    private static final long TYPE_SETTLE_MS = 300;

    /**
     * Types WITHOUT checking the result - for negative tests where the field is expected
     * to refuse or change the input (e.g. letters in a phone number field).
     */
    protected void typeRaw(By locator, String text) {
        WebElement field = wait.waitForVisible(locator);
        field.click();
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        field.sendKeys(text);
        WaitUtils.pause(TYPE_SETTLE_MS);
    }

    /** Opens a path on the app's site, e.g. "/staff" or "/login". */
    public void openPath(String path) {
        String base = ConfigReader.get("baseUrl");
        String origin = base.substring(0, base.indexOf('/', "https://".length()));
        driver.get(origin + path);
    }

    /** Visible text of the whole page. */
    public String getPageText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    protected boolean isDisplayed(By locator) {
        return wait.waitForVisible(locator).isDisplayed();
    }

    /**
     * If the app asks for confirmation (e.g. the record already exists), clicks the
     * "go ahead" button so the action still completes. Returns true if one was clicked.
     * The exact wording of Aldun's duplicate prompt is not known yet, so common labels are tried.
     */
    public boolean confirmIfAsked() {
        // "Create anyway" is Aldun's button when the record already exists (confirmed by the user
        // 2026-10-13). Matched case-insensitively on any element; other labels are fallbacks.
        By createAnyway = By.xpath("//*[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                + "'abcdefghijklmnopqrstuvwxyz')='create anyway']");
        By otherConfirm = By.xpath("//button[" + String.join(" or ",
                "normalize-space(.)='Continue'", "normalize-space(.)='Proceed'",
                "normalize-space(.)='Confirm'", "normalize-space(.)='Yes'", "normalize-space(.)='OK'",
                "normalize-space(.)='Ok'") + "]");
        try {
            WebElement button = new WaitUtils(driver, CONFIRM_PROMPT_WAIT_SECONDS).until(d ->
                    d.findElements(createAnyway).stream().filter(WebElement::isDisplayed).findFirst()
                     .orElseGet(() -> d.findElements(otherConfirm).stream()
                             .filter(WebElement::isDisplayed).findFirst().orElse(null)));
            String label = button.getText().trim();
            button.click();
            org.apache.logging.log4j.LogManager.getLogger(getClass())
                    .info("Confirmation prompt shown - clicked '{}'", label);
            WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false; // no prompt: nothing to confirm
        }
    }

    /** How long to look for a confirmation prompt after an action. */
    private static final int CONFIRM_PROMPT_WAIT_SECONDS = 5;

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
