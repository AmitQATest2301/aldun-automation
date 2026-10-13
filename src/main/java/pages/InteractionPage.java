package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

/**
 * "New Interaction" screen (https://team.aldun.com/staff/my-work/interaction/new),
 * opened from Log Interaction on the staff home screen.
 * Seen 2026-10-13: title "New Interaction", one field "Attender Phone No *",
 * and a "Create Interaction" button (disabled until the field is filled).
 */
public class InteractionPage extends BasePage {

    private static final Logger LOG = LogManager.getLogger(InteractionPage.class);

    private final By title = By.xpath("//*[normalize-space(text())='New Interaction']");
    private final By attenderPhoneInput =
            By.xpath("//*[contains(normalize-space(text()), 'Attender Phone No')]/following::input[1]");
    /** Full XPath supplied by the user (2026-10-13). Brittle if the layout changes. */
    private static final String CREATE_BUTTON_FULL_XPATH = "//*[@id=\"root\"]/div/div/div[1]/div[2]/div/div/div/div[2]"
            + "/div/div[2]/div[1]/div/div[2]/div/div/div[2]/div[2]/div/div/div/div[3]/div/button/div";
    /** Fallback by the button's visible text, used if the full XPath no longer matches. */
    private static final String CREATE_BUTTON_BY_TEXT = "//*[normalize-space(text())='Create Interaction']";
    private final By createInteractionButton =
            By.xpath("(" + CREATE_BUTTON_FULL_XPATH + " | " + CREATE_BUTTON_BY_TEXT + ")[1]");
    /** Panel that loads after a number is entered (seen 2026-10-13). */
    private final By numberHistoryPanel =
            By.xpath("//*[contains(normalize-space(text()), 'Aldun history for this number')]");

    public InteractionPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        try {
            return isDisplayed(title) && isDisplayed(attenderPhoneInput);
        } catch (TimeoutException e) {
            LOG.warn("New Interaction screen not shown. URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    public InteractionPage enterAttenderPhoneNumber(String number) {
        LOG.info("Entering Attender Phone No {}", number);
        type(attenderPhoneInput, number);
        return this;
    }

    public String getAttenderPhoneNumber() {
        return wait.waitForVisible(attenderPhoneInput).getDomProperty("value");
    }

    /**
     * Waits for the "Aldun history for this number" panel that appears after a number
     * is entered (shows e.g. "New - No Interaction found", Admissions, Interactions).
     */
    public boolean isNumberHistoryShown() {
        try {
            return isDisplayed(numberHistoryPanel);
        } catch (TimeoutException e) {
            LOG.warn("'Aldun history for this number' panel did not appear");
            return false;
        }
    }

    /** Waits until Create Interaction becomes clickable (it is disabled while the field is empty). */
    public boolean isCreateInteractionEnabled() {
        try {
            return wait.waitForClickable(createInteractionButton) != null;
        } catch (TimeoutException e) {
            LOG.warn("'Create Interaction' did not become clickable");
            return false;
        }
    }

    public void clickCreateInteraction() {
        LOG.info("Clicking Create Interaction");
        click(createInteractionButton);
    }

    /**
     * Waits until the form moves on after Create Interaction: the URL leaves
     * /interaction/new or the "Attender Phone No" field is gone.
     * What the next screen contains is not known yet.
     */
    public boolean waitUntilSubmitted(String formUrl) {
        try {
            wait.until(d -> !d.getCurrentUrl().equals(formUrl)
                    || d.findElements(attenderPhoneInput).isEmpty());
            LOG.info("After Create Interaction, URL: {}", driver.getCurrentUrl());
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Still on the New Interaction form after clicking Create Interaction");
            return false;
        }
    }
}
