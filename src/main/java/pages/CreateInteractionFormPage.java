package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import utils.WaitUtils;

/**
 * "Create New Interaction" form, opened after Create Interaction
 * (https://team.aldun.com/staff/my-work/interaction/edit/?phoneNo=<number>).
 * Seen 2026-10-13: hint "For Onboarding Now, either Admission Id or UHID is required.",
 * fields Admission Id, UHID, Attender Phone No *, Patient Name *, Admission Date,
 * Source *, Status *, "Interested in ClaimIt (Pre & Post)" Yes/No, and a Create button.
 * Inputs are located by the label text shown above them.
 */
public class CreateInteractionFormPage extends BasePage {

    private static final Logger LOG = LogManager.getLogger(CreateInteractionFormPage.class);

    private final By title = By.xpath("//*[normalize-space(text())='Create New Interaction']");
    private final By admissionIdInput = inputUnderLabel("Admission Id");
    private final By uhidInput = inputUnderLabel("UHID");
    private final By patientNameInput =
            By.xpath("//*[starts-with(normalize-space(text()), 'Patient Name')]/following::input[1]");

    /** The form's submit button, text exactly "Create" (not the "Create New Interaction" title). */
    private final By createButton = By.xpath("//button[normalize-space(.)='Create']");

    /** Saving calls the server; allow up to this long for it to finish. */
    private static final int CREATE_TIMEOUT_SECONDS = 30;

    private static By inputUnderLabel(String label) {
        return By.xpath("//*[normalize-space(text())='" + label + "']/following::input[1]");
    }

    public CreateInteractionFormPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        try {
            return isDisplayed(title);
        } catch (TimeoutException e) {
            LOG.warn("'Create New Interaction' form not shown. URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    public CreateInteractionFormPage enterAdmissionId(String admissionId) {
        LOG.info("Entering Admission Id {}", admissionId);
        type(admissionIdInput, admissionId);
        return this;
    }

    public CreateInteractionFormPage enterUhid(String uhid) {
        LOG.info("Entering UHID {}", uhid);
        type(uhidInput, uhid);
        return this;
    }

    public CreateInteractionFormPage enterPatientName(String name) {
        LOG.info("Entering Patient Name {}", name);
        type(patientNameInput, name);
        return this;
    }

    public String getAdmissionId() {
        return valueOf(admissionIdInput);
    }

    public String getUhid() {
        return valueOf(uhidInput);
    }

    public String getPatientName() {
        return valueOf(patientNameInput);
    }

    public void clickCreate() {
        LOG.info("Clicking Create");
        click(createButton);
    }

    /**
     * Waits until the interaction is saved: the app leaves the Create New Interaction form
     * (URL no longer /interaction/edit, or the form title is gone) or shows a
     * "created"/"success" message. What the app shows on success is not known yet.
     */
    public boolean waitUntilCreated() {
        try {
            new WaitUtils(driver, CREATE_TIMEOUT_SECONDS).until(d -> {
                String text = d.findElement(By.tagName("body")).getText().toLowerCase();
                return !d.getCurrentUrl().contains("/interaction/edit")
                        || d.findElements(title).isEmpty()
                        || text.contains("created successfully")
                        || text.contains("success");
            });
            LOG.info("Interaction saved. Now at {}", driver.getCurrentUrl());
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Interaction not confirmed as created within {}s. URL: {}",
                    CREATE_TIMEOUT_SECONDS, driver.getCurrentUrl());
            return false;
        }
    }

    private String valueOf(By input) {
        return wait.waitForVisible(input).getDomProperty("value");
    }
}
