package base;

import static org.testng.Assert.assertTrue;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import pages.DashboardPage;
import pages.LoginPage;
import tests.TestData;

/**
 * Base for test classes that share ONE browser session for all their test cases:
 * the browser opens once before the first test of the class and closes after the last.
 * Use logInAsStaff() once (e.g. in @BeforeClass) when the tests need to be logged in.
 */
public abstract class SessionTest {

    protected WebDriver driver;
    protected LoginPage loginPage;

    @BeforeClass(alwaysRun = true)
    public void openBrowser() {
        driver = DriverFactory.initDriver();
        loginPage = new LoginPage(driver);
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        DriverFactory.quitDriver();
    }

    /** Logs in with the saved work email and password and waits for the home screen. */
    protected DashboardPage logInAsStaff() {
        loginPage.enterEmail(TestData.resolve(TestData.VALID_EMAIL))
                 .enterPassword(TestData.resolve(TestData.VALID_PASSWORD));
        loginPage.clickLogin();
        DashboardPage home = new DashboardPage(driver);
        assertTrue(home.isLoaded(), "Could not log in - home screen did not open");
        return home;
    }

    /** Goes back to a fresh login page. */
    protected void openLoginPage() {
        loginPage.openPath(utils.ConfigReader.get("loginPath"));
    }
}
