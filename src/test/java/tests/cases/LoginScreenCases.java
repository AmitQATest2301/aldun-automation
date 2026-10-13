package tests.cases;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import base.IndependentTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.InteractionPage;
import pages.LoginPage;
import tests.TestData;

/** ALD001-ALD011: login screen and protected pages. No login needed. Each test has its own browser. */
public class LoginScreenCases extends IndependentTest {

    @Test(groups = {"ui", "positive"}, description = "Login screen is displayed")
    @TestCase(id = "ALD001", type = Type.POSITIVE,
            steps = "Launch the app and look at the login screen.",
            expected = "Login screen shows Work email, Password, Log in, Forgot password and Log in via SSO.")
    public void loginScreenIsDisplayed() {
        LoginPage login = loginPage();
        assertTrue(login.isEmailFieldDisplayed(), "Work email box is not shown");
        assertTrue(login.isPasswordFieldDisplayed(), "Password box is not shown");
        assertTrue(login.isLoginButtonDisplayed(), "Log in button is not shown");
        assertTrue(login.isForgotPasswordDisplayed(), "Forgot password is not shown");
        assertTrue(login.isSsoButtonDisplayed(), "Log in via SSO is not shown");
    }

    @Test(groups = {"ui", "positive"}, description = "Password is hidden while typing")
    @TestCase(id = "ALD002", type = Type.POSITIVE,
            steps = "Type a password into the Password box.",
            expected = "The password is hidden (shown as dots).")
    public void passwordIsHidden() {
        LoginPage login = loginPage();
        login.enterPassword(TestData.SAMPLE_PASSWORD);
        assertEquals(login.getPasswordFieldType(), "password", "Password is visible");
    }

    @Test(groups = {"ui", "positive"}, description = "Show / Hide password")
    @TestCase(id = "ALD003", type = Type.POSITIVE,
            steps = "Type a password, click the eye icon to show it, then click it again to hide it.",
            expected = "The password becomes readable, then hidden again, and the text does not change.")
    public void showAndHidePassword() {
        LoginPage login = loginPage();
        login.enterPassword(TestData.SAMPLE_PASSWORD);
        login.showPassword();
        assertEquals(login.getPasswordFieldType(), "text", "Password is not shown after clicking the eye");
        login.hidePassword();
        assertEquals(login.getPasswordFieldType(), "password", "Password is not hidden again");
        assertEquals(login.getPasswordFieldValue(), TestData.SAMPLE_PASSWORD, "Password text changed");
    }

    @Test(groups = {"ui", "positive"}, description = "Log in becomes active")
    @TestCase(id = "ALD004", type = Type.POSITIVE,
            steps = "Enter an email and a password (do not click Log in).",
            expected = "The Log in button becomes active.")
    public void loginButtonBecomesActive() {
        LoginPage login = loginPage();
        login.enterEmail(TestData.SAMPLE_EMAIL).enterPassword(TestData.SAMPLE_PASSWORD);
        assertTrue(login.isLoginButtonEnabled(), "Log in button is not active");
    }

    @Test(groups = {"ui", "negative"}, description = "Both fields blank")
    @TestCase(id = "ALD005", type = Type.NEGATIVE,
            steps = "Leave both Work email and Password blank and try to click Log in.",
            expected = "Log in button stays disabled, so the form cannot be submitted.")
    public void bothFieldsBlank() {
        assertFalse(loginPage().isLoginButtonEnabled(), "Log in button is active with both fields blank");
    }

    @Test(groups = {"ui", "negative"}, description = "Password blank")
    @TestCase(id = "ALD006", type = Type.NEGATIVE,
            steps = "Enter an email, leave Password blank and try to click Log in.",
            expected = "Log in button stays disabled.")
    public void passwordBlank() {
        LoginPage login = loginPage();
        login.enterEmail(TestData.SAMPLE_EMAIL);
        assertFalse(login.isLoginButtonEnabled(), "Log in button is active with Password blank");
    }

    @Test(groups = {"ui", "negative"}, description = "Email blank")
    @TestCase(id = "ALD007", type = Type.NEGATIVE,
            steps = "Leave Work email blank, enter a password and try to click Log in.",
            expected = "Log in button stays disabled until an email is entered.")
    public void emailBlank() {
        // Observed 2026-10-13: the button is ENABLED here - reported as a defect until fixed.
        LoginPage login = loginPage();
        login.enterPassword(TestData.SAMPLE_PASSWORD);
        assertFalse(login.isLoginButtonEnabled(), "DEFECT: Log in button is active even though Work email is blank");
    }

    @Test(groups = {"ui", "edge"}, description = "Special characters in password")
    @TestCase(id = "ALD008", type = Type.EDGE,
            steps = "Type a password with special characters (P@ss#W0rd!$%^&*()_+) and click the eye icon.",
            expected = "Every character is kept exactly as typed.")
    public void specialCharactersKept() {
        LoginPage login = loginPage();
        login.enterPassword(TestData.SPECIAL_CHARACTER_PASSWORD);
        login.showPassword();
        assertEquals(login.getPasswordFieldValue(), TestData.SPECIAL_CHARACTER_PASSWORD, "Characters were changed");
    }

    @Test(groups = {"ui", "edge"}, description = "Longest allowed email")
    @TestCase(id = "ALD009", type = Type.EDGE,
            steps = "Enter a 254-character email (the longest an email can be) and a password.",
            expected = "The full email is accepted and Log in becomes active.")
    public void longestEmailAccepted() {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(57) + ".com";
        LoginPage login = loginPage();
        login.enterEmail(email).enterPassword(TestData.SAMPLE_PASSWORD);
        assertEquals(login.getEmailFieldValue(), email, "Email was cut short");
        assertTrue(login.isLoginButtonEnabled(), "Log in button is not active for a 254-character email");
    }

    @Test(groups = {"ui", "negative"}, description = "Home page without login")
    @TestCase(id = "ALD010", type = Type.NEGATIVE,
            steps = "Without logging in, type the staff home address (team.aldun.com/staff) into the browser.",
            expected = "The staff home is not shown; the login screen is shown instead.")
    public void staffHomeNeedsLogin() {
        LoginPage login = loginPage();
        login.openPath(utils.ConfigReader.get("dashboardPath"));
        assertTrue(login.isEmailFieldDisplayed(), "Staff home opened without logging in");
    }

    @Test(groups = {"ui", "negative"}, description = "New Interaction page without login")
    @TestCase(id = "ALD011", type = Type.NEGATIVE,
            steps = "Without logging in, type the New Interaction address into the browser.",
            expected = "The New Interaction screen is not shown; the login screen is shown instead.")
    public void newInteractionNeedsLogin() {
        new InteractionPage(driver()).open();
        assertTrue(loginPage().isEmailFieldDisplayed(), "New Interaction screen opened without logging in");
    }
}
