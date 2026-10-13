package tests;

import static org.testng.Assert.assertEquals;
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
import pages.NavigationMenu;
import utils.ScreenshotUtil;

/**
 * Create Interaction flow, one step per test case in ONE browser session:
 * log out if already logged in, log in, create an interaction, then go to Today.
 * If a step fails, the steps after it are reported as Skipped.
 * Runs on the LIVE app and creates a real interaction.
 */
public class CreateInteractionFlowTest extends SessionTest {

    private static final Logger LOG = LogManager.getLogger(CreateInteractionFlowTest.class);

    private DashboardPage home;
    private InteractionPage newInteraction;
    private CreateInteractionFormPage patientForm;

    @Test(priority = 1, groups = {"auth", "positive"}, description = "Start logged out")
    @TestCase(type = Type.POSITIVE,
            steps = "Open the staff home. If someone is already logged in, open the menu and click Log out.",
            expected = "The login screen is shown, ready for a fresh login.")
    public void logOutIfAlreadyLoggedIn() {
        home = new DashboardPage(driver);
        home.open();
        if (home.isLoaded()) {
            LOG.info("Session was already logged in - logging out first");
            new NavigationMenu(driver).open().clickLogOut();
        } else {
            LOG.info("Not logged in - nothing to log out");
            openLoginPage();
        }
        assertTrue(loginPage.isEmailFieldDisplayed(), "Login screen is not shown");
    }

    @Test(priority = 2, groups = {"auth", "positive"}, dependsOnMethods = "logOutIfAlreadyLoggedIn",
            description = "Successful login")
    @TestCase(type = Type.POSITIVE,
            steps = "Enter the correct work email and password and click Log in.",
            expected = "User logs in and the home screen shows Hello, Test.")
    public void logIn() {
        home = logInAsStaff();
        assertTrue(home.getGreeting().startsWith("Hello,"), "Greeting 'Hello, ...' is not shown");
    }

    @Test(priority = 3, groups = {"auth", "positive"}, dependsOnMethods = "logIn",
            description = "Open Log Interaction")
    @TestCase(type = Type.POSITIVE,
            steps = "On the home screen, click Log Interaction.",
            expected = "The New Interaction screen opens with Attender Phone No.")
    public void openLogInteraction() {
        String homeUrl = home.getCurrentUrl();
        home.clickLogInteraction();
        assertTrue(home.waitUntilLeftHomeScreen(homeUrl), "Nothing happened after clicking Log Interaction");
        newInteraction = new InteractionPage(driver);
        assertTrue(newInteraction.isLoaded(), "New Interaction screen did not open");
    }

    @Test(priority = 4, groups = {"auth", "positive"}, dependsOnMethods = "openLogInteraction",
            description = "Enter phone number and continue")
    @TestCase(type = Type.POSITIVE,
            steps = "Enter 9811465840 in Attender Phone No and click Create Interaction (Create anyway if asked).",
            expected = "The number history is shown and the Create New Interaction form opens.")
    public void enterPhoneNumber() throws IOException {
        newInteraction.enterAttenderPhoneNumber(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(newInteraction.isNumberHistoryShown(), "'Aldun history for this number' is not shown");
        assertTrue(newInteraction.isCreateInteractionEnabled(), "Create Interaction button is not active");
        String formUrl = newInteraction.getCurrentUrl();
        newInteraction.clickCreateInteraction();
        newInteraction.confirmIfAsked();
        assertTrue(newInteraction.waitUntilSubmitted(formUrl), "Did not move on after Create Interaction");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "flow_phone_entered"));
    }

    @Test(priority = 5, groups = {"auth", "positive"}, dependsOnMethods = "enterPhoneNumber",
            description = "Fill patient details")
    @TestCase(type = Type.POSITIVE,
            steps = "Enter Admission Id AD556142, UHID UH558440 and Patient Name Rahul Sharma.",
            expected = "All three values are entered correctly.")
    public void fillPatientDetails() {
        patientForm = new CreateInteractionFormPage(driver);
        assertTrue(patientForm.isLoaded(), "'Create New Interaction' form is not shown");
        patientForm.enterAdmissionId(TestData.ADMISSION_ID)
                   .enterUhid(TestData.UHID)
                   .enterPatientName(TestData.PATIENT_NAME);
        assertEquals(patientForm.getAdmissionId(), TestData.ADMISSION_ID, "Admission Id is wrong");
        assertEquals(patientForm.getUhid(), TestData.UHID, "UHID is wrong");
        assertEquals(patientForm.getPatientName(), TestData.PATIENT_NAME, "Patient Name is wrong");
    }

    @Test(priority = 6, groups = {"auth", "positive"}, dependsOnMethods = "fillPatientDetails",
            description = "Create the interaction")
    @TestCase(type = Type.POSITIVE,
            steps = "Click Create (and Create anyway if asked).",
            expected = "The interaction is saved and shown with Rahul Sharma and 9811465840.")
    public void createInteraction() throws IOException {
        patientForm.clickCreate();
        patientForm.confirmIfAsked();
        assertTrue(patientForm.waitUntilCreated(), "Interaction was not saved");
        assertTrue(new InteractionsListPage(driver)
                        .isInteractionShown(TestData.PATIENT_NAME, TestData.INTERACTION_CONTACT_NUMBER),
                "The saved interaction is not shown");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "flow_interaction_created"));
    }

    @Test(priority = 7, groups = {"auth", "positive"}, dependsOnMethods = "createInteraction",
            description = "Go to Today")
    @TestCase(type = Type.POSITIVE,
            steps = "Click the three lines in the top-left corner, then click Today.",
            expected = "The home screen opens with Today and Hello, Test.")
    public void goToToday() throws IOException {
        home = new NavigationMenu(driver).open().clickToday();
        assertTrue(home.isLoaded(), "Home screen did not open from Today");
        assertTrue(home.isTodayLabelDisplayed(), "'Today' is not shown");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "flow_today"));
    }
}
