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

    /** "Log out" only exists inside the open menu, so it marks the menu as open (first match = menu item). */
    private final By logOutButton = By.xpath("(//*[normalize-space(text())='Log out'])[1]");

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

    /** Every item seen in the menu (screenshot 2026-10-13), top to bottom. */
    public static final java.util.List<String> ITEMS = java.util.List.of(
            "Today", "Interactions", "Admissions", "Master Search",
            "Performance", "Leaderboard", "Reports", "Report Issue");

    /** True when the menu item with this label is visible in the open menu. */
    public boolean isItemShown(String label) {
        return driver.findElements(menuItem(label)).stream().anyMatch(WebElement::isDisplayed);
    }

    /**
     * "Log out?" confirmation dialog (seen 2026-10-13): "You'll need to sign in again to continue."
     * with Cancel and a red Log out button. Buttons are found inside the dialog, so the menu's own
     * "Log out" item is not clicked by mistake.
     */
    private final By logOutDialog = By.xpath("//*[contains(text(), 'need to sign in again')]");
    private final By dialogLogOut = By.xpath("//*[contains(text(), 'need to sign in again')]"
            + "/ancestor::*[.//*[normalize-space(text())='Cancel']][1]//*[normalize-space(text())='Log out']");
    private final By dialogCancel = By.xpath("//*[contains(text(), 'need to sign in again')]"
            + "/ancestor::*[.//*[normalize-space(text())='Cancel']][1]//*[normalize-space(text())='Cancel']");

    /** Clicks "Log out" in the menu and confirms it in the "Log out?" dialog. */
    public LoginPage clickLogOut() {
        openLogOutDialog();
        LOG.info("Confirming Log out");
        click(dialogLogOut);
        return new LoginPage(driver);
    }

    /** Clicks "Log out" in the menu, then Cancel in the "Log out?" dialog - the user stays logged in. */
    public void clickLogOutThenCancel() {
        openLogOutDialog();
        LOG.info("Cancelling Log out");
        click(dialogCancel);
    }

    /** True while the "Log out?" confirmation dialog is shown. */
    public boolean isLogOutDialogShown() {
        return driver.findElements(logOutDialog).stream().anyMatch(WebElement::isDisplayed);
    }

    private void openLogOutDialog() {
        LOG.info("Clicking Log out in the menu");
        click(logOutButton);
        wait.waitForVisible(logOutDialog);
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
        LOG.info("Clicking '{}' in the menu", label);
        click(menuItem(label));
    }

    private static By menuItem(String label) {
        return By.xpath("//*[normalize-space(text())='Log out']"
                + "/ancestor::*[.//*[normalize-space(text())='Admissions']][1]"
                + "//*[normalize-space(text())='" + label + "']");
    }
}
