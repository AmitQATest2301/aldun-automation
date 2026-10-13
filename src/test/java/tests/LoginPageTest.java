package tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;

/**
 * Login screen checks that never submit the form, so they need no account.
 * One browser for the whole class; the login page is reloaded before each test.
 */
public class LoginPageTest extends SessionTest {

    @BeforeMethod(alwaysRun = true)
    public void freshLoginPage() {
        openLoginPage();
    }

    @Test(priority = 1, groups = {"ui", "positive"}, description = "Login screen is displayed")
    @TestCase(type = Type.POSITIVE,
            steps = "Launch the app and look at the login screen.",
            expected = "Login screen shows Work email, Password, Log in, Forgot password and Log in via SSO.")
    public void loginScreenIsDisplayed() {
        assertTrue(loginPage.isEmailFieldDisplayed(), "Work email box is not shown");
        assertTrue(loginPage.isPasswordFieldDisplayed(), "Password box is not shown");
        assertTrue(loginPage.isLoginButtonDisplayed(), "Log in button is not shown");
        assertTrue(loginPage.isForgotPasswordDisplayed(), "Forgot password is not shown");
        assertTrue(loginPage.isSsoButtonDisplayed(), "Log in via SSO is not shown");
    }

    @Test(priority = 2, groups = {"ui", "positive"}, description = "Password is hidden while typing")
    @TestCase(type = Type.POSITIVE,
            steps = "Type a password into the Password box.",
            expected = "The password is hidden (shown as dots).")
    public void passwordIsHiddenByDefault() {
        loginPage.enterPassword(TestData.SAMPLE_PASSWORD);
        assertEquals(loginPage.getPasswordFieldType(), "password", "Password is visible");
    }

    @Test(priority = 3, groups = {"ui", "positive"}, description = "Show / Hide password")
    @TestCase(type = Type.POSITIVE,
            steps = "Type a password, click the eye icon to show it, then click it again to hide it.",
            expected = "The password becomes readable, then hidden again, and the typed text does not change.")
    public void showAndHidePassword() {
        loginPage.enterPassword(TestData.SAMPLE_PASSWORD);
        loginPage.showPassword();
        assertEquals(loginPage.getPasswordFieldType(), "text", "Password is not shown after clicking the eye");
        assertEquals(loginPage.getPasswordFieldValue(), TestData.SAMPLE_PASSWORD, "Password text changed");
        loginPage.hidePassword();
        assertEquals(loginPage.getPasswordFieldType(), "password", "Password is not hidden again");
        assertEquals(loginPage.getPasswordFieldValue(), TestData.SAMPLE_PASSWORD, "Password text changed");
    }

    @Test(priority = 4, groups = {"ui", "positive"}, description = "Log in becomes active")
    @TestCase(type = Type.POSITIVE,
            steps = "Enter an email and a password (do not click Log in).",
            expected = "The Log in button becomes active.")
    public void loginButtonActiveWhenBothFilled() {
        loginPage.enterEmail(TestData.SAMPLE_EMAIL).enterPassword(TestData.SAMPLE_PASSWORD);
        assertTrue(loginPage.isLoginButtonEnabled(), "Log in button is not active");
    }

    @Test(priority = 5, groups = {"ui", "negative"}, description = "Both fields blank")
    @TestCase(type = Type.NEGATIVE,
            steps = "Leave both Work email and Password blank and try to click Log in.",
            expected = "Log in button stays disabled, so the form cannot be submitted.")
    public void bothFieldsBlankBlocksLogin() {
        assertFalse(loginPage.isLoginButtonEnabled(), "Log in button is active with both fields blank");
    }

    @Test(priority = 6, groups = {"ui", "negative"}, description = "Password blank")
    @TestCase(type = Type.NEGATIVE,
            steps = "Enter an email, leave Password blank and try to click Log in.",
            expected = "Log in button stays disabled.")
    public void passwordBlankBlocksLogin() {
        loginPage.enterEmail(TestData.SAMPLE_EMAIL);
        assertFalse(loginPage.isLoginButtonEnabled(), "Log in button is active with Password blank");
    }

    @Test(priority = 7, groups = {"ui", "negative"}, description = "Email blank")
    @TestCase(type = Type.NEGATIVE,
            steps = "Leave Work email blank, enter a password and try to click Log in.",
            expected = "Log in button stays disabled until an email is entered.")
    public void emailBlankBlocksLogin() {
        // Observed 2026-10-13: the button is ENABLED in this case (unlike Password blank).
        // This test states the expected rule, so it reports that as a defect until fixed.
        loginPage.enterPassword(TestData.SAMPLE_PASSWORD);
        assertFalse(loginPage.isLoginButtonEnabled(),
                "DEFECT: Log in button is active even though Work email is blank");
    }

    @Test(priority = 8, groups = {"ui", "edge"}, description = "Special characters in password")
    @TestCase(type = Type.EDGE,
            steps = "Type a password with special characters (P@ss#W0rd!$%^&*()_+) and click the eye icon.",
            expected = "Every character is kept exactly as typed.")
    public void specialCharacterPasswordKept() {
        loginPage.enterPassword(TestData.SPECIAL_CHARACTER_PASSWORD);
        loginPage.showPassword();
        assertEquals(loginPage.getPasswordFieldValue(), TestData.SPECIAL_CHARACTER_PASSWORD,
                "Special characters were changed or dropped");
    }

    @Test(priority = 9, groups = {"ui", "edge"}, description = "Longest allowed email")
    @TestCase(type = Type.EDGE,
            steps = "Enter a 254-character email (the longest an email can be) and a password.",
            expected = "The full email is accepted and Log in becomes active.")
    public void longestEmailAccepted() {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(57) + ".com";
        loginPage.enterEmail(email).enterPassword(TestData.SAMPLE_PASSWORD);
        assertEquals(loginPage.getEmailFieldValue(), email, "Email was cut short");
        assertTrue(loginPage.isLoginButtonEnabled(), "Log in button is not active for a 254-character email");
    }
}
