package tests;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.DashboardPage;
import pages.LoginPage;
import pages.NavigationMenu;

/** The three-lines menu, its sections, and logging out. Logs in once; Log out runs last. */
public class MenuAndLogoutTest extends SessionTest {

    private DashboardPage home;

    @BeforeClass(alwaysRun = true)
    public void logIn() {
        home = logInAsStaff();
    }

    /** Opens the menu from the home screen and clicks a section; checks its name appears. */
    private void openSection(String section) {
        home.open();
        assertTrue(home.isLoaded(), "Home screen did not open");
        new NavigationMenu(driver).open().clickItem(section);
        assertTrue(home.isTextShownOnPage(section), "'" + section + "' screen did not open");
    }

    @Test(priority = 1, groups = {"auth", "positive"}, description = "Menu shows all sections")
    @TestCase(type = Type.POSITIVE,
            steps = "On the home screen, click the three lines in the top-left corner.",
            expected = "The menu opens with Today, Interactions, Admissions, Master Search, Performance, "
                    + "Leaderboard, Reports, Report Issue and Log out.")
    public void menuShowsAllSections() {
        home.open();
        NavigationMenu menu = new NavigationMenu(driver).open();
        for (String item : NavigationMenu.ITEMS) {
            assertTrue(menu.isItemShown(item), "Menu item missing: " + item);
        }
    }

    @Test(priority = 2, groups = {"auth", "positive"}, description = "Open Interactions")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Interactions.",
            expected = "The Interactions screen opens.")
    public void openInteractions() {
        openSection("Interactions");
    }

    @Test(priority = 3, groups = {"auth", "positive"}, description = "Open Admissions")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Admissions.",
            expected = "The Admissions screen opens.")
    public void openAdmissions() {
        openSection("Admissions");
    }

    @Test(priority = 4, groups = {"auth", "positive"}, description = "Open Master Search")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Master Search.",
            expected = "The Master Search screen opens.")
    public void openMasterSearch() {
        openSection("Master Search");
    }

    @Test(priority = 5, groups = {"auth", "positive"}, description = "Open Performance")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Performance.",
            expected = "The Performance screen opens.")
    public void openPerformance() {
        openSection("Performance");
    }

    @Test(priority = 6, groups = {"auth", "positive"}, description = "Open Leaderboard")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Leaderboard.",
            expected = "The Leaderboard screen opens.")
    public void openLeaderboard() {
        openSection("Leaderboard");
    }

    @Test(priority = 7, groups = {"auth", "positive"}, description = "Open Reports")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Reports.",
            expected = "The Reports screen opens.")
    public void openReports() {
        openSection("Reports");
    }

    @Test(priority = 8, groups = {"auth", "positive"}, description = "Today returns home")
    @TestCase(type = Type.POSITIVE, steps = "From another screen, open the menu and click Today.",
            expected = "The home screen opens with 'Today' and 'Hello, ...'.")
    public void todayReturnsHome() {
        openSection("Interactions");
        home = new NavigationMenu(driver).open().clickToday();
        assertTrue(home.isLoaded(), "Home screen did not open from Today");
        assertTrue(home.isTodayLabelDisplayed(), "'Today' is not shown");
    }

    @Test(priority = 9, groups = {"auth", "positive"}, description = "Log out")
    @TestCase(type = Type.POSITIVE, steps = "Open the menu and click Log out.",
            expected = "The user is logged out and the login screen is shown.")
    public void logOut() {
        home.open();
        LoginPage login = new NavigationMenu(driver).open().clickLogOut();
        assertTrue(login.isEmailFieldDisplayed(), "Login screen not shown after Log out");
    }

    @Test(priority = 10, groups = {"auth", "negative"}, dependsOnMethods = "logOut",
            description = "Staff page after logging out")
    @TestCase(type = Type.NEGATIVE,
            steps = "After logging out, type the staff home address (team.aldun.com/staff) into the browser.",
            expected = "The staff home is not shown; the login screen is shown instead.")
    public void staffPageClosedAfterLogout() {
        home.open();
        assertTrue(loginPage.isEmailFieldDisplayed(), "Staff home opened again after logging out");
    }
}
