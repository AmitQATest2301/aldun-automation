package tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import base.DriverFactory;
import pages.CreateInteractionFormPage;
import pages.DashboardPage;
import pages.InteractionPage;
import pages.InteractionsListPage;
import pages.LoginPage;
import pages.NavigationMenu;
import utils.ScreenshotUtil;

/**
 * A staff member's day in Aldun, one step per test case, in ONE browser session.
 * Each step continues where the previous one stopped, so nothing is repeated.
 * If a step fails, the steps after it are reported as SKIP.
 *
 * NOTE: runs against the live app - it checks in/out and creates a real interaction.
 */
public class DailyWorkflowTest {

    private static final Logger LOG = LogManager.getLogger(DailyWorkflowTest.class);

    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage home;
    private InteractionPage newInteraction;
    private CreateInteractionFormPage patientForm;

    @BeforeClass(alwaysRun = true)
    public void openBrowser() {
        driver = DriverFactory.initDriver();
        loginPage = new LoginPage(driver);
        home = new DashboardPage(driver);
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        DriverFactory.quitDriver();
    }

    // ------------------------------------------------------------------ 1
    @Test(priority = 1, groups = {"auth", "positive"},
            description = "Open the Aldun login page")
    public void openLoginPage() {
        assertTrue(loginPage.isEmailFieldDisplayed(), "Work email box is not shown");
        assertTrue(loginPage.isPasswordFieldDisplayed(), "Password box is not shown");
        assertTrue(loginPage.isLoginButtonDisplayed(), "Log in button is not shown");
    }

    // ------------------------------------------------------------------ 2
    @Test(priority = 2, groups = {"auth", "positive"}, dependsOnMethods = "openLoginPage",
            description = "Log in with the correct work email and password")
    public void logIn() {
        String email = TestData.resolve(TestData.VALID_EMAIL);
        String password = TestData.resolve(TestData.VALID_PASSWORD);

        loginPage.enterEmail(email).enterPassword(password);
        loginPage.showPassword();
        assertEquals(loginPage.getEmailFieldValue(), email, "Email was not typed correctly");
        assertEquals(loginPage.getPasswordFieldValue(), password, "Password was not typed correctly");
        loginPage.clickLogin();

        assertTrue(home.isLoaded(), "Home screen did not open after Log in");
        assertTrue(home.getGreeting().startsWith("Hello,"), "Greeting 'Hello, ...' is not shown");
    }

    // ------------------------------------------------------------------ 3
    @Test(priority = 3, groups = {"auth", "positive"}, dependsOnMethods = "logIn",
            description = "Check out first if already checked in")
    public void checkOutIfAlreadyCheckedIn() throws IOException {
        if (home.isNotCheckedIn()) {
            LOG.info("Status is already 'Not checked in' - nothing to check out");
            return;
        }
        home.clickCheckOut();
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step3_checked_out"));
        assertTrue(home.waitUntilCheckedOut(), "Status did not change to 'Not checked in'");
    }

    // ------------------------------------------------------------------ 4
    @Test(priority = 4, groups = {"auth", "positive"}, dependsOnMethods = "checkOutIfAlreadyCheckedIn",
            description = "Check in for the day")
    public void checkIn() throws IOException {
        assertTrue(home.isCheckInButtonDisplayed(), "Check in button is not shown");
        home.clickCheckIn();
        boolean done = home.waitForCheckInResponse();
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step4_checked_in"));
        assertTrue(done, "Check in did not complete");
    }

    // ------------------------------------------------------------------ 5
    @Test(priority = 5, groups = {"auth", "positive"}, dependsOnMethods = "checkIn",
            description = "Open a new interaction from the Interactions list")
    public void openNewInteraction() {
        InteractionsListPage list = new InteractionsListPage(driver);
        assertTrue(list.isLoaded(), "Interactions list did not open after Check in");
        newInteraction = list.clickNewInteraction();
        assertTrue(newInteraction.isLoaded(), "New Interaction screen did not open");
    }

    // ------------------------------------------------------------------ 6
    @Test(priority = 6, groups = {"auth", "positive"}, dependsOnMethods = "openNewInteraction",
            description = "Enter the attender phone number and continue")
    public void enterAttenderPhoneNumber() throws IOException {
        newInteraction.enterAttenderPhoneNumber(TestData.INTERACTION_CONTACT_NUMBER);
        String typed = newInteraction.getAttenderPhoneNumber().replaceAll("\\D", "");
        assertTrue(typed.endsWith(TestData.INTERACTION_CONTACT_NUMBER), "Phone number was not typed correctly");
        assertTrue(newInteraction.isNumberHistoryShown(), "'Aldun history for this number' is not shown");

        assertTrue(newInteraction.isCreateInteractionEnabled(), "Create Interaction button is not active");
        String formUrl = newInteraction.getCurrentUrl();
        newInteraction.clickCreateInteraction();
        newInteraction.confirmIfAsked(); // number already known: Create anyway
        assertTrue(newInteraction.waitUntilSubmitted(formUrl), "Did not move on after Create Interaction");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step6_phone_entered"));
    }

    // ------------------------------------------------------------------ 7
    @Test(priority = 7, groups = {"auth", "positive"}, dependsOnMethods = "enterAttenderPhoneNumber",
            description = "Fill in patient details (Admission Id, UHID, Patient Name)")
    public void fillPatientDetails() throws IOException {
        patientForm = new CreateInteractionFormPage(driver);
        assertTrue(patientForm.isLoaded(), "'Create New Interaction' form is not shown");
        patientForm.enterAdmissionId(TestData.ADMISSION_ID)
                   .enterUhid(TestData.UHID)
                   .enterPatientName(TestData.PATIENT_NAME);
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step7_patient_details"));

        assertEquals(patientForm.getAdmissionId(), TestData.ADMISSION_ID, "Admission Id is wrong");
        assertEquals(patientForm.getUhid(), TestData.UHID, "UHID is wrong");
        assertEquals(patientForm.getPatientName(), TestData.PATIENT_NAME, "Patient Name is wrong");
    }

    // ------------------------------------------------------------------ 8
    @Test(priority = 8, groups = {"auth", "positive"}, dependsOnMethods = "fillPatientDetails",
            description = "Create the interaction and see it saved")
    public void createInteraction() throws IOException {
        patientForm.clickCreate();
        patientForm.confirmIfAsked(); // same patient already exists: Create anyway
        assertTrue(patientForm.waitUntilCreated(), "Interaction was not saved");

        InteractionsListPage saved = new InteractionsListPage(driver);
        assertTrue(saved.isInteractionShown(TestData.PATIENT_NAME, TestData.INTERACTION_CONTACT_NUMBER),
                "The saved interaction is not shown");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step8_interaction_created"));
    }

    // ------------------------------------------------------------------ 9
    @Test(priority = 9, groups = {"auth", "positive"}, dependsOnMethods = "createInteraction",
            description = "Go back to Today using the menu")
    public void goToTodayFromMenu() throws IOException {
        home = new NavigationMenu(driver).open().clickToday();
        assertTrue(home.isLoaded(), "Home screen did not open from the menu");
        assertTrue(home.isTodayLabelDisplayed(), "'Today' is not shown");
        assertTrue(home.isCheckedIn(), "Status is not 'Checked in'");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step9_today"));
    }

    // ------------------------------------------------------------------ 10
    @Test(priority = 10, groups = {"auth", "positive"}, dependsOnMethods = "goToTodayFromMenu",
            description = "Check out at the end of the day")
    public void checkOutAtEndOfDay() throws IOException {
        assertTrue(home.isCheckOutButtonDisplayed(), "Check out button is not shown");
        home.clickCheckOut();
        assertTrue(home.waitUntilCheckedOut(), "Status did not change to 'Not checked in'");
        LOG.info("Screenshot: {}", ScreenshotUtil.capture(driver, "step10_checked_out"));
    }
}
