package tests;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import base.SessionTest;
import listeners.TestCase;
import listeners.TestCase.Type;
import pages.CreateInteractionFormPage;
import pages.InteractionPage;

/**
 * Checks on the New Interaction and Create New Interaction forms. Logs in once.
 * Negative checks never confirm "Create anyway", so they do not save anything.
 * The one Edge check that is expected to save creates a REAL interaction.
 */
public class InteractionValidationTest extends SessionTest {

    @BeforeClass(alwaysRun = true)
    public void logIn() {
        logInAsStaff();
    }

    private InteractionPage freshNewInteraction() {
        InteractionPage page = new InteractionPage(driver).open();
        assertTrue(page.isLoaded(), "New Interaction screen did not open");
        return page;
    }

    private CreateInteractionFormPage freshPatientForm() {
        CreateInteractionFormPage form = new CreateInteractionFormPage(driver).open(TestData.INTERACTION_CONTACT_NUMBER);
        assertTrue(form.isLoaded(), "'Create New Interaction' form did not open");
        return form;
    }

    // ---------------- New Interaction: Attender Phone No ----------------

    @Test(priority = 1, groups = {"auth", "negative"}, description = "Empty phone number")
    @TestCase(type = Type.NEGATIVE,
            steps = "Open New Interaction and leave Attender Phone No empty.",
            expected = "Create Interaction button is disabled, so you cannot continue.")
    public void emptyPhoneCannotContinue() {
        InteractionPage page = freshNewInteraction();
        assertTrue(page.isCreateInteractionDisabled(), "Create Interaction is active with no phone number");
    }

    @Test(priority = 2, groups = {"auth", "negative"}, description = "Phone number too short")
    @TestCase(type = Type.NEGATIVE,
            steps = "Open New Interaction, enter a 5-digit phone number (98114) and click Create Interaction.",
            expected = "The short number is not accepted and you cannot continue.")
    public void shortPhoneCannotContinue() {
        InteractionPage page = freshNewInteraction().typeAttenderPhoneRaw(TestData.SHORT_PHONE_NUMBER);
        assertFalse(page.canContinue(), "Continued with a 5-digit phone number");
    }

    @Test(priority = 3, groups = {"auth", "negative"}, description = "Letters in phone number")
    @TestCase(type = Type.NEGATIVE,
            steps = "Open New Interaction, type letters (abcdefghij) into Attender Phone No and click Create Interaction.",
            expected = "Letters are not accepted and you cannot continue.")
    public void lettersInPhoneNotAccepted() {
        InteractionPage page = freshNewInteraction().typeAttenderPhoneRaw(TestData.LETTERS_PHONE_NUMBER);
        boolean lettersKept = page.getAttenderPhoneNumber().matches(".*[a-zA-Z].*");
        assertFalse(lettersKept && page.canContinue(), "Continued with letters as the phone number");
    }

    @Test(priority = 4, groups = {"auth", "edge"}, description = "Phone number with 11 digits")
    @TestCase(type = Type.EDGE,
            steps = "Open New Interaction, enter an 11-digit number (98114658401) and click Create Interaction.",
            expected = "Only a 10-digit number is accepted: the extra digit is blocked or you cannot continue.")
    public void elevenDigitPhoneNotAccepted() {
        InteractionPage page = freshNewInteraction().typeAttenderPhoneRaw(TestData.ELEVEN_DIGIT_PHONE_NUMBER);
        int digits = page.getAttenderPhoneNumber().replaceAll("\\D", "").length();
        assertFalse(digits > 10 && page.canContinue(), "Continued with an 11-digit phone number");
    }

    // ---------------- Create New Interaction form ----------------

    @Test(priority = 5, groups = {"auth", "negative"}, description = "Patient Name missing")
    @TestCase(type = Type.NEGATIVE,
            steps = "On Create New Interaction, fill Admission Id and UHID, leave Patient Name empty and click Create.",
            expected = "The interaction is not saved because Patient Name is required.")
    public void patientNameRequired() {
        CreateInteractionFormPage form = freshPatientForm();
        form.enterAdmissionId(TestData.ADMISSION_ID).enterUhid(TestData.UHID).enterPatientName("");
        assertTrue(form.createIsBlocked(), "Interaction was saved without a Patient Name");
    }

    @Test(priority = 6, groups = {"auth", "negative"}, description = "Admission Id and UHID both missing")
    @TestCase(type = Type.NEGATIVE,
            steps = "On Create New Interaction, fill only Patient Name (no Admission Id, no UHID) and click Create.",
            expected = "The interaction is not saved: for Onboarding Now, Admission Id or UHID is required.")
    public void admissionIdOrUhidRequired() {
        CreateInteractionFormPage form = freshPatientForm();
        form.enterAdmissionId("").enterUhid("").enterPatientName(TestData.PATIENT_NAME);
        assertTrue(form.createIsBlocked(), "Interaction was saved without Admission Id or UHID");
    }

    @Test(priority = 7, groups = {"auth", "edge"}, description = "UHID only, no Admission Id")
    @TestCase(type = Type.EDGE,
            steps = "On Create New Interaction, fill UHID and Patient Name but no Admission Id. Click Create "
                    + "(and Create anyway if asked).",
            expected = "The interaction is saved, because UHID alone is enough.")
    public void uhidAloneIsEnough() {
        CreateInteractionFormPage form = freshPatientForm();
        form.enterAdmissionId("").enterUhid(TestData.UHID).enterPatientName(TestData.PATIENT_NAME);
        form.clickCreate();
        form.confirmIfAsked();
        assertTrue(form.waitUntilCreated(), "Interaction with UHID only was not saved");
    }
}
