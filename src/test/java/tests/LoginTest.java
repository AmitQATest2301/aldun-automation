package tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.DashboardPage;

/**
 * Login module tests.
 * Group "ui"   - never submits the form; safe to run any time.
 * Group "auth" - submits real login attempts; needs ALDUN_EMAIL / ALDUN_PASSWORD.
 * Each test is also tagged "positive", "negative" or "edge" (see testng-positive.xml).
 */
public class LoginTest extends BaseTest {

    private static final Logger LOG = LogManager.getLogger(LoginTest.class);

    // ===================== UI (no submit) =====================

    @Test(groups = {"ui", "positive"}, description = "Email, password, Log in, Forgot password and SSO all displayed")
    public void verifyLoginPageElementsDisplayed() {
        assertTrue(loginPage.isEmailFieldDisplayed(), "Email field not displayed");
        assertTrue(loginPage.isPasswordFieldDisplayed(), "Password field not displayed");
        assertTrue(loginPage.isLoginButtonDisplayed(), "Log in button not displayed");
        assertTrue(loginPage.isForgotPasswordDisplayed(), "Forgot password not displayed");
        assertTrue(loginPage.isSsoButtonDisplayed(), "Log in via SSO not displayed");
    }

    @Test(groups = {"ui", "positive"}, description = "Password hidden by default")
    public void verifyPasswordMaskedByDefault() {
        assertEquals(loginPage.getPasswordFieldType(), "password");
    }

    @Test(groups = {"ui", "negative"}, dataProvider = "loginButtonDisabled", dataProviderClass = TestData.class,
            description = "Log in is disabled when required fields are blank")
    public void verifyLoginButtonDisabled(String scenario, String email, String password) {
        loginPage.enterEmail(email).enterPassword(password);
        assertFalse(loginPage.isLoginButtonEnabled(), scenario + ": Log in should be disabled");
    }

    @Test(groups = {"ui", "positive"}, description = "Log in enabled when both fields are filled")
    public void verifyLoginButtonEnabledWhenBothFilled() {
        loginPage.enterEmail("qa.user@example.com").enterPassword("Sample@123");
        assertTrue(loginPage.isLoginButtonEnabled(), "Log in should be enabled");
    }

    /**
     * Observed 2026-10-13: with email blank and password filled, Log in is ENABLED
     * (unlike password-blank, where it is disabled). This test locks in current behaviour;
     * if the team confirms it is a bug, flip the assertion.
     */
    @Test(groups = {"ui", "edge"}, description = "Email blank, password filled: Log in enabled (possible app bug)")
    public void verifyLoginButtonStateWhenOnlyPasswordFilled() {
        loginPage.enterEmail("").enterPassword("Sample@123");
        assertTrue(loginPage.isLoginButtonEnabled(),
                "Behaviour changed: Log in is now disabled when email is blank - update this test");
    }

    @Test(groups = {"ui", "edge"}, description = "Malformed email shows no inline error")
    public void verifyMalformedEmailHasNoInlineValidation() {
        loginPage.enterEmail("abc").enterPassword("Sample@123");
        assertTrue(loginPage.isLoginButtonEnabled(), "Log in should be enabled");
        assertEquals(loginPage.getCurrentToastText(), "", "Unexpected message shown before submit");
    }

    @Test(groups = {"ui", "positive"}, description = "Show/Hide password works and keeps the typed value")
    public void verifyPasswordVisibilityToggle() {
        String value = "Sample@123";
        loginPage.enterPassword(value);

        loginPage.showPassword();
        assertEquals(loginPage.getPasswordFieldType(), "text", "Password should be visible after Show");
        assertEquals(loginPage.getPasswordFieldValue(), value, "Value changed after Show");

        loginPage.hidePassword();
        assertEquals(loginPage.getPasswordFieldType(), "password", "Password should be masked after Hide");
        assertEquals(loginPage.getPasswordFieldValue(), value, "Value changed after Hide");
    }

    @Test(groups = {"ui", "positive"}, description = "254-character email accepted")
    public void verifyLongEmailAccepted() {
        String local = "a".repeat(64);
        String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(57) + ".com";
        String email = local + "@" + domain;
        loginPage.enterEmail(email).enterPassword("Sample@123");
        assertTrue(loginPage.isLoginButtonEnabled(), "Log in should be enabled for a 254-char email");
    }

    // ===================== AUTH (submits the form) =====================

    @Test(groups = {"auth", "positive"}, priority = 1,
            description = "Valid login opens the staff home screen")
    public void verifyValidLogin() {
        String email = TestData.resolve(TestData.VALID_EMAIL);
        String password = TestData.resolve(TestData.VALID_PASSWORD);

        // Steps follow the user's screenshot: email typed, password typed and made visible, then Log in
        loginPage.enterEmail(email).enterPassword(password);
        loginPage.showPassword();
        assertEquals(loginPage.getEmailFieldValue(), email, "Email not entered correctly");
        assertEquals(loginPage.getPasswordFieldType(), "text", "Password should be visible before Log in");
        assertEquals(loginPage.getPasswordFieldValue(), password, "Password not entered correctly");
        assertTrue(loginPage.isLoginButtonEnabled(), "Log in should be enabled");
        loginPage.clickLogin();

        DashboardPage dashboard = new DashboardPage(driver);
        assertTrue(dashboard.isLoaded(), "Staff home screen not shown after valid login. URL: "
                + dashboard.getCurrentUrl());
        assertTrue(dashboard.getGreeting().startsWith("Hello,"),
                "Unexpected greeting: " + dashboard.getGreeting());
        assertTrue(dashboard.isTodayLabelDisplayed(), "'Today' label not displayed");
        assertTrue(dashboard.isShiftSectionDisplayed(), "'Your shift' section not displayed");
        assertTrue(dashboard.isAttendanceButtonDisplayed(), "Neither 'Check in' nor 'Check out' button displayed");
        assertTrue(dashboard.isLogInteractionButtonDisplayed(), "'Log Interaction' button not displayed");
    }

    @Test(groups = {"auth", "negative"}, priority = 2, dataProvider = "invalidLogin", dataProviderClass = TestData.class,
            description = "Invalid credentials are rejected")
    public void verifyInvalidLoginRejected(String scenario, String email, String password) {
        loginPage.login(TestData.resolve(email), TestData.resolve(password));

        String message = loginPage.waitForErrorMessage();
        LOG.info("[{}] message shown: '{}'", scenario, message);

        assertTrue(loginPage.isOnLoginPage(), scenario + ": should stay on login page, URL is "
                + loginPage.getCurrentUrl());
        assertFalse(message.isEmpty(), scenario + ": no error message was shown");
    }
}
