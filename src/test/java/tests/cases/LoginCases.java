package tests.cases;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import base.IndependentTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.DashboardPage;
import pages.LoginPage;
import tests.TestData;

/**
 * ALD012-ALD018: real login attempts. Each test has its own browser.
 * Only ONE wrong-password attempt is made for the real account, to avoid locking it.
 */
public class LoginCases extends IndependentTest {

    /** Fills the form, clicks Log in and returns the message the app shows. */
    private String attemptLogin(LoginPage login, String email, String password) {
        login.enterEmail(email).enterPassword(password);
        String before = login.getPageText();
        login.clickLogin();
        return login.waitForNewMessage(before);
    }

    @Test(groups = {"auth", "positive"}, description = "Successful login")
    @TestCase(id = "ALD012", type = Type.POSITIVE,
            steps = "Enter the correct work email and password and click Log in.",
            expected = "User logs in; home shows Today, Hello, Your shift, Check in/Check out and Log Interaction.")
    public void successfulLogin() {
        DashboardPage home = logIn();
        assertTrue(home.getGreeting().startsWith("Hello,"), "Greeting 'Hello, ...' is not shown");
        assertTrue(home.isTodayLabelDisplayed(), "'Today' is not shown");
        assertTrue(home.isShiftSectionDisplayed(), "'Your shift' is not shown");
        assertTrue(home.isAttendanceButtonDisplayed(), "Neither Check in nor Check out is shown");
        assertTrue(home.isLogInteractionButtonDisplayed(), "Log Interaction is not shown");
    }

    @Test(groups = {"auth", "negative"}, description = "Invalid username, valid password")
    @TestCase(id = "ALD013", type = Type.NEGATIVE,
            steps = "Enter an email that does not exist and the correct password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void unknownEmailRejected() {
        LoginPage login = loginPage();
        String message = attemptLogin(login, TestData.UNKNOWN_EMAIL, TestData.resolve(TestData.VALID_PASSWORD));
        assertTrue(login.isOnLoginPage(), "User was logged in with an unknown email");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(groups = {"auth", "negative"}, description = "Valid username, invalid password")
    @TestCase(id = "ALD014", type = Type.NEGATIVE,
            steps = "Enter the correct email and a wrong password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void wrongPasswordRejected() {
        LoginPage login = loginPage();
        String message = attemptLogin(login, TestData.resolve(TestData.VALID_EMAIL), TestData.WRONG_PASSWORD);
        assertTrue(login.isOnLoginPage(), "User was logged in with a wrong password");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(groups = {"auth", "negative"}, description = "Email in wrong format")
    @TestCase(id = "ALD015", type = Type.NEGATIVE,
            steps = "Enter 'abc' (not an email address) and the correct password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void malformedEmailRejected() {
        LoginPage login = loginPage();
        String message = attemptLogin(login, TestData.MALFORMED_EMAIL, TestData.resolve(TestData.VALID_PASSWORD));
        assertTrue(login.isOnLoginPage(), "User was logged in with 'abc' as email");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(groups = {"auth", "negative"}, description = "Email blank, password filled")
    @TestCase(id = "ALD016", type = Type.NEGATIVE,
            steps = "Leave Work email blank, enter the correct password. Click Log in.",
            expected = "Login does not happen and a message asks for the email.")
    public void blankEmailRejected() {
        LoginPage login = loginPage();
        String message = attemptLogin(login, "", TestData.resolve(TestData.VALID_PASSWORD));
        assertTrue(login.isOnLoginPage(), "User was logged in without an email");
        assertFalse(message.isEmpty(), "No message was shown asking for the email");
    }

    @Test(groups = {"auth", "edge"}, description = "Spaces around the email")
    @TestCase(id = "ALD017", type = Type.EDGE,
            steps = "Enter the correct email with spaces before and after it, and the correct password. Click Log in.",
            expected = "The extra spaces are ignored and the user logs in.")
    public void spacesAroundEmailIgnored() {
        LoginPage login = loginPage();
        login.enterEmailAsTyped("  " + TestData.resolve(TestData.VALID_EMAIL) + "  ")
             .enterPassword(TestData.resolve(TestData.VALID_PASSWORD));
        login.clickLogin();
        assertTrue(new DashboardPage(driver()).isLoaded(), "Login failed when the email had spaces around it");
    }

    @Test(groups = {"auth", "edge"}, description = "Log in clicked several times")
    @TestCase(id = "ALD018", type = Type.EDGE,
            steps = "Enter the correct email and password, then click Log in 3 times very quickly.",
            expected = "Only one login happens: the home screen opens normally with no error.")
    public void rapidClicksLogInOnce() {
        LoginPage login = loginPage();
        login.enterEmail(TestData.resolve(TestData.VALID_EMAIL))
             .enterPassword(TestData.resolve(TestData.VALID_PASSWORD));
        login.clickLoginRepeatedly(3);
        DashboardPage home = new DashboardPage(driver());
        assertTrue(home.isLoaded(), "Home screen did not open after clicking Log in several times");
        assertTrue(home.getGreeting().startsWith("Hello,"), "Home screen is not shown correctly");
    }
}
