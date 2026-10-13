package tests.cases;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import base.IndependentTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.DashboardPage;
import pages.InteractionsListPage;
import pages.LoginPage;
import pages.NavigationMenu;
import tests.TestData;

/** ALD027-ALD030: search, menu and log out. Each test logs in in its own browser. */
public class NavigationCases extends IndependentTest {

    private InteractionsListPage interactionsList() {
        logIn();
        new NavigationMenu(driver()).open().clickItem("Interactions");
        InteractionsListPage list = new InteractionsListPage(driver());
        assertTrue(list.isLoaded(), "Interactions list did not open from the menu");
        return list;
    }

    @Test(groups = {"auth", "positive"}, description = "Search by phone number")
    @TestCase(id = "ALD027", type = Type.POSITIVE,
            steps = "Log in, open Interactions from the menu and search for 9811465840.",
            expected = "The interaction for Rahul Sharma is shown.")
    public void searchByPhone() {
        InteractionsListPage list = interactionsList().search(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(list.isInteractionShown(TestData.PATIENT_NAME), "No result for the phone number");
    }

    @Test(groups = {"auth", "negative"}, description = "Search with no match")
    @TestCase(id = "ALD028", type = Type.NEGATIVE,
            steps = "Log in, open Interactions from the menu and search for text that matches nobody (zz-no-such-patient-000).",
            expected = "No interactions are shown (Rahul Sharma disappears from the list).")
    public void searchWithNoMatch() {
        InteractionsListPage list = interactionsList().search(TestData.NO_MATCH_SEARCH);
        assertTrue(list.isTextGone(TestData.PATIENT_NAME), "Results still shown for a search that should match nothing");
    }

    @Test(groups = {"auth", "positive"}, description = "Menu shows all sections")
    @TestCase(id = "ALD029", type = Type.POSITIVE,
            steps = "Log in and click the three lines in the top-left corner.",
            expected = "The menu opens with Today, Interactions, Admissions, Master Search, Performance, "
                    + "Leaderboard, Reports, Report Issue and Log out.")
    public void menuShowsAllSections() {
        logIn();
        NavigationMenu menu = new NavigationMenu(driver()).open();
        for (String item : NavigationMenu.ITEMS) {
            assertTrue(menu.isItemShown(item), "Menu item missing: " + item);
        }
    }

    @Test(groups = {"auth", "positive"}, description = "Log out")
    @TestCase(id = "ALD030", type = Type.POSITIVE,
            steps = "Log in, open the menu and click Log out. Then try to open the staff home address again.",
            expected = "The login screen is shown, and the staff home cannot be opened without logging in again.")
    public void logOut() {
        DashboardPage home = logIn();
        LoginPage login = new NavigationMenu(driver()).open().clickLogOut();
        assertTrue(login.isEmailFieldDisplayed(), "Login screen not shown after Log out");
        home.open();
        assertTrue(login.isEmailFieldDisplayed(), "Staff home opened again after logging out");
    }
}
