package pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

/**
 * "Interactions" list screen (seen 2026-10-13 after Check in): title "Interactions",
 * search box "Phone, Adm ID, UHID or Name", "Created by me" filter, interaction cards,
 * and a "New interaction" button at the bottom.
 */
public class InteractionsListPage extends BasePage {

    private static final Logger LOG = LogManager.getLogger(InteractionsListPage.class);

    // Matched on the element that holds the text itself (case-insensitive), the same way
    // as the working "Check in" locator; //button[...] did not match this page's button.
    private final By newInteractionButton = By.xpath("//*[translate(normalize-space(text()), "
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='new interaction']");

    public InteractionsListPage(WebDriver driver) {
        super(driver);
    }

    /** Recognised by its content: the "Created by me" filter and the "New interaction" button. */
    public boolean isLoaded() {
        try {
            wait.waitForPageText("Created by me");
            wait.waitForVisible(newInteractionButton);
            LOG.info("Interactions list shown at {}", driver.getCurrentUrl());
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Interactions list not shown. URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    /**
     * True when an interaction showing all the given texts (e.g. patient name and phone)
     * is visible on the current screen. Waits up to the explicit wait.
     */
    public boolean isInteractionShown(String... texts) {
        try {
            for (String text : texts) {
                wait.waitForPageText(text);
            }
            LOG.info("Created interaction is shown on screen");
            return true;
        } catch (TimeoutException e) {
            LOG.warn("Created interaction not shown on screen. URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    /** Opens the New Interaction form. */
    public InteractionPage clickNewInteraction() {
        LOG.info("Clicking New interaction");
        click(newInteractionButton);
        return new InteractionPage(driver);
    }
}
