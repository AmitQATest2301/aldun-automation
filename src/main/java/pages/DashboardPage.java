package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import utils.ConfigReader;
import utils.WaitUtils;

/**
 * Staff home screen shown after a successful login (https://team.aldun.com/staff).
 * Based on the user's screenshots and test runs (2026-10-13):
 * "Today", "Hello, <name>", "Not checked in", "Your shift", "Check in", "Log Interaction".
 * After clicking Check in the app shows a "Checking in..." screen before returning.
 */
public class DashboardPage extends BasePage {

    private static final Logger LOG = LogManager.getLogger(DashboardPage.class);

    /**
     * Upper limit for the check-in / check-out server response. The waits end as soon as
     * the app responds; this only matters when something goes wrong.
     */
    private static final int CHECK_IN_TIMEOUT_SECONDS = 20;

    private final By greetingText = By.xpath("//*[starts-with(normalize-space(text()), 'Hello,')]");
    private final By checkInButton = textIgnoringCase("check in");
    private final By logInteractionButton = textIgnoringCase("log interaction");
    private final By checkOutButton = textIgnoringCase("check out");
    private final By pageBody = By.tagName("body");

    private static By textIgnoringCase(String lowerCaseText) {
        return By.xpath("//*[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
                + "'abcdefghijklmnopqrstuvwxyz')='" + lowerCaseText + "']");
    }

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Waits until the staff screen is fully visible: URL contains the dashboard path
     * and the "Hello, ..." greeting is shown. Returns false if that does not happen in time.
     */
    public boolean isLoaded() {
        String dashboardPath = ConfigReader.get("dashboardPath");
        try {
            wait.waitForUrlContaining(dashboardPath);
            wait.waitForVisible(greetingText);
            LOG.info("Dashboard visible at {} - '{}'", driver.getCurrentUrl(), getGreeting());
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Dashboard did not appear. Current URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    /** e.g. "Hello, Test" */
    public String getGreeting() {
        return wait.waitForVisible(greetingText).getText().trim();
    }

    // Labels are checked against the rendered page text: the "Today" label could not be
    // matched by an element locator, but it is present in the visible text.
    public boolean isTodayLabelDisplayed() {
        return isTextShown("Today");
    }

    public boolean isShiftSectionDisplayed() {
        return isTextShown("Your shift");
    }

    public boolean isCheckInButtonDisplayed() {
        return isDisplayed(checkInButton);
    }

    /** "Check in" when not checked in, "Check out" when checked in - either one counts. */
    public boolean isAttendanceButtonDisplayed() {
        try {
            return wait.until(d -> isShownNow(checkInButton) || isShownNow(checkOutButton));
        } catch (TimeoutException e) {
            LOG.warn("Neither 'Check in' nor 'Check out' button shown");
            return false;
        }
    }

    /** Opens the staff home screen directly (the app sometimes moves to another screen). */
    public void open() {
        String base = ConfigReader.get("baseUrl");
        String origin = base.substring(0, base.indexOf('/', "https://".length()));
        driver.get(origin + ConfigReader.get("dashboardPath"));
    }

    public boolean isLogInteractionButtonDisplayed() {
        return isDisplayed(logInteractionButton);
    }

    // ---------- attendance ----------

    /** True when the status currently reads "Not checked in" (no waiting). */
    public boolean isNotCheckedIn() {
        return pageTextLower().contains("not checked in");
    }

    public void clickCheckIn() {
        LOG.info("Clicking Check in");
        click(checkInButton);
    }

    /** True when the status reads "Checked in" (and not "Not checked in"). */
    public boolean isCheckedIn() {
        return pageTextLower().replace("not checked in", "").contains("checked in");
    }

    /**
     * Waits for check-in to finish: "Checking in..." is gone and either a "Checked in" status
     * is shown or the app has moved to the Interactions list (seen 2026-10-13: after Check in
     * the app opens the Interactions list). Then reopens the home screen and confirms the status.
     */
    public boolean waitUntilCheckedIn() {
        return waitForCheckInResponse() && confirmCheckedInOnHome();
    }

    /** Waits for "Checking in..." to finish and the app to show "Checked in" or the Interactions list. */
    public boolean waitForCheckInResponse() {
        try {
            new WaitUtils(driver, CHECK_IN_TIMEOUT_SECONDS).until(d -> {
                String text = pageTextLower();
                // The Interactions list is recognised by its content, not its URL
                boolean onInteractionsList = text.contains("created by me") && text.contains("new interaction");
                return !text.contains("checking in") && (isCheckedIn() || onInteractionsList);
            });
            LOG.info("Check-in response received, now at {}", driver.getCurrentUrl());
            return true;
        } catch (TimeoutException e) {
            LOG.warn("No 'Checked in' confirmation within {}s. Page shows:\n{}",
                    CHECK_IN_TIMEOUT_SECONDS, getVisibleText());
            return false;
        }
    }

    /** Reopens the home screen and confirms the status reads "Checked in". */
    public boolean confirmCheckedInOnHome() {
        open();
        boolean checkedIn = isLoaded() && waitForText(this::isCheckedIn);
        LOG.info("After check-in, home screen status is 'Checked in': {}", checkedIn);
        return checkedIn;
    }

    /** True when a "Check out" button is currently shown (no waiting). */
    public boolean isCheckOutButtonDisplayed() {
        return isShownNow(checkOutButton);
    }

    public void clickCheckOut() {
        LOG.info("Clicking Check out");
        click(checkOutButton);
    }

    /**
     * Waits for check-out to finish (a "Checked out" confirmation or "Not checked in" status;
     * seen 2026-10-13: a "Checked out" popup, then the app moved to the Interactions list),
     * then reopens the home screen and confirms the status reads "Not checked in".
     */
    public boolean waitUntilCheckedOut() {
        try {
            new WaitUtils(driver, CHECK_IN_TIMEOUT_SECONDS).until(d -> {
                String text = pageTextLower();
                return !text.contains("checking out")
                        && (text.contains("checked out") || text.contains("not checked in"));
            });
        } catch (TimeoutException e) {
            LOG.warn("No 'Checked out' confirmation within {}s. Page shows:\n{}",
                    CHECK_IN_TIMEOUT_SECONDS, getVisibleText());
            return false;
        }
        open();
        boolean checkedOut = isLoaded() && waitForText(this::isNotCheckedIn);
        LOG.info("After check-out, home screen status is 'Not checked in': {}", checkedOut);
        return checkedOut;
    }

    // ---------- interactions ----------

    public void clickLogInteraction() {
        LOG.info("Clicking Log Interaction");
        click(logInteractionButton);
    }

    /**
     * Waits until the screen changes after clicking Log Interaction: either the URL
     * changes or the staff home greeting is no longer the page content.
     */
    public boolean waitUntilLeftHomeScreen(String homeUrl) {
        try {
            wait.until(d -> !d.getCurrentUrl().equals(homeUrl) || !pageTextLower().contains("hello,"));
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Screen did not change after clicking Log Interaction");
            return false;
        }
    }

    // ---------- page text ----------

    /** Visible text of the whole page, for logging what the app shows. */
    public String getVisibleText() {
        return driver.findElement(pageBody).getText();
    }

    private String pageTextLower() {
        return getVisibleText().toLowerCase();
    }

    private boolean isShownNow(By locator) {
        return driver.findElements(locator).stream().anyMatch(e -> e.isDisplayed());
    }

    private boolean waitForText(java.util.function.BooleanSupplier condition) {
        try {
            return wait.until(d -> condition.getAsBoolean());
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean isTextShown(String text) {
        try {
            return wait.waitForPageText(text);
        } catch (TimeoutException e) {
            LOG.warn("'{}' not found in page text", text);
            return false;
        }
    }
}
