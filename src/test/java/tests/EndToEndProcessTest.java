package tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.CreateInteractionFormPage;
import pages.DashboardPage;
import pages.InteractionPage;
import pages.InteractionsListPage;
import pages.LoginPage;
import pages.NavigationMenu;
import utils.ScreenshotUtil;

/**
 * The positive flow of the app (from the user's screenshots, 2026-10-13) as 20 test cases in
 * ONE browser, run in ID order:
 * Log in -> Check in -> Interactions -> New interaction -> phone -> Create Interaction ->
 * fill details -> Create (Create anyway) -> Patient profile -> Today -> Interactions ->
 * Check out -> Log out (confirm).
 * A few negative/edge checks sit where they belong and never save anything.
 * If a key step fails, the steps that need it are reported as Skipped.
 * Runs on the LIVE app: checks in, creates one real interaction, checks out.
 */
public class EndToEndProcessTest extends SessionTest {

    private static final Logger LOG = LogManager.getLogger(EndToEndProcessTest.class);

    private DashboardPage home;
    private InteractionPage newInteraction;
    private CreateInteractionFormPage form;
    /** Exact addresses the app opened (they include ?hospitalId=...), reused to reset a screen. */
    private String newInteractionUrl;
    private String formUrl;

    // =============================== LOG IN ===============================

    @Test(groups = {"auth", "positive"}, description = "Login screen is displayed")
    @TestCase(id = "ALD001", type = Type.POSITIVE,
            steps = "Launch the app (team.aldun.com/login).",
            expected = "'Welcome back' login screen shows Work email, Password, Log in, Forgot password and Log in via SSO.")
    public void loginScreenIsDisplayed() {
        assertTrue(loginPage.isEmailFieldDisplayed(), "Work email box is not shown");
        assertTrue(loginPage.isPasswordFieldDisplayed(), "Password box is not shown");
        assertTrue(loginPage.isLoginButtonDisplayed(), "Log in button is not shown");
        assertTrue(loginPage.isForgotPasswordDisplayed(), "Forgot password is not shown");
        assertTrue(loginPage.isSsoButtonDisplayed(), "Log in via SSO is not shown");
    }

    @Test(groups = {"auth", "negative"}, description = "Both fields blank")
    @TestCase(id = "ALD002", type = Type.NEGATIVE,
            steps = "Leave Work email and Password blank and try to click Log in.",
            expected = "Log in button stays disabled (greyed out).")
    public void bothFieldsBlank() {
        loginPage.enterEmail("").enterPassword("");
        assertFalse(loginPage.isLoginButtonEnabled(), "Log in button is active with both fields blank");
    }

    @Test(groups = {"auth", "negative"}, description = "Wrong password")
    @TestCase(id = "ALD003", type = Type.NEGATIVE,
            steps = "Enter the correct work email and a wrong password. Click Log in.",
            expected = "Login fails, the login screen stays, and an error message is shown.")
    public void wrongPasswordRejected() {
        loginPage.enterEmail(TestData.resolve(TestData.VALID_EMAIL)).enterPassword(TestData.WRONG_PASSWORD);
        String before = loginPage.getPageText();
        loginPage.clickLogin();
        String message = loginPage.waitForNewMessage(before);
        assertTrue(loginPage.isOnLoginPage(), "User was logged in with a wrong password");
        assertFalse(message.isEmpty(), "No error message was shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Successful login")
    @TestCase(id = "ALD004", type = Type.POSITIVE,
            steps = "Enter the correct work email (test@aldun.com) and password. Click Log in.",
            expected = "Home screen opens: Today, Hello, Test, Your shift 09:00 - 18:00 and Log Interaction.")
    public void successfulLogin() {
        openLoginPage();
        home = logInAsStaff();
        assertTrue(home.getGreeting().startsWith("Hello,"), "Greeting 'Hello, ...' is not shown");
        assertTrue(home.isShiftSectionDisplayed(), "'Your shift' is not shown");
        assertTrue(home.isLogInteractionButtonDisplayed(), "Log Interaction is not shown");
    }

    // ============================== CHECK IN ==============================

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "successfulLogin", description = "Check in")
    @TestCase(id = "ALD005", type = Type.POSITIVE,
            steps = "On the home screen (status Not checked in), click Check in.",
            expected = "'Checking in...' is shown, then the Interactions list opens.")
    public void checkIn() throws IOException {
        home.open();
        assertTrue(home.isLoaded(), "Home screen did not open");
        if (!home.isNotCheckedIn()) {
            LOG.info("Already checked in - checking out first so Check in can be tested");
            home.clickCheckOut();
            assertTrue(home.waitUntilCheckedOut(), "Could not check out before checking in");
        }
        home.clickCheckIn();
        assertTrue(home.waitForCheckInResponse(), "Check in did not complete");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "e2e_after_check_in"));
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "checkIn", description = "Interactions list")
    @TestCase(id = "ALD006", type = Type.POSITIVE,
            steps = "Look at the Interactions screen that opened after Check in.",
            expected = "It shows the search box (Phone, Adm ID, UHID or Name), 'Created by me' and New interaction.")
    public void interactionsListShown() {
        InteractionsListPage list = new InteractionsListPage(driver);
        assertTrue(list.isLoaded(), "Interactions list is not shown");
        assertTrue(list.isSearchBoxDisplayed(), "Search box is not shown");
    }

    // ========================= CREATE INTERACTION =========================

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "interactionsListShown", description = "New interaction")
    @TestCase(id = "ALD007", type = Type.POSITIVE,
            steps = "Click New interaction at the bottom of the Interactions list.",
            expected = "The New Interaction screen opens with an empty Attender Phone No box.")
    public void openNewInteraction() {
        newInteraction = new InteractionsListPage(driver).clickNewInteraction();
        assertTrue(newInteraction.isLoaded(), "New Interaction screen did not open");
        newInteractionUrl = newInteraction.getCurrentUrl();
        LOG.info("New Interaction address: {}", newInteractionUrl);
    }

    @Test(groups = {"auth", "negative"}, dependsOnMethods = "openNewInteraction", description = "Empty phone number")
    @TestCase(id = "ALD008", type = Type.NEGATIVE,
            steps = "Leave Attender Phone No empty.",
            expected = "No active Create Interaction button, so you cannot continue.")
    public void emptyPhoneCannotContinue() {
        assertTrue(newInteraction.isCreateInteractionDisabled(), "Create Interaction is active with no phone number");
    }

    @Test(groups = {"auth", "negative"}, dependsOnMethods = "openNewInteraction", description = "Phone number too short")
    @TestCase(id = "ALD009", type = Type.NEGATIVE,
            steps = "Enter a 5-digit number (98114) and try Create Interaction.",
            expected = "The short number is not accepted and you cannot continue.")
    public void shortPhoneCannotContinue() {
        driver.get(newInteractionUrl);
        newInteraction.typeAttenderPhoneRaw(TestData.SHORT_PHONE_NUMBER);
        assertFalse(newInteraction.canContinue(), "Continued with a 5-digit phone number");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "openNewInteraction", description = "Number history")
    @TestCase(id = "ALD010", type = Type.POSITIVE,
            steps = "Enter 9811465840 in Attender Phone No.",
            expected = "'Aldun history for this number' appears with Admissions and Interactions counts, "
                    + "and the Create Interaction button is active.")
    public void numberHistoryShown() {
        driver.get(newInteractionUrl);
        newInteraction.enterAttenderPhoneNumber(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(newInteraction.isNumberHistoryShown(), "'Aldun history for this number' is not shown");
        String text = newInteraction.getPageText();
        assertTrue(text.contains("Admissions") && text.contains("Interactions"), "Admissions / Interactions counts not shown");
        assertTrue(newInteraction.isCreateInteractionEnabled(), "Create Interaction button is not active");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "numberHistoryShown", description = "Create Interaction")
    @TestCase(id = "ALD011", type = Type.POSITIVE,
            steps = "Click Create Interaction.",
            expected = "The Create New Interaction form opens with the note "
                    + "'For Onboarding Now, either Admission Id or UHID is required.'")
    public void openCreateForm() {
        String url = newInteraction.getCurrentUrl();
        newInteraction.clickCreateInteraction();
        newInteraction.confirmIfAsked();
        assertTrue(newInteraction.waitUntilSubmitted(url), "Did not move on after Create Interaction");
        form = new CreateInteractionFormPage(driver);
        assertTrue(form.isLoaded(), "'Create New Interaction' form did not open");
        assertTrue(form.isOnboardingHintShown(), "Admission Id / UHID note is not shown");
        formUrl = form.getCurrentUrl();
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "openCreateForm", description = "Phone number carried over")
    @TestCase(id = "ALD012", type = Type.POSITIVE,
            steps = "Look at Attender Phone No on the Create New Interaction form.",
            expected = "It already contains 9811465840 from the previous screen.")
    public void phoneNumberCarriedOver() {
        assertTrue(form.getAttenderPhoneNumber().replaceAll("\\D", "").endsWith(TestData.INTERACTION_CONTACT_NUMBER),
                "Phone number was not carried over. Field shows: " + form.getAttenderPhoneNumber());
    }

    @Test(groups = {"auth", "negative"}, dependsOnMethods = "openCreateForm", description = "Patient Name missing")
    @TestCase(id = "ALD013", type = Type.NEGATIVE,
            steps = "Fill Admission Id and UHID but leave Patient Name empty.",
            expected = "Create stays disabled (greyed out), so nothing can be saved.")
    public void createDisabledWithoutPatientName() {
        form.enterAdmissionId(TestData.ADMISSION_ID).enterUhid(TestData.UHID).enterPatientName("");
        assertFalse(form.isCreateEnabled(), "Create is active even though Patient Name is empty");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "openCreateForm", description = "Fill the details")
    @TestCase(id = "ALD014", type = Type.POSITIVE,
            steps = "Enter Admission Id AD556142, UHID UH558440 and Patient Name Rahul Sharma.",
            expected = "All three values are entered correctly and the Create button becomes active.")
    public void fillDetails() {
        driver.get(formUrl);
        assertTrue(form.isLoaded(), "'Create New Interaction' form did not open");
        form.enterAdmissionId(TestData.ADMISSION_ID).enterUhid(TestData.UHID).enterPatientName(TestData.PATIENT_NAME);
        assertEquals(form.getAdmissionId(), TestData.ADMISSION_ID, "Admission Id is wrong");
        assertEquals(form.getUhid(), TestData.UHID, "UHID is wrong");
        assertEquals(form.getPatientName(), TestData.PATIENT_NAME, "Patient Name is wrong");
        assertTrue(form.isCreateEnabled(), "Create button did not become active");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "fillDetails", description = "Create the interaction")
    @TestCase(id = "ALD015", type = Type.POSITIVE,
            steps = "Click Create. If 'Duplicate interaction found' appears, click Create anyway.",
            expected = "The interaction is saved and the Patient profile opens with 9811465840 and Rahul Sharma.")
    public void createInteraction() throws IOException {
        form.clickCreate();
        form.confirmIfAsked();
        assertTrue(form.waitUntilCreated(), "Interaction was not saved");
        assertTrue(new InteractionsListPage(driver)
                        .isInteractionShown(TestData.INTERACTION_CONTACT_NUMBER, TestData.PATIENT_NAME),
                "Patient profile with the new details is not shown");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "e2e_patient_profile"));
    }

    // =============================== FINISH ===============================

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "checkIn", description = "Click Today")
    @TestCase(id = "ALD016", type = Type.POSITIVE,
            steps = "Click the three lines in the top-left corner, then click Today.",
            expected = "The home screen opens with Hello, Test, status Checked in and a Check out button.")
    public void clickToday() {
        home = new NavigationMenu(driver).open().clickToday();
        assertTrue(home.isLoaded(), "Home screen did not open from Today");
        assertTrue(home.isCheckedIn(), "Status is not 'Checked in'");
        assertTrue(home.isCheckOutButtonDisplayed(), "Check out button is not shown");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "createInteraction", description = "Click Interactions")
    @TestCase(id = "ALD017", type = Type.POSITIVE,
            steps = "Click the three lines, then click Interactions.",
            expected = "The Interactions list opens and shows the new interaction (Adm Id AD556142, Rahul Sharma).")
    public void clickInteractions() {
        new NavigationMenu(driver).open().clickItem("Interactions");
        InteractionsListPage list = new InteractionsListPage(driver);
        assertTrue(list.isLoaded(), "Interactions list did not open");
        assertTrue(list.isInteractionShown(TestData.ADMISSION_ID, TestData.PATIENT_NAME),
                "The new interaction is not in the list");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "clickToday", description = "Check out")
    @TestCase(id = "ALD018", type = Type.POSITIVE,
            steps = "Click the three lines, click Today, then click Check out.",
            expected = "Status changes to Not checked in and the Check in button is shown again.")
    public void checkOut() throws IOException {
        home = new NavigationMenu(driver).open().clickToday();
        assertTrue(home.isLoaded(), "Home screen did not open");
        assertTrue(home.isCheckOutButtonDisplayed(), "Check out button is not shown");
        home.clickCheckOut();
        assertTrue(home.waitUntilCheckedOut(), "Status did not change to 'Not checked in'");
        assertTrue(home.isCheckInButtonDisplayed(), "Check in button is not shown again");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "e2e_checked_out"));
    }

    @Test(groups = {"auth", "edge"}, dependsOnMethods = "successfulLogin", description = "Cancel log out")
    @TestCase(id = "ALD019", type = Type.EDGE,
            steps = "Click the three lines, click Log out, then click Cancel in the 'Log out?' box.",
            expected = "The user stays logged in on the home screen.")
    public void cancelLogOut() {
        home.open();
        NavigationMenu menu = new NavigationMenu(driver).open();
        menu.clickLogOutThenCancel();
        assertFalse(menu.isLogOutDialogShown(), "'Log out?' box did not close after Cancel");
        home.open();
        assertTrue(home.isLoaded(), "User was logged out even though Cancel was clicked");
    }

    @Test(groups = {"auth", "positive"}, dependsOnMethods = "successfulLogin", description = "Log out")
    @TestCase(id = "ALD020", type = Type.POSITIVE,
            steps = "Click the three lines, click Log out, then click Log out in the 'Log out?' box. "
                    + "Then try to open the staff home address again.",
            expected = "The login screen is shown, and the staff home cannot be opened without logging in again.")
    public void logOut() {
        home.open();
        LoginPage login = new NavigationMenu(driver).open().clickLogOut();
        assertTrue(login.isEmailFieldDisplayed(), "Login screen not shown after Log out");
        home.open();
        assertTrue(login.isEmailFieldDisplayed(), "Staff home opened again after logging out");
    }
}
