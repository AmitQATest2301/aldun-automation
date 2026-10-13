package tests;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.DashboardPage;
import pages.NavigationMenu;

/**
 * Real login attempts against the live app. Only ONE wrong-password attempt is made for the
 * real account, to avoid locking it. Tests that log in successfully log out again afterwards.
 */
public class LoginAttemptTest extends SessionTest {

    private static final Logger LOG = LogManager.getLogger(LoginAttemptTest.class);

    @BeforeMethod(alwaysRun = true)
    public void freshLoginPage() {
        openLoginPage();
    }

    @AfterMethod(alwaysRun = true)
    public void logOutIfLoggedIn() {
        if (driver.getCurrentUrl().contains(utils.ConfigReader.get("dashboardPath"))) {
            new NavigationMenu(driver).open().clickLogOut();
        }
    }

    /** Fills the form, clicks Log in and returns the message the app shows. */
    private String attemptLogin(String email, String password) {
        loginPage.enterEmail(email).enterPassword(password);
        String before = loginPage.getPageText();
        loginPage.clickLogin();
        String message = loginPage.waitForNewMessage(before);
        LOG.info("Message shown: '{}'", message);
        return message;
    }

    @Test(priority = 1, groups = {"auth", "negative"}, description = "Invalid username, valid password")
    @TestCase(type = Type.NEGATIVE,
            steps = "Enter an email that does not exist and the correct password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void unknownEmailIsRejected() {
        String message = attemptLogin(TestData.UNKNOWN_EMAIL, TestData.resolve(TestData.VALID_PASSWORD));
        assertTrue(loginPage.isOnLoginPage(), "User was logged in with an unknown email");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(priority = 2, groups = {"auth", "negative"}, description = "Valid username, invalid password")
    @TestCase(type = Type.NEGATIVE,
            steps = "Enter the correct email and a wrong password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void wrongPasswordIsRejected() {
        String message = attemptLogin(TestData.resolve(TestData.VALID_EMAIL), TestData.WRONG_PASSWORD);
        assertTrue(loginPage.isOnLoginPage(), "User was logged in with a wrong password");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(priority = 3, groups = {"auth", "negative"}, description = "Email in wrong format")
    @TestCase(type = Type.NEGATIVE,
            steps = "Enter 'abc' (not an email address) and the correct password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void malformedEmailIsRejected() {
        String message = attemptLogin(TestData.MALFORMED_EMAIL, TestData.resolve(TestData.VALID_PASSWORD));
        assertTrue(loginPage.isOnLoginPage(), "User was logged in with 'abc' as email");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(priority = 4, groups = {"auth", "negative"}, description = "Email blank, password filled")
    @TestCase(type = Type.NEGATIVE,
            steps = "Leave Work email blank, enter the correct password. Click Log in.",
            expected = "Login does not happen and a message asks for the email.")
    public void blankEmailIsRejected() {
        String message = attemptLogin("", TestData.resolve(TestData.VALID_PASSWORD));
        assertTrue(loginPage.isOnLoginPage(), "User was logged in without an email");
        assertFalse(message.isEmpty(), "No message was shown asking for the email");
    }

    @Test(priority = 5, groups = {"auth", "edge"}, description = "Spaces around the email")
    @TestCase(type = Type.EDGE,
            steps = "Enter the correct email with spaces before and after it, and the correct password. Click Log in.",
            expected = "The extra spaces are ignored and the user logs in.")
    public void spacesAroundEmailAreIgnored() {
        loginPage.enterEmailAsTyped("  " + TestData.resolve(TestData.VALID_EMAIL) + "  ")
                 .enterPassword(TestData.resolve(TestData.VALID_PASSWORD));
        loginPage.clickLogin();
        assertTrue(new DashboardPage(driver).isLoaded(), "Login failed when the email had spaces around it");
    }

    @Test(priority = 6, groups = {"auth", "edge"}, description = "Log in clicked several times")
    @TestCase(type = Type.EDGE,
            steps = "Enter the correct email and password, then click Log in 3 times very quickly.",
            expected = "Only one login happens: the home screen opens normally with no error.")
    public void rapidLoginClicksLogInOnce() {
        loginPage.enterEmail(TestData.resolve(TestData.VALID_EMAIL))
                 .enterPassword(TestData.resolve(TestData.VALID_PASSWORD));
        loginPage.clickLoginRepeatedly(3);
        DashboardPage home = new DashboardPage(driver);
        assertTrue(home.isLoaded(), "Home screen did not open after clicking Log in several times");
        assertTrue(home.getGreeting().startsWith("Hello,"), "Home screen is not shown correctly");
    }
}
