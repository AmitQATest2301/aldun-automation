package tests;

import org.testng.SkipException;

import utils.ConfigReader;

/**
 * All test data in one place. The tokens VALID_EMAIL / VALID_PASSWORD are replaced at
 * runtime from config.local.properties (or ALDUN_EMAIL / ALDUN_PASSWORD environment
 * variables), so no secret lives in code.
 */
public final class TestData {

    // ---------- login ----------
    public static final String VALID_EMAIL = "${VALID_EMAIL}";
    public static final String VALID_PASSWORD = "${VALID_PASSWORD}";

    public static final String UNKNOWN_EMAIL = "qa.unknown.user@aldun.com";
    public static final String WRONG_PASSWORD = "Wrong@Pass123";
    public static final String MALFORMED_EMAIL = "abc";
    /** Used only on the login page (never submitted) to check special characters are kept. */
    public static final String SPECIAL_CHARACTER_PASSWORD = "P@ss#W0rd!$%^&*()_+";
    public static final String SAMPLE_EMAIL = "qa.user@example.com";
    public static final String SAMPLE_PASSWORD = "Sample@123";

    // ---------- interaction ----------
    /** Contact number entered on the New Interaction screen. */
    public static final String INTERACTION_CONTACT_NUMBER = "9811465840";
    public static final String SHORT_PHONE_NUMBER = "98114";
    public static final String LETTERS_PHONE_NUMBER = "abcdefghij";
    public static final String ELEVEN_DIGIT_PHONE_NUMBER = "98114658401";

    /** Values for the "Create New Interaction" form. */
    public static final String ADMISSION_ID = "AD556142";
    public static final String UHID = "UH558440";
    public static final String PATIENT_NAME = "Rahul Sharma";

    /** A search that should match nothing. */
    public static final String NO_MATCH_SEARCH = "zz-no-such-patient-000";

    private TestData() {
    }

    /** Swaps placeholder tokens for real values; skips the test when the value is not configured. */
    public static String resolve(String value) {
        return switch (value) {
            case VALID_EMAIL -> require("ALDUN_EMAIL");
            case VALID_PASSWORD -> require("ALDUN_PASSWORD");
            default -> value;
        };
    }

    private static String require(String envName) {
        String secret = ConfigReader.getSecret(envName);
        if (secret == null) {
            throw new SkipException(envName + " is not set (config.local.properties or environment variable)");
        }
        return secret;
    }
}
