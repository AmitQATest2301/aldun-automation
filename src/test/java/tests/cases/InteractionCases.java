package tests.cases;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import base.IndependentTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.CreateInteractionFormPage;
import pages.DashboardPage;
import pages.InteractionPage;
import pages.InteractionsListPage;
import tests.TestData;

/**
 * ALD019-ALD026: New Interaction and Create New Interaction forms. Each test logs in in its
 * own browser. Negative checks never confirm "Create anyway", so they save nothing.
 * ALD026 creates a REAL interaction.
 */
public class InteractionCases extends IndependentTest {

    private InteractionPage newInteraction() {
        logIn();
        InteractionPage page = new InteractionPage(driver()).open();
        assertTrue(page.isLoaded(), "New Interaction screen did not open");
        return page;
    }

    private CreateInteractionFormPage patientForm() {
        logIn();
        CreateInteractionFormPage form = new CreateInteractionFormPage(driver()).open(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(form.isLoaded(), "'Create New Interaction' form did not open");
        return form;
    }

    @Test(groups = {"auth", "positive"}, description = "Open Log Interaction")
    @TestCase(id = "ALD019", type = Type.POSITIVE,
            steps = "Log in and click Log Interaction on the home screen.",
            expected = "The New Interaction screen opens with Attender Phone No.")
    public void logInteractionOpens() {
        DashboardPage home = logIn();
        String homeUrl = home.getCurrentUrl();
        home.clickLogInteraction();
        assertTrue(home.waitUntilLeftHomeScreen(homeUrl), "Nothing happened after clicking Log Interaction");
        assertTrue(new InteractionPage(driver()).isLoaded(), "New Interaction screen did not open");
    }

    @Test(groups = {"auth", "negative"}, description = "Empty phone number")
    @TestCase(id = "ALD020", type = Type.NEGATIVE,
            steps = "Log in, open New Interaction and leave Attender Phone No empty.",
            expected = "Create Interaction button is disabled, so you cannot continue.")
    public void emptyPhoneCannotContinue() {
        assertTrue(newInteraction().isCreateInteractionDisabled(), "Create Interaction is active with no phone number");
    }

    @Test(groups = {"auth", "negative"}, description = "Phone number too short")
    @TestCase(id = "ALD021", type = Type.NEGATIVE,
            steps = "Log in, open New Interaction, enter a 5-digit number (98114) and click Create Interaction.",
            expected = "The short number is not accepted and you cannot continue.")
    public void shortPhoneCannotContinue() {
        InteractionPage page = newInteraction().typeAttenderPhoneRaw(TestData.SHORT_PHONE_NUMBER);
        assertFalse(page.canContinue(), "Continued with a 5-digit phone number");
    }

    @Test(groups = {"auth", "negative"}, description = "Letters in phone number")
    @TestCase(id = "ALD022", type = Type.NEGATIVE,
            steps = "Log in, open New Interaction, type letters (abcdefghij) into Attender Phone No and click Create Interaction.",
            expected = "Letters are not accepted and you cannot continue.")
    public void lettersNotAccepted() {
        InteractionPage page = newInteraction().typeAttenderPhoneRaw(TestData.LETTERS_PHONE_NUMBER);
        boolean lettersKept = page.getAttenderPhoneNumber().matches(".*[a-zA-Z].*");
        assertFalse(lettersKept && page.canContinue(), "Continued with letters as the phone number");
    }

    @Test(groups = {"auth", "edge"}, description = "Phone number with 11 digits")
    @TestCase(id = "ALD023", type = Type.EDGE,
            steps = "Log in, open New Interaction, enter an 11-digit number (98114658401) and click Create Interaction.",
            expected = "Only a 10-digit number is accepted: the extra digit is blocked or you cannot continue.")
    public void elevenDigitsNotAccepted() {
        InteractionPage page = newInteraction().typeAttenderPhoneRaw(TestData.ELEVEN_DIGIT_PHONE_NUMBER);
        int digits = page.getAttenderPhoneNumber().replaceAll("\\D", "").length();
        assertFalse(digits > 10 && page.canContinue(), "Continued with an 11-digit phone number");
    }

    @Test(groups = {"auth", "negative"}, description = "Patient Name missing")
    @TestCase(id = "ALD024", type = Type.NEGATIVE,
            steps = "Log in, open Create New Interaction, fill Admission Id and UHID, leave Patient Name empty, click Create.",
            expected = "The interaction is not saved because Patient Name is required.")
    public void patientNameRequired() {
        CreateInteractionFormPage form = patientForm();
        form.enterAdmissionId(TestData.ADMISSION_ID).enterUhid(TestData.UHID).enterPatientName("");
        assertTrue(form.createIsBlocked(), "Interaction was saved without a Patient Name");
    }

    @Test(groups = {"auth", "negative"}, description = "Admission Id and UHID both missing")
    @TestCase(id = "ALD025", type = Type.NEGATIVE,
            steps = "Log in, open Create New Interaction, fill only Patient Name (no Admission Id, no UHID), click Create.",
            expected = "The interaction is not saved: Admission Id or UHID is required.")
    public void admissionIdOrUhidRequired() {
        CreateInteractionFormPage form = patientForm();
        form.enterAdmissionId("").enterUhid("").enterPatientName(TestData.PATIENT_NAME);
        assertTrue(form.createIsBlocked(), "Interaction was saved without Admission Id or UHID");
    }

    @Test(groups = {"auth", "positive"}, description = "Create an interaction")
    @TestCase(id = "ALD026", type = Type.POSITIVE,
            steps = "Log in, click Log Interaction, enter 9811465840, click Create Interaction, enter AD556142, "
                    + "UH558440 and Rahul Sharma, click Create (Create anyway if asked).",
            expected = "The interaction is saved and shown with Rahul Sharma and 9811465840.")
    public void createInteraction() {
        DashboardPage home = logIn();
        String homeUrl = home.getCurrentUrl();
        home.clickLogInteraction();
        assertTrue(home.waitUntilLeftHomeScreen(homeUrl), "Nothing happened after clicking Log Interaction");

        InteractionPage page = new InteractionPage(driver());
        assertTrue(page.isLoaded(), "New Interaction screen did not open");
        page.enterAttenderPhoneNumber(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(page.isNumberHistoryShown(), "'Aldun history for this number' is not shown");
        assertTrue(page.isCreateInteractionEnabled(), "Create Interaction button is not active");
        String formUrl = page.getCurrentUrl();
        page.clickCreateInteraction();
        page.confirmIfAsked();
        assertTrue(page.waitUntilSubmitted(formUrl), "Did not move on after Create Interaction");

        CreateInteractionFormPage form = new CreateInteractionFormPage(driver());
        assertTrue(form.isLoaded(), "'Create New Interaction' form is not shown");
        form.enterAdmissionId(TestData.ADMISSION_ID).enterUhid(TestData.UHID).enterPatientName(TestData.PATIENT_NAME);
        assertEquals(form.getPatientName(), TestData.PATIENT_NAME, "Patient Name is wrong");
        form.clickCreate();
        form.confirmIfAsked();
        assertTrue(form.waitUntilCreated(), "Interaction was not saved");
        assertTrue(new InteractionsListPage(driver())
                        .isInteractionShown(TestData.PATIENT_NAME, TestData.INTERACTION_CONTACT_NUMBER),
                "The saved interaction is not shown");
    }
}
