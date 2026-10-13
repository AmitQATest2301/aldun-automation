package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import utils.ConfigReader;
import utils.WaitUtils;

/**
 * Page object for https://team.aldun.com/login.
 * All locators were taken from the live DOM on 2026-10-13 (see PROJECT_NOTES.md).
 */
public class LoginPage extends BasePage {

    private static final Logger LOG = LogManager.getLogger(LoginPage.class);

    private final By emailInput = By.cssSelector("[data-testid='login-email-input']");
    private final By passwordInput = By.cssSelector("[data-testid='login-password-input']");
    private final By showPasswordButton = By.cssSelector("button[aria-label='Show password']");
    private final By hidePasswordButton = By.cssSelector("button[aria-label='Hide password']");
    private final By loginButton = By.cssSelector("button[aria-label='Log in']");
    private final By forgotPasswordButton = By.xpath("//button[normalize-space()='Forgot password?']");
    private final By ssoButton = By.cssSelector("[data-testid='login-sso-btn']");
    private final By toastContainer = By.cssSelector("[data-testid='toastAnimatedContainer']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ---------- actions ----------

    public LoginPage enterEmail(String email) {
        type(emailInput, email);
        WaitUtils.pause(ConfigReader.getInt("afterTypingPauseMs"));
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(passwordInput, password);
        WaitUtils.pause(ConfigReader.getInt("afterTypingPauseMs"));
        return this;
    }

    public void clickLogin() {
        click(loginButton);
    }

    /** Types the email exactly as given, without checking it (the app may trim spaces). */
    public LoginPage enterEmailAsTyped(String email) {
        typeRaw(emailInput, email);
        return this;
    }

    /**
     * Clicks Log in several times as fast as possible (edge case: impatient double-clicking).
     * Stops early if the button disappears because the app already moved on.
     */
    public void clickLoginRepeatedly(int times) {
        WebElement button = wait.waitForClickable(loginButton);
        for (int i = 1; i <= times; i++) {
            try {
                button.click();
            } catch (org.openqa.selenium.StaleElementReferenceException
                     | org.openqa.selenium.ElementNotInteractableException e) {
                LOG.info("Log in button gone after {} click(s) - app already moved on", i - 1);
                return;
            }
        }
        LOG.info("Clicked Log in {} times", times);
    }

    /** Fills both fields and clicks Log in. */
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        LOG.info("Submitting login for email '{}'", email);
        clickLogin();
    }

    public void showPassword() {
        click(showPasswordButton);
        wait.waitForVisible(hidePasswordButton);
    }

    public void hidePassword() {
        click(hidePasswordButton);
        wait.waitForVisible(showPasswordButton);
    }

    // ---------- state / values ----------

    public boolean isEmailFieldDisplayed() {
        return isDisplayed(emailInput);
    }

    public boolean isPasswordFieldDisplayed() {
        return isDisplayed(passwordInput);
    }

    public boolean isLoginButtonDisplayed() {
        return isDisplayed(loginButton);
    }

    public boolean isForgotPasswordDisplayed() {
        return isDisplayed(forgotPasswordButton);
    }

    public boolean isSsoButtonDisplayed() {
        return isDisplayed(ssoButton);
    }

    /** The app disables the button with both the disabled property and aria-disabled="true". */
    public boolean isLoginButtonEnabled() {
        WebElement button = wait.waitForVisible(loginButton);
        return button.isEnabled() && !"true".equals(button.getDomAttribute("aria-disabled"));
    }

    /** "password" when masked, "text" when visible. */
    public String getPasswordFieldType() {
        return wait.waitForVisible(passwordInput).getDomProperty("type");
    }

    public String getEmailFieldValue() {
        return wait.waitForVisible(emailInput).getDomProperty("value");
    }

    public String getPasswordFieldValue() {
        return wait.waitForVisible(passwordInput).getDomProperty("value");
    }

    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains(ConfigReader.get("loginPath"))
                && isEmailFieldDisplayed();
    }

    /**
     * After a failed login, waits for the page to show something new (an error message,
     * wherever the app puts it) and returns the new text. Empty string if nothing appears.
     */
    public String waitForNewMessage(String pageTextBefore) {
        try {
            wait.until(d -> !getPageText().equals(pageTextBefore));
        } catch (TimeoutException e) {
            LOG.warn("No new message appeared on the login page");
            return "";
        }
        java.util.Set<String> before = new java.util.HashSet<>(java.util.Arrays.asList(pageTextBefore.split("\\R")));
        String added = java.util.Arrays.stream(getPageText().split("\\R"))
                .filter(line -> !line.isBlank() && !before.contains(line))
                .collect(java.util.stream.Collectors.joining(" | "));
        LOG.info("Message shown: '{}'", added);
        return added;
    }

    /** Text currently in the toast area, without waiting. Empty string when nothing is shown. */
    public String getCurrentToastText() {
        return driver.findElements(toastContainer).stream()
                .map(e -> e.getText().trim())
                .filter(t -> !t.isEmpty())
                .findFirst()
                .orElse("");
    }

    /**
     * Waits for a message to appear in the toast area after a failed login.
     * Returns an empty string if none appears within the explicit wait.
     * NOTE: that login errors use this toast area is not yet confirmed against the live app.
     */
    public String waitForErrorMessage() {
        try {
            return wait.waitForNonEmptyText(toastContainer);
        } catch (TimeoutException e) {
            LOG.warn("No toast message appeared within the explicit wait");
            return "";
        }
    }
}
