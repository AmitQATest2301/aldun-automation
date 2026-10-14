package pages;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import utils.ConfigReader;
import utils.WaitUtils;

/**
 * The screens opened from the side menu: Admissions, Master Search, Performance, Leaderboard,
 * Reports and Report Issue. Facts recorded from the live app on 2026-10-14:
 * <ul>
 *   <li>Admissions: /staff/my-work?filter=ADMISSIONS, search [data-testid=admissions-list-search]
 *       "Phone, Adm ID, UHID or Name", "Created by me", "Filter by case status", rows admission-row-&lt;id&gt;.</li>
 *   <li>Master Search: /staff/my-work?filter=MASTER_SEARCH, [data-testid=master-search-input],
 *       Search [data-testid=master-search-submit] disabled while empty.</li>
 *   <li>Performance: /staff/performance, Today / This month, MY PROGRESS, HOSPITAL PROGRESS, Show incentive.</li>
 *   <li>Leaderboard: /staff/leaderboard, Today / This month, "N discharges of M target" and "NN%".</li>
 *   <li>Reports: /staff/reports, Report = Interaction Report, Date range, Generate Report disabled
 *       until a date range is chosen, Requested reports with "Ready" and Download report buttons.</li>
 *   <li>Report Issue: /staff/report-issue, Raised by me / To do / Validating / Completed, Search by title,
 *       rows report-issue-row-&lt;id&gt;, "Report a new issue". The menu badge clears once it is opened.</li>
 * </ul>
 * Read-only by design: nothing here submits, generates or downloads anything.
 */
public class MenuScreens extends BasePage {

    private static final Logger LOG = LogManager.getLogger(MenuScreens.class);

    public static final String ADMISSIONS_SEARCH = "admissions-list-search";
    public static final String INTERACTIONS_SEARCH = "interactions-list-search";
    public static final String MASTER_SEARCH_INPUT = "master-search-input";
    public static final String MASTER_SEARCH_SUBMIT = "master-search-submit";

    /** A row id is the prefix followed by a 36-character id (its inner buttons add a suffix). */
    private static final int ROW_ID_LENGTH = 36;
    private static final int QUICK_SECONDS = 5;
    private static final Pattern PHONE = Pattern.compile("\\b\\d{10}\\b");

    public MenuScreens(WebDriver driver) {
        super(driver);
    }

    // ---------------- finding things ----------------

    private static By aria(String label) {
        return By.xpath("//*[@aria-label=\"" + label + "\"]");
    }

    private static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    private WebElement visible(By locator) {
        return wait.until(d -> d.findElements(locator).stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
    }

    private static boolean enabled(WebElement e) {
        return e.isEnabled() && !"true".equals(e.getDomAttribute("aria-disabled"));
    }

    // ---------------- page state ----------------

    public boolean waitForUrl(String fragment) {
        try {
            return wait.waitForUrlContaining(fragment);
        } catch (TimeoutException e) {
            LOG.warn("URL does not contain '{}': {}", fragment, driver.getCurrentUrl());
            return false;
        }
    }

    public boolean isTextShown(String text) {
        try {
            return wait.waitForPageText(text);
        } catch (TimeoutException e) {
            LOG.warn("'{}' not found in page text", text);
            return false;
        }
    }

    /** True if the text is (or becomes) absent from the page within a few seconds. */
    public boolean isTextGone(String text) {
        try {
            return new WaitUtils(driver, QUICK_SECONDS).until(d -> !getPageText().contains(text));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** True if any page text matches the regular expression. */
    public boolean pageMatches(String regex) {
        try {
            return wait.until(d -> Pattern.compile(regex).matcher(getPageText()).find());
        } catch (TimeoutException e) {
            LOG.warn("No page text matches /{}/", regex);
            return false;
        }
    }

    /** The page's visible lines, trimmed. */
    public List<String> pageLines() {
        return getPageText().lines().map(String::trim).toList();
    }

    public boolean isAriaShown(String label) {
        try {
            return visible(aria(label)) != null;
        } catch (TimeoutException e) {
            LOG.warn("No visible element labelled '{}'", label);
            return false;
        }
    }

    public boolean isAriaEnabled(String label) {
        return enabled(visible(aria(label)));
    }

    public int countAria(String label) {
        return (int) driver.findElements(aria(label)).stream().filter(WebElement::isDisplayed).count();
    }

    public void clickAria(String label) {
        LOG.info("Clicking '{}'", label);
        wait.waitForClickable(aria(label));
        visible(aria(label)).click();
        WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
    }

    /**
     * Clicks a tab such as "Today" or "This month". The closed side menu also holds a "Today"
     * button earlier in the page, so the LAST visible match is the screen's own tab.
     */
    public void clickTab(String label) {
        By tab = By.xpath("//*[(self::button or @role='button') and normalize-space(.)='" + label + "']");
        List<WebElement> tabs = driver.findElements(tab).stream().filter(WebElement::isDisplayed).toList();
        if (tabs.isEmpty()) {
            throw new IllegalStateException("Tab not found: " + label);
        }
        LOG.info("Clicking tab '{}'", label);
        tabs.get(tabs.size() - 1).click();
        WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
    }

    public void pressEscape() {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
    }

    // ---------------- fields and rows ----------------

    public boolean isFieldShown(String id) {
        try {
            return visible(testId(id)) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public String placeholderOf(String id) {
        return visible(testId(id)).getDomAttribute("placeholder");
    }

    public void typeInto(String id, String text) {
        type(testId(id), text);
    }

    /** Types without checking what the field kept - for edge input such as spaces only. */
    public void typeRawInto(String id, String text) {
        typeRaw(testId(id), text);
    }

    public boolean isEnabledById(String id) {
        return enabled(visible(testId(id)));
    }

    /** True once the element becomes enabled (the app enables buttons as you type). */
    public boolean becomesEnabled(String id) {
        try {
            return new WaitUtils(driver, QUICK_SECONDS).until(d -> enabled(visible(testId(id))));
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void clickById(String id) {
        LOG.info("Clicking [data-testid={}]", id);
        click(testId(id));
    }

    /** Number of list rows whose data-testid is the prefix plus a row id, e.g. admission-row-&lt;id&gt;. */
    public int countRows(String prefix) {
        By rows = By.xpath("//*[starts-with(@data-testid, '" + prefix + "') and string-length(@data-testid) = "
                + (prefix.length() + ROW_ID_LENGTH) + "]");
        return (int) driver.findElements(rows).stream().filter(WebElement::isDisplayed).count();
    }

    /** Waits for list rows to appear and returns how many there are (0 if none appear). */
    public int waitForRows(String prefix) {
        try {
            return new WaitUtils(driver, QUICK_SECONDS * 2).until(d -> {
                int n = countRows(prefix);
                return n > 0 ? n : null;
            });
        } catch (TimeoutException e) {
            return 0;
        }
    }

    /** True if, within a few seconds, no rows with this prefix are shown. */
    public boolean rowsGone(String prefix) {
        try {
            return new WaitUtils(driver, QUICK_SECONDS * 2).until(d -> countRows(prefix) == 0);
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** First 10-digit phone number visible on the page, or null. */
    public String firstPhoneOnPage() {
        Matcher m = PHONE.matcher(getPageText());
        return m.find() ? m.group() : null;
    }
}
