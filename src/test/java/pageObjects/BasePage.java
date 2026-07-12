package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import utils.WaitUtil;

import java.util.List;

public class BasePage {

    // Shared browser instance used by all page objects that extend BasePage.
    protected final WebDriver driver;

    // Provides reusable explicit wait methods for page interactions.
    protected final WaitUtil waitUtil;


    // Initializes the page object with the active WebDriver.
    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.waitUtil = new WaitUtil(driver);
    }


    // Clicks an element using the framework's reliable click mechanism.
    // Waits until the element is ready before clicking.
    protected void click(By locator) {
        waitUtil.clickWhenReady(locator);
    }


    // Types text into an input field.
    // Waits until the input is visible, clears existing text, then enters new text.
    protected void type(By locator, String text) {
        WebElement element = waitUtil.waitForVisibility(locator);
        element.clear();
        element.sendKeys(text);
    }


    // Retrieves the visible text of an element.
    protected String getText(By locator) {
        return waitUtil.waitForVisibility(locator).getText();
    }


    // Checks whether an element is visible.
    // Returns false instead of failing if the element cannot be found.
    // returns false instead of failing so that caller can decide how to handle missing elements.
    protected boolean isVisible(By locator) {
        try {
            return waitUtil.waitForVisibility(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Performs an immediate visibility check without waiting.
     * Used for elements like temporary overlays where waiting is unnecessary.
     */
    protected boolean isDisplayedNow(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }


    /**
     * Closes an overlay if it is currently visible.
     * Clicks the overlay and waits until it disappears before continuing.
     */
    protected void dismissOverlayIfPresent(By overlayLocator) {
        if (isDisplayedNow(overlayLocator)) {
            click(overlayLocator);

            //Why don't we continue immediately?
            //Because clicking doesn't instantly remove the popup.

            waitUtil.waitForInvisibility(overlayLocator);
        }
    }


    // Scrolls directly to an element and places it in the center of the screen.
    protected void scrollToElement(By locator) {
        WebElement element = waitUtil.waitForVisibility(locator);

        //arguments[0] means: "Use the first object I passed from Java."
        //
        ((JavascriptExecutor) driver)
                .executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});",
                        element
                );
    }


    // Scrolls gradually until the target element becomes visible.
    protected void scrollUntilVisible(By locator) {
        waitUtil.scrollUntilVisible(locator);
    }


    // Uses JavaScript to click an element.
    // Used as a fallback when normal Selenium clicking fails.
    protected void jsClick(By locator) {
        WebElement element = waitUtil.waitForVisibility(locator);

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", element);
    }


    // Attempts a normal click first.
    // If an overlay or hover issue blocks the click, moves the mouse away and retries.
    // If the normal click still fails, uses JavaScript click as the final fallback.
    protected void clickWithHoverRecovery(By locator) {

        try {
            waitUtil.clickWhenReady(locator);
            return;

        } catch (TimeoutException firstAttemptFailed) {
            // Element is still blocked after retrying.
            // A hover-triggered overlay may be preventing interaction.
        }


        // Moves the mouse away from the element to remove possible hover effects.
        new Actions(driver)
                .moveToElement(driver.findElement(By.tagName("body")), 0, 0)
                .perform();


        try {
            // Retry normal Selenium click after removing hover interference.
            waitUtil.clickWhenReady(locator);

        } catch (TimeoutException secondAttemptFailed) {

            // Final fallback when Selenium click cannot interact with the element.
            jsClick(locator);
        }
    }


    // Switches from the current browser tab to the newly opened tab.
    protected void switchToNewTab() {

        // Stores the current tab before opening a new one.
        String originalHandle = driver.getWindowHandle();

        // Waits until the second tab/window exists.
        waitUtil.waitForWindowCount(2);


        // Finds the new tab and switches the driver context to it.
        for (String handle : driver.getWindowHandles()) {

            if (!handle.equals(originalHandle)) {
                driver.switchTo().window(handle);
                break;
            }
        }
    }
}