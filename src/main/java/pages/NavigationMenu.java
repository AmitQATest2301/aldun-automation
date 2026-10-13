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
 * Side menu opened with the three-lines button in the top-left corner.
 * Seen in the user's screenshot (2026-10-13): header "Test User" / "Hops staff" with a close (X)
 * button; items Today, Interactions, Admissions, Master Search, Performance, Leaderboard,
 * Reports, Report Issue; and a "Log out" button at the bottom.
 */
public class NavigationMenu extends BasePage {

    private static final Logger LOG = LogManager.getLogger(NavigationMenu.class);

    private static final String LOWER = "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'";

    /** Preferred: a button whose aria-label mentions "menu". */
    private final By menuButtonByLabel = By.xpath("//*[(self::button or @role='button') and "
            + "contains(translate(@aria-label, " + LOWER + "), 'menu')]");
    /** Fallback: the first button before the page's main title (the icon left of the title). */
    private final By menuButtonBeforeTitle = By.xpath("(//*[normalize-space(text())='Interactions' "
            + "or normalize-space(text())='New Interaction' or normalize-space(text())='Create New Interaction' "
            + "or starts-with(normalize-space(text()), 'Hello,')])[1]"
            + "/preceding::*[self::button or @role='button'][1]");

    /** "Log out" only exists inside the open menu, so it marks the menu as open. */
    private final By logOutButton = By.xpath("//*[normalize-space(text())='Log out']");

    public NavigationMenu(WebDriver driver) {
        super(driver);
    }

    public NavigationMenu open() {
        WebElement button = driver.findElements(menuButtonByLabel).stream()
                .filter(WebElement::isDisplayed).findFirst()
                .orElseGet(() -> wait.waitForClickable(menuButtonBeforeTitle));
        LOG.info("Opening the menu (three lines, top-left)");
        button.click();
        wait.waitForVisible(logOutButton);
        WaitUtils.pause(ConfigReader.getInt("stepDelayMs"));
        return this;
    }

    public boolean isOpen() {
        try {
            return isDisplayed(logOutButton);
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Clicks "Today" in the menu, which leads back to the staff home screen. */
    public DashboardPage clickToday() {
        clickItem("Today");
        return new DashboardPage(driver);
    }

    /**
     * Clicks a menu item by its label (Today, Interactions, Admissions, Master Search,
     * Performance, Leaderboard, Reports, Report Issue). The search is limited to the menu
     * panel (the smallest container holding both "Log out" and "Admissions"), so e.g. the
     * page's own "Today" label is not clicked by mistake.
     */
    public void clickItem(String label) {
        By item = By.xpath("//*[normalize-space(text())='Log out']"
                + "/ancestor::*[.//*[normalize-space(text())='Admissions']][1]"
                + "//*[normalize-space(text())='" + label + "']");
        LOG.info("Clicking '{}' in the menu", label);
        click(item);
    }
}
