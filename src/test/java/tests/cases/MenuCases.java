package tests.cases;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.DashboardPage;
import pages.LoginPage;
import pages.MenuScreens;
import pages.NavigationMenu;
import tests.TestData;
import utils.WaitUtils;

/**
 * ALD001-ALD050: the side menu and every screen it opens (Admissions, Master Search, Performance,
 * Leaderboard, Reports, Report Issue), ending with Log out. One browser, logged in once.
 * Read-only on the live app: nothing is submitted, generated or downloaded.
 */
public class MenuCases extends SessionTest {

    private static final String ADMISSION_ROW = "admission-row-";
    private static final String ISSUE_ROW = "report-issue-row-";
    private static final String NO_MATCH_PHONE = "9000000001";

    private DashboardPage home;
    private MenuScreens screen;

    /** Runs after SessionTest.openBrowser - TestNG does not otherwise promise that order. */
    @BeforeClass(alwaysRun = true, dependsOnMethods = "openBrowser")
    public void logIn() {
        home = logInAsStaff();
        screen = new MenuScreens(driver);
    }

    /**
     * Opens the menu from whichever screen is showing (it is on every screen), and only if it is
     * not open already - so consecutive menu checks do not repeat the same clicks.
     */
    private NavigationMenu openMenu() {
        return new NavigationMenu(driver).ensureOpen();
    }

    /**
     * Opens a screen from the menu, unless it is already showing: the checks for one screen run
     * one after another on the same page instead of reopening it each time.
     */
    private void openFromMenu(String item, String urlPart) {
        if (driver.getCurrentUrl().contains(urlPart) && !new NavigationMenu(driver).isOpenNow()) {
            return;
        }
        openMenu().clickItem(item);
        assertTrue(screen.waitForUrl(urlPart), "'" + item + "' did not open (" + urlPart + " not in the address)");
    }

    /** Opens a screen and reloads it, for checks that need it unfiltered (no earlier search or filter). */
    private void openClean(String item, String urlPart) {
        openFromMenu(item, urlPart);
        driver.navigate().refresh();
        assertTrue(screen.waitForUrl(urlPart), "'" + item + "' did not reload");
    }

    // ================= Menu =================

    @Test(groups = {"auth", "positive"}, description = "Menu opens with user details")
    @TestCase(id = "ALD001", type = Type.POSITIVE,
            steps = "On the home screen, click the three lines in the top-left corner.",
            expected = "The menu opens showing the user 'Test User' and the role 'Hops staff'.")
    public void menuShowsUser() {
        openMenu();
        assertTrue(screen.isTextShown("Test User"), "'Test User' not shown in the menu");
        assertTrue(screen.isTextShown("Hops staff"), "'Hops staff' not shown in the menu");
    }

    @Test(groups = {"auth", "positive"}, description = "Menu shows all sections")
    @TestCase(id = "ALD002", type = Type.POSITIVE,
            steps = "Open the menu.",
            expected = "Today, Interactions, Admissions, Master Search, Performance, Leaderboard, Reports, "
                    + "Report Issue and Log out are all shown.")
    public void menuShowsAllSections() {
        NavigationMenu menu = openMenu();
        for (String item : NavigationMenu.ITEMS) {
            assertTrue(menu.isItemShown(item), "Menu item missing: " + item);
        }
        assertTrue(screen.isAriaShown("Log out"), "Log out not shown in the menu");
    }

    @Test(groups = {"auth", "positive"}, description = "Menu items are in the right order")
    @TestCase(id = "ALD003", type = Type.POSITIVE,
            steps = "Open the menu and read the items from top to bottom.",
            expected = "Order: Today, Interactions, Admissions, Master Search, Performance, Leaderboard, Reports, Report Issue.")
    public void menuOrder() {
        List<Integer> tops = openMenu().itemTops(NavigationMenu.ITEMS);
        for (int i = 1; i < tops.size(); i++) {
            assertTrue(tops.get(i) > tops.get(i - 1),
                    NavigationMenu.ITEMS.get(i) + " is not below " + NavigationMenu.ITEMS.get(i - 1));
        }
    }

    @Test(groups = {"auth", "edge"}, description = "Report Issue badge is a number")
    @TestCase(id = "ALD004", type = Type.EDGE,
            steps = "Open the menu and look at the badge next to Report Issue.",
            expected = "If a badge is shown it is a whole number (the count of new issues), nothing else.")
    public void reportIssueBadgeIsNumber() {
        String text = openMenu().getItemText("Report Issue");
        assertTrue(text.matches("Report Issue( \\d+)?"), "Unexpected Report Issue label/badge: '" + text + "'");
    }

    @Test(groups = {"auth", "positive"}, description = "Profile button in the menu")
    @TestCase(id = "ALD005", type = Type.POSITIVE,
            steps = "Open the menu and look at the top.",
            expected = "A 'View your profile' button with the initials TU is shown.")
    public void profileButtonShown() {
        openMenu();
        assertTrue(screen.isAriaShown("View your profile"), "'View your profile' button not shown");
        assertTrue(screen.isTextShown("TU"), "Initials 'TU' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Close the menu with X")
    @TestCase(id = "ALD006", type = Type.POSITIVE,
            steps = "Open the menu and click the X (Close menu).",
            expected = "The menu closes and the home screen is shown again.")
    public void closeMenu() {
        NavigationMenu menu = openMenu();
        assertTrue(menu.close(), "Menu did not close");
        assertTrue(home.isTextShownOnPage("Hello,"), "Home screen not shown after closing the menu");
    }

    @Test(groups = {"auth", "positive"}, description = "Today returns home")
    @TestCase(id = "ALD007", type = Type.POSITIVE,
            steps = "Open Interactions from the menu, then open the menu again and click Today.",
            expected = "The home screen opens with 'Today', 'Hello, ...' and 'Your shift'.")
    public void todayReturnsHome() {
        openFromMenu("Interactions", "/staff/my-work");
        DashboardPage today = openMenu().clickToday();
        assertTrue(today.isLoaded(), "Home screen did not open from Today");
        assertTrue(screen.isTextShown("Your shift"), "'Your shift' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Open Interactions")
    @TestCase(id = "ALD008", type = Type.POSITIVE,
            steps = "Open the menu and click Interactions.",
            expected = "The Interactions list opens with the search box and the New interaction button.")
    public void openInteractions() {
        openFromMenu("Interactions", "/staff/my-work");
        assertTrue(screen.isFieldShown(MenuScreens.INTERACTIONS_SEARCH), "Interactions search box not shown");
        assertTrue(screen.isAriaShown("New interaction"), "New interaction button not shown");
    }

    // ================= Admissions =================

    @Test(groups = {"auth", "positive"}, description = "Open Admissions")
    @TestCase(id = "ALD009", type = Type.POSITIVE,
            steps = "Open the menu and click Admissions.",
            expected = "The Admissions screen opens (address ends with ?filter=ADMISSIONS).")
    public void openAdmissions() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        assertTrue(screen.isTextShown("Admissions"), "'Admissions' title not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Admissions search box")
    @TestCase(id = "ALD010", type = Type.POSITIVE,
            steps = "Open Admissions and look at the search box.",
            expected = "The search box says 'Phone, Adm ID, UHID or Name'.")
    public void admissionsSearchBox() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        assertEquals(screen.placeholderOf(MenuScreens.ADMISSIONS_SEARCH), "Phone, Adm ID, UHID or Name",
                "Wrong search hint");
    }

    @Test(groups = {"auth", "positive"}, description = "Admissions 'Created by me'")
    @TestCase(id = "ALD011", type = Type.POSITIVE,
            steps = "Open Admissions.",
            expected = "The 'Created by me' option is shown.")
    public void admissionsCreatedByMe() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        assertTrue(screen.isTextShown("Created by me"), "'Created by me' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Admissions status filter")
    @TestCase(id = "ALD012", type = Type.POSITIVE,
            steps = "Open Admissions.",
            expected = "The 'Filter by case status' button is shown.")
    public void admissionsStatusFilter() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        assertTrue(screen.isAriaShown("Filter by case status"), "'Filter by case status' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Admissions list shows cases")
    @TestCase(id = "ALD013", type = Type.POSITIVE,
            steps = "Open Admissions and look at the list.",
            expected = "At least one admission is listed with its Case ID and Status.")
    public void admissionsListShowsCases() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        assertTrue(screen.waitForRows(ADMISSION_ROW) > 0, "No admissions listed");
        assertTrue(screen.isTextShown("Case ID:"), "'Case ID:' not shown on the admissions");
        assertTrue(screen.isTextShown("Status:"), "'Status:' not shown on the admissions");
    }

    @Test(groups = {"auth", "positive"}, description = "Search admissions by phone")
    @TestCase(id = "ALD014", type = Type.POSITIVE,
            steps = "Open Admissions, take the phone number of the first admission and search for it.",
            expected = "That admission is still listed after the search.")
    public void searchAdmissionsByPhone() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        assertTrue(screen.waitForRows(ADMISSION_ROW) > 0, "No admissions listed to search for");
        String phone = screen.firstPhoneOnPage();
        assertNotNull(phone, "No phone number shown on the admissions list");
        screen.typeInto(MenuScreens.ADMISSIONS_SEARCH, phone);
        assertTrue(screen.waitForRows(ADMISSION_ROW) > 0, "No admission found for " + phone);
        assertTrue(screen.isTextShown(phone), "Searched phone " + phone + " not shown in the results");
    }

    @Test(groups = {"auth", "negative"}, description = "Search admissions with no match")
    @TestCase(id = "ALD015", type = Type.NEGATIVE,
            steps = "Open Admissions and search for text that matches nobody (zz-no-such-patient-000).",
            expected = "No admissions are listed.")
    public void searchAdmissionsNoMatch() {
        openFromMenu("Admissions", "filter=ADMISSIONS");
        screen.typeInto(MenuScreens.ADMISSIONS_SEARCH, TestData.NO_MATCH_SEARCH);
        assertTrue(screen.rowsGone(ADMISSION_ROW), "Admissions still listed for a search that matches nothing");
    }

    // ================= Master Search =================

    @Test(groups = {"auth", "positive"}, description = "Open Master Search")
    @TestCase(id = "ALD016", type = Type.POSITIVE,
            steps = "Open the menu and click Master Search.",
            expected = "Master Search opens with 'Search anywhere' and 'Enter a phone, admission ID or UHID, then tap Search.'")
    public void openMasterSearch() {
        openFromMenu("Master Search", "filter=MASTER_SEARCH");
        assertTrue(screen.isTextShown("Search anywhere"), "'Search anywhere' not shown");
        assertTrue(screen.isTextShown("Enter a phone, admission ID or UHID, then tap Search."), "Search hint not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Master Search box hint")
    @TestCase(id = "ALD017", type = Type.POSITIVE,
            steps = "Open Master Search and look at the search box.",
            expected = "The search box says 'Phone, Admission ID or UHID'.")
    public void masterSearchHint() {
        openFromMenu("Master Search", "filter=MASTER_SEARCH");
        assertEquals(screen.placeholderOf(MenuScreens.MASTER_SEARCH_INPUT), "Phone, Admission ID or UHID",
                "Wrong search hint");
    }

    @Test(groups = {"auth", "negative"}, description = "Master Search empty")
    @TestCase(id = "ALD018", type = Type.NEGATIVE,
            steps = "Open Master Search and leave the search box empty.",
            expected = "The Search button is disabled.")
    public void masterSearchDisabledWhenEmpty() {
        openFromMenu("Master Search", "filter=MASTER_SEARCH");
        assertFalse(screen.isEnabledById(MenuScreens.MASTER_SEARCH_SUBMIT), "Search is active with an empty box");
    }

    @Test(groups = {"auth", "positive"}, description = "Master Search becomes active")
    @TestCase(id = "ALD019", type = Type.POSITIVE,
            steps = "Open Master Search and type 9811465840.",
            expected = "The Search button becomes active.")
    public void masterSearchEnabledAfterTyping() {
        openFromMenu("Master Search", "filter=MASTER_SEARCH");
        screen.typeInto(MenuScreens.MASTER_SEARCH_INPUT, TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(screen.becomesEnabled(MenuScreens.MASTER_SEARCH_SUBMIT), "Search did not become active");
    }

    @Test(groups = {"auth", "positive"}, description = "Master Search by phone")
    @TestCase(id = "ALD020", type = Type.POSITIVE,
            steps = "Open Master Search, type 9811465840 and click Search.",
            expected = "Results for Rahul Sharma are shown.")
    public void masterSearchByPhone() {
        masterSearch(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(screen.isTextShown(TestData.PATIENT_NAME), "No result for the phone number");
    }

    @Test(groups = {"auth", "positive"}, description = "Master Search by Admission ID")
    @TestCase(id = "ALD021", type = Type.POSITIVE,
            steps = "Open Master Search, type AD556142 and click Search.",
            expected = "Results for Rahul Sharma are shown.")
    public void masterSearchByAdmissionId() {
        masterSearch(TestData.ADMISSION_ID);
        assertTrue(screen.isTextShown(TestData.PATIENT_NAME), "No result for the Admission ID");
    }

    @Test(groups = {"auth", "positive"}, description = "Master Search by UHID")
    @TestCase(id = "ALD022", type = Type.POSITIVE,
            steps = "Open Master Search, type UH558440 and click Search.",
            expected = "Results for Rahul Sharma are shown.")
    public void masterSearchByUhid() {
        masterSearch(TestData.UHID);
        assertTrue(screen.isTextShown(TestData.PATIENT_NAME), "No result for the UHID");
    }

    @Test(groups = {"auth", "negative"}, description = "Master Search with no match")
    @TestCase(id = "ALD023", type = Type.NEGATIVE,
            steps = "With Rahul Sharma's results on screen, search 9000000001, which belongs to nobody.",
            expected = "Rahul Sharma is no longer shown - nothing is found for the unknown number.")
    public void masterSearchNoMatch() {
        if (!screen.getPageText().contains(TestData.PATIENT_NAME)) {
            masterSearch(TestData.INTERACTION_CONTACT_NUMBER); // only when run on its own
            assertTrue(screen.isTextShown(TestData.PATIENT_NAME), "Rahul Sharma not found first");
        }
        masterSearch(NO_MATCH_PHONE);
        assertTrue(screen.isTextGone(TestData.PATIENT_NAME), "Rahul Sharma still shown for an unknown number");
    }

    @Test(groups = {"auth", "edge"}, description = "Master Search with spaces only")
    @TestCase(id = "ALD024", type = Type.EDGE,
            steps = "Open Master Search and type only spaces.",
            expected = "The Search button stays disabled - spaces are not a search.")
    public void masterSearchSpacesOnly() {
        openFromMenu("Master Search", "filter=MASTER_SEARCH");
        screen.typeRawInto(MenuScreens.MASTER_SEARCH_INPUT, "     ");
        WaitUtils.pause(1000);
        assertFalse(screen.isEnabledById(MenuScreens.MASTER_SEARCH_SUBMIT), "Search is active with only spaces");
    }

    private void masterSearch(String text) {
        openFromMenu("Master Search", "filter=MASTER_SEARCH");
        screen.typeInto(MenuScreens.MASTER_SEARCH_INPUT, text);
        assertTrue(screen.becomesEnabled(MenuScreens.MASTER_SEARCH_SUBMIT), "Search did not become active");
        screen.clickById(MenuScreens.MASTER_SEARCH_SUBMIT);
    }

    // ================= Performance =================

    @Test(groups = {"auth", "positive"}, description = "Open Performance")
    @TestCase(id = "ALD025", type = Type.POSITIVE,
            steps = "Open the menu and click Performance.",
            expected = "The Performance screen opens (/staff/performance).")
    public void openPerformance() {
        openFromMenu("Performance", "/staff/performance");
        assertTrue(screen.isTextShown("Performance"), "'Performance' title not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Performance period tabs")
    @TestCase(id = "ALD026", type = Type.POSITIVE,
            steps = "Open Performance.",
            expected = "'Today' and 'This month' tabs are shown.")
    public void performanceTabs() {
        openFromMenu("Performance", "/staff/performance");
        assertTrue(screen.isTextShown("This month"), "'This month' not shown");
        assertTrue(screen.pageLines().contains("Today"), "'Today' tab not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "My progress figures")
    @TestCase(id = "ALD027", type = Type.POSITIVE,
            steps = "Open Performance and look at MY PROGRESS.",
            expected = "MY PROGRESS shows Interactions, Onboardings and Discharges.")
    public void myProgress() {
        openFromMenu("Performance", "/staff/performance");
        assertTrue(screen.isTextShown("MY PROGRESS"), "'MY PROGRESS' not shown");
        for (String label : List.of("Interactions", "Onboardings", "Discharges")) {
            assertTrue(screen.isTextShown(label), "'" + label + "' not shown");
        }
    }

    @Test(groups = {"auth", "positive"}, description = "Hospital progress")
    @TestCase(id = "ALD028", type = Type.POSITIVE,
            steps = "Open Performance and look at HOSPITAL PROGRESS.",
            expected = "HOSPITAL PROGRESS shows the user's hospital, test carefi hospital delhi.")
    public void hospitalProgress() {
        openFromMenu("Performance", "/staff/performance");
        assertTrue(screen.isTextShown("HOSPITAL PROGRESS"), "'HOSPITAL PROGRESS' not shown");
        assertTrue(screen.isTextShown("test carefi hospital delhi"), "Hospital name not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Performance for this month")
    @TestCase(id = "ALD029", type = Type.POSITIVE,
            steps = "Open Performance and click 'This month'.",
            expected = "The figures stay on screen: MY PROGRESS and HOSPITAL PROGRESS are shown for the month.")
    public void performanceThisMonth() {
        openFromMenu("Performance", "/staff/performance");
        screen.clickTab("This month");
        assertTrue(screen.isTextShown("MY PROGRESS"), "'MY PROGRESS' not shown for this month");
        assertTrue(screen.isTextShown("HOSPITAL PROGRESS"), "'HOSPITAL PROGRESS' not shown for this month");
    }

    @Test(groups = {"auth", "positive"}, description = "Show incentive")
    @TestCase(id = "ALD030", type = Type.POSITIVE,
            steps = "Open Performance and click 'Show incentive'.",
            expected = "The screen stays on Performance and responds without an error.")
    public void showIncentive() {
        openFromMenu("Performance", "/staff/performance");
        assertTrue(screen.isAriaShown("Show incentive"), "'Show incentive' not shown");
        screen.clickAria("Show incentive");
        assertTrue(screen.waitForUrl("/staff/performance"), "Left Performance after Show incentive");
        assertFalse(screen.pageMatches("(?i)something went wrong|error occurred"), "An error was shown");
    }

    // ================= Leaderboard =================

    @Test(groups = {"auth", "positive"}, description = "Open Leaderboard")
    @TestCase(id = "ALD031", type = Type.POSITIVE,
            steps = "Open the menu and click Leaderboard.",
            expected = "The Leaderboard screen opens (/staff/leaderboard).")
    public void openLeaderboard() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        assertTrue(screen.isTextShown("Leaderboard"), "'Leaderboard' title not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Leaderboard period tabs")
    @TestCase(id = "ALD032", type = Type.POSITIVE,
            steps = "Open Leaderboard.",
            expected = "'Today' and 'This month' tabs are shown.")
    public void leaderboardTabs() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        assertTrue(screen.isTextShown("This month"), "'This month' not shown");
        assertTrue(screen.pageLines().contains("Today"), "'Today' tab not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Leaderboard ranking note")
    @TestCase(id = "ALD033", type = Type.POSITIVE,
            steps = "Open Leaderboard and read the note under the tabs.",
            expected = "It explains the ranking: 'Ranked by projected achievement for this month, not discharges done so far.'")
    public void leaderboardRankingNote() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        assertTrue(screen.isTextShown("Ranked by projected achievement"), "Ranking note not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Own hospital on Leaderboard")
    @TestCase(id = "ALD034", type = Type.POSITIVE,
            steps = "Open Leaderboard and look at 'Your hospitals'.",
            expected = "'Your hospitals' lists test carefi hospital delhi.")
    public void leaderboardOwnHospital() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        assertTrue(screen.isTextShown("Your hospitals"), "'Your hospitals' not shown");
        assertTrue(screen.isTextShown("test carefi hospital delhi"), "Own hospital not listed");
    }

    @Test(groups = {"auth", "positive"}, description = "Leaderboard entries")
    @TestCase(id = "ALD035", type = Type.POSITIVE,
            steps = "Open Leaderboard and read the hospital entries.",
            expected = "Entries show 'N discharges of M target' and a percentage.")
    public void leaderboardEntries() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        assertTrue(screen.pageMatches("\\d+ discharges of \\d+ target"), "No 'discharges of ... target' entries");
        assertTrue(screen.pageMatches("\\d+%"), "No percentages shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Leaderboard switch tabs")
    @TestCase(id = "ALD036", type = Type.POSITIVE,
            steps = "Open Leaderboard, click 'This month', then click 'Today'.",
            expected = "The leaderboard stays on screen after each switch.")
    public void leaderboardSwitchTabs() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        screen.clickTab("This month");
        assertTrue(screen.isTextShown("Your hospitals"), "Leaderboard missing after 'This month'");
        screen.clickTab("Today");
        assertTrue(screen.isTextShown("Your hospitals"), "Leaderboard missing after 'Today'");
    }

    // ================= Reports =================

    @Test(groups = {"auth", "positive"}, description = "Open Reports")
    @TestCase(id = "ALD037", type = Type.POSITIVE,
            steps = "Open the menu and click Reports.",
            expected = "The Reports screen opens with 'Generate a report' (/staff/reports).")
    public void openReports() {
        openFromMenu("Reports", "/staff/reports");
        assertTrue(screen.isTextShown("Generate a report"), "'Generate a report' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Report type")
    @TestCase(id = "ALD038", type = Type.POSITIVE,
            steps = "Open Reports and look at the Report field.",
            expected = "The report type is 'Interaction Report'.")
    public void reportType() {
        openFromMenu("Reports", "/staff/reports");
        assertTrue(screen.isAriaShown("Report"), "Report field not shown");
        assertTrue(screen.isTextShown("Interaction Report"), "'Interaction Report' not shown");
    }

    @Test(groups = {"auth", "negative"}, description = "Generate without a date range")
    @TestCase(id = "ALD039", type = Type.NEGATIVE,
            steps = "Open Reports and do not choose a date range.",
            expected = "'Select date range' is shown and Generate Report is disabled.")
    public void generateDisabledWithoutDates() {
        openFromMenu("Reports", "/staff/reports");
        assertTrue(screen.isTextShown("Select date range"), "'Select date range' not shown");
        assertFalse(screen.isAriaEnabled("Generate Report"), "Generate Report is active without a date range");
    }

    @Test(groups = {"auth", "positive"}, description = "Date range picker opens")
    @TestCase(id = "ALD042", type = Type.POSITIVE,
            steps = "Open Reports and click 'Select date range'. Then press Esc.",
            expected = "A date picker opens (new content appears). Nothing is generated.")
    public void dateRangePickerOpens() {
        openFromMenu("Reports", "/staff/reports");
        String before = screen.getPageText();
        screen.clickAria("Date range");
        WaitUtils.pause(1500);
        assertFalse(screen.getPageText().equals(before), "Nothing opened after clicking 'Select date range'");
        screen.pressEscape();
    }

    @Test(groups = {"auth", "positive"}, description = "Requested reports list")
    @TestCase(id = "ALD040", type = Type.POSITIVE,
            steps = "Open Reports and look at 'Requested reports'.",
            expected = "Earlier reports are listed with the status 'Ready'.")
    public void requestedReports() {
        openFromMenu("Reports", "/staff/reports");
        assertTrue(screen.isTextShown("Requested reports"), "'Requested reports' not shown");
        assertTrue(screen.isTextShown("Ready"), "No report with status 'Ready'");
    }

    @Test(groups = {"auth", "positive"}, description = "Download buttons on reports")
    @TestCase(id = "ALD041", type = Type.POSITIVE,
            steps = "Open Reports and look at the requested reports (do not download).",
            expected = "Each ready report has a 'Download report' button.")
    public void downloadButtons() {
        openFromMenu("Reports", "/staff/reports");
        assertTrue(screen.isAriaShown("Download report"), "No 'Download report' button shown");
        assertTrue(screen.countAria("Download report") >= 1, "No 'Download report' buttons");
    }

    // ================= Report Issue =================

    @Test(groups = {"auth", "positive"}, description = "Open Report Issue")
    @TestCase(id = "ALD043", type = Type.POSITIVE,
            steps = "Open the menu and click Report Issue.",
            expected = "The Report Issue screen opens (/staff/report-issue) with 'Report a new issue'.")
    public void openReportIssue() {
        openFromMenu("Report Issue", "/staff/report-issue");
        assertTrue(screen.isAriaShown("Report a new issue"), "'Report a new issue' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Report Issue filters")
    @TestCase(id = "ALD044", type = Type.POSITIVE,
            steps = "Open Report Issue.",
            expected = "Filters 'Raised by me', 'To do', 'Validating' and 'Completed' and 'Search by title' are shown.")
    public void reportIssueFilters() {
        openFromMenu("Report Issue", "/staff/report-issue");
        for (String label : List.of("Show only my tickets", "Filter by status To do",
                "Filter by status Validating", "Filter by status Completed", "Search by title")) {
            assertTrue(screen.isAriaShown(label), "Not shown: " + label);
        }
    }

    @Test(groups = {"auth", "positive"}, description = "Issue list")
    @TestCase(id = "ALD045", type = Type.POSITIVE,
            steps = "Open Report Issue and look at the list.",
            expected = "Issues are listed, each with an 'I'm facing this issue too' button.")
    public void issueList() {
        openFromMenu("Report Issue", "/staff/report-issue");
        assertTrue(screen.waitForRows(ISSUE_ROW) > 0, "No issues listed");
        assertTrue(screen.isAriaShown("I'm facing this issue too"), "'I'm facing this issue too' not shown");
    }

    @Test(groups = {"auth", "positive"}, description = "Completed filter")
    @TestCase(id = "ALD046", type = Type.POSITIVE,
            steps = "Open Report Issue and click 'Completed'.",
            expected = "No issue with status 'to do' is listed.")
    public void completedFilter() {
        openFromMenu("Report Issue", "/staff/report-issue");
        screen.waitForRows(ISSUE_ROW);
        screen.clickAria("Filter by status Completed");
        WaitUtils.pause(2000);
        assertFalse(screen.pageLines().contains("to do"), "'to do' issues still listed under Completed");
    }

    @Test(groups = {"auth", "positive"}, description = "Raised by me filter")
    @TestCase(id = "ALD047", type = Type.POSITIVE,
            steps = "Open Report Issue, count the issues, then click 'Raised by me'.",
            expected = "The list is the same or shorter - only the user's own issues remain.")
    public void raisedByMeFilter() {
        openClean("Report Issue", "/staff/report-issue");
        int all = screen.waitForRows(ISSUE_ROW);
        screen.clickAria("Show only my tickets");
        WaitUtils.pause(2000);
        int mine = screen.countRows(ISSUE_ROW);
        assertTrue(mine <= all, "'Raised by me' shows more issues (" + mine + ") than all issues (" + all + ")");
    }

    @Test(groups = {"auth", "positive"}, description = "Badge clears after opening Report Issue")
    @TestCase(id = "ALD048", type = Type.POSITIVE,
            steps = "After opening Report Issue, open the menu again.",
            expected = "Report Issue no longer shows a number badge - the new issues have been seen.")
    public void badgeClearsAfterOpening() {
        openFromMenu("Report Issue", "/staff/report-issue");
        assertEquals(openMenu().getItemText("Report Issue"), "Report Issue", "Badge still shown after opening Report Issue");
    }

    // ================= Moving around and Log out =================

    @Test(groups = {"auth", "edge"}, description = "Go from screen to screen")
    @TestCase(id = "ALD049", type = Type.EDGE,
            steps = "Open Leaderboard, then from its menu open Admissions, then Reports, without going home in between.",
            expected = "Each screen opens directly from the previous one.")
    public void screenToScreen() {
        openFromMenu("Leaderboard", "/staff/leaderboard");
        openMenu().clickItem("Admissions");
        assertTrue(screen.waitForUrl("filter=ADMISSIONS"), "Admissions did not open from Leaderboard");
        openMenu().clickItem("Reports");
        assertTrue(screen.waitForUrl("/staff/reports"), "Reports did not open from Admissions");
    }

    @Test(groups = {"auth", "positive"}, description = "Cancel then confirm Log out")
    @TestCase(id = "ALD050", type = Type.POSITIVE,
            steps = "Open the menu, click Log out and Cancel; then click Log out again and confirm. "
                    + "Then try to open the staff home address.",
            expected = "Cancel keeps the user logged in; confirming shows the login screen, and the staff home "
                    + "cannot be opened without logging in again.")
    public void logOut() {
        NavigationMenu menu = openMenu();
        menu.clickLogOutThenCancel();
        assertTrue(screen.isTextGone("need to sign in again"), "Log out dialog still shown after Cancel");
        assertTrue(screen.waitForUrl("/staff"), "User was logged out after Cancel");

        LoginPage login = openMenu().clickLogOut();
        assertTrue(login.isEmailFieldDisplayed(), "Login screen not shown after Log out");
        home.open();
        assertTrue(login.isEmailFieldDisplayed(), "Staff home opened again after logging out");
    }
}
