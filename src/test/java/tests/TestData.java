package tests;

import org.testng.SkipException;
import org.testng.annotations.DataProvider;

import utils.ConfigReader;

/**
 * DataProviders for login tests. Row format: { scenario, email, password }.
 * The tokens VALID_EMAIL / VALID_PASSWORD are replaced at runtime from the
 * ALDUN_EMAIL / ALDUN_PASSWORD environment variables, so no secret lives in code.
 */
public final class TestData {

    public static final String VALID_EMAIL = "${VALID_EMAIL}";
    public static final String VALID_EMAIL_UPPERCASE = "${VALID_EMAIL_UPPERCASE}";
    public static final String VALID_PASSWORD = "${VALID_PASSWORD}";

    /** Contact number entered on the Log Interaction screen. */
    public static final String INTERACTION_CONTACT_NUMBER = "9811465840";

    /** Values for the "Create New Interaction" form. */
    public static final String ADMISSION_ID = "AD556142";
    public static final String UHID = "UH558440";
    public static final String PATIENT_NAME = "Rahul Sharma";

    private static final String UNKNOWN_EMAIL = "qa.unknown.user@aldun.com";
    private static final String WRONG_PASSWORD = "Wrong@Pass123";

    private TestData() {
    }

    /** Verified 2026-10-13: Log in is disabled for these combinations. */
    @DataProvider(name = "loginButtonDisabled")
    public static Object[][] loginButtonDisabled() {
        return new Object[][] {
                { "Log in disabled when both fields are blank", "", "" },
                { "Log in disabled when the password is blank", "qa.user@example.com", "" },
        };
    }

    /** Submitted against the live app; each should be rejected and stay on the login page. */
    @DataProvider(name = "invalidLogin")
    public static Object[][] invalidLogin() {
        return new Object[][] {
                { "Login rejected: unknown email + valid password", UNKNOWN_EMAIL, VALID_PASSWORD },
                { "Login rejected: valid email + wrong password", VALID_EMAIL, WRONG_PASSWORD },
                { "Login rejected: unknown email + wrong password", UNKNOWN_EMAIL, WRONG_PASSWORD },
                { "Login rejected: blank email + valid password", "", VALID_PASSWORD },
                { "Login rejected: malformed email + valid password", "abc", VALID_PASSWORD },
                { "Login rejected: UPPERCASE email + wrong password", VALID_EMAIL_UPPERCASE, WRONG_PASSWORD },
        };
    }

    /** Swaps placeholder tokens for real values; skips the test when the env var is not set. */
    public static String resolve(String value) {
        return switch (value) {
            case VALID_EMAIL -> require("ALDUN_EMAIL");
            case VALID_EMAIL_UPPERCASE -> require("ALDUN_EMAIL").toUpperCase();
            case VALID_PASSWORD -> require("ALDUN_PASSWORD");
            default -> value;
        };
    }

    private static String require(String envName) {
        String secret = ConfigReader.getSecret(envName);
        if (secret == null) {
            throw new SkipException("Environment variable " + envName + " is not set");
        }
        return secret;
    }
}
