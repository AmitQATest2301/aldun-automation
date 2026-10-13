package tests;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.InteractionsListPage;
import pages.NavigationMenu;

/**
 * Searching the Interactions list. Relies on the interaction for Rahul Sharma / 9811465840 /
 * AD556142 that the Daily Workflow creates earlier in the run. Logs in once.
 */
public class InteractionSearchTest extends SessionTest {

    private InteractionsListPage list;

    @BeforeClass(alwaysRun = true)
    public void logIn() {
        logInAsStaff();
    }

    @BeforeMethod(alwaysRun = true)
    public void openInteractionsList() {
        new pages.DashboardPage(driver).open();
        new NavigationMenu(driver).open().clickItem("Interactions");
        list = new InteractionsListPage(driver);
        assertTrue(list.isLoaded(), "Interactions list did not open from the menu");
    }

    @Test(priority = 1, groups = {"auth", "positive"}, description = "Search by phone number")
    @TestCase(type = Type.POSITIVE,
            steps = "Open Interactions from the menu and search for the phone number 9811465840.",
            expected = "The interaction for Rahul Sharma is shown.")
    public void searchByPhone() {
        list.search(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(list.isInteractionShown(TestData.PATIENT_NAME), "No result for the phone number");
    }

    @Test(priority = 2, groups = {"auth", "positive"}, description = "Search by Admission Id")
    @TestCase(type = Type.POSITIVE,
            steps = "Open Interactions from the menu and search for the Admission Id AD556142.",
            expected = "The interaction for Rahul Sharma is shown.")
    public void searchByAdmissionId() {
        list.search(TestData.ADMISSION_ID);
        assertTrue(list.isInteractionShown(TestData.PATIENT_NAME), "No result for the Admission Id");
    }

    @Test(priority = 3, groups = {"auth", "positive"}, description = "Search by patient name")
    @TestCase(type = Type.POSITIVE,
            steps = "Open Interactions from the menu and search for the patient name Rahul Sharma.",
            expected = "The interaction with phone number 9811465840 is shown.")
    public void searchByPatientName() {
        list.search(TestData.PATIENT_NAME);
        assertTrue(list.isInteractionShown(TestData.INTERACTION_CONTACT_NUMBER), "No result for the patient name");
    }

    @Test(priority = 4, groups = {"auth", "negative"}, description = "Search with no match")
    @TestCase(type = Type.NEGATIVE,
            steps = "Open Interactions from the menu and search for text that matches nobody (zz-no-such-patient-000).",
            expected = "No interactions are shown (Rahul Sharma disappears from the list).")
    public void searchWithNoMatch() {
        list.search(TestData.NO_MATCH_SEARCH);
        assertTrue(list.isTextGone(TestData.PATIENT_NAME), "Results still shown for a search that should match nothing");
    }
}
