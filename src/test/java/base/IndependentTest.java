package base;

import static org.testng.Assert.assertTrue;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import pages.DashboardPage;
import pages.LoginPage;
import pages.NavigationMenu;
import tests.TestData;
import utils.ConfigReader;

/**
 * Base for independent test cases that share ONE browser for the whole run:
 * the browser is launched once before the first test and closed once after the last.
 * Each test still starts from a clean state: after a test, a logged-in user is logged out,
 * and before a test the login page is opened. Tests run one after another, never in parallel.
 */
public abstract class IndependentTest {

    @BeforeSuite(alwaysRun = true)
    public void launchBrowserOnce() {
        DriverFactory.initDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void closeBrowserOnce() {
        DriverFactory.quitDriver();
    }

    /** Every test starts on a fresh login page. */
    @BeforeMethod(alwaysRun = true)
    public void startFromLoginPage() {
        loginPage().openPath(ConfigReader.get("loginPath"));
    }

    /** If the test left someone logged in, log out so the next test starts clean. */
    @AfterMethod(alwaysRun = true)
    public void logOutIfLoggedIn() {
        if (driver() == null || !driver().getCurrentUrl().contains(ConfigReader.get("dashboardPath"))) {
            return;
        }
        DashboardPage home = new DashboardPage(driver());
        home.open(); // leave any open form or prompt first
        if (home.isLoaded()) {
            new NavigationMenu(driver()).open().clickLogOut();
        }
    }

    /** The shared browser. */
    protected WebDriver driver() {
        return DriverFactory.getDriver();
    }

    /** The login page in the shared browser. */
    protected LoginPage loginPage() {
        return new LoginPage(driver());
    }

    /** Logs in with the saved work email and password; fails the test if the home screen does not open. */
    protected DashboardPage logIn() {
        LoginPage login = loginPage();
        login.enterEmail(TestData.resolve(TestData.VALID_EMAIL))
             .enterPassword(TestData.resolve(TestData.VALID_PASSWORD));
        login.clickLogin();
        DashboardPage home = new DashboardPage(driver());
        assertTrue(home.isLoaded(), "Could not log in - home screen did not open");
        return home;
    }
}
