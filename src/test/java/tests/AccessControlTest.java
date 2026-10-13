package tests;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;

/** Staff pages must not open for someone who is not logged in. Needs no account. */
public class AccessControlTest extends SessionTest {

    @Test(priority = 1, groups = {"ui", "negative"}, description = "Home page without login")
    @TestCase(type = Type.NEGATIVE,
            steps = "Without logging in, type the staff home address (team.aldun.com/staff) into the browser.",
            expected = "The staff home is not shown; the login screen is shown instead.")
    public void staffHomeNeedsLogin() {
        loginPage.openPath(utils.ConfigReader.get("dashboardPath"));
        assertTrue(loginPage.isEmailFieldDisplayed(), "Staff home opened without logging in");
    }

    @Test(priority = 2, groups = {"ui", "negative"}, description = "New Interaction page without login")
    @TestCase(type = Type.NEGATIVE,
            steps = "Without logging in, type the New Interaction address into the browser.",
            expected = "The New Interaction screen is not shown; the login screen is shown instead.")
    public void newInteractionNeedsLogin() {
        new pages.InteractionPage(driver).open();
        assertTrue(loginPage.isEmailFieldDisplayed(), "New Interaction screen opened without logging in");
    }
}
