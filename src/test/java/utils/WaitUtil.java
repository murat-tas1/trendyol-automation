package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class WaitUtil {

    // Stores the browser instance used for all waiting operations.
    private final WebDriver driver;

    // Provides Selenium explicit wait functionality.
    private final WebDriverWait wait;


    // Constructor receives the active browser and initializes the explicit wait.
    public WaitUtil(WebDriver driver) {
        this.driver = driver;

        // Uses the wait duration defined in config.properties.
        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(ConfigReader.getExplicitWaitSeconds())
        );
    }


    // Waits for a required element to become visible.
    // If the element does not appear, the test fails because this element is necessary.
    public WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }


    // Checks whether an optional element appears within a specific time.
    // If it does not appear, the test continues instead of failing.
    public boolean isVisibleWithin(By locator, int seconds) {

        try {
            // Creates a temporary wait with a custom timeout.
            // Used for elements that may or may not appear, such as popups.
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));

            // Element appeared successfully.
            return true;

        } catch (TimeoutException elementNeverAppeared) {

            // Optional element did not appear within the given time.
            return false;
        }
    }


    // Waits until an element is visible and enabled for clicking.
    public WebElement waitForClickability(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }


    // Attempts to click an element and retries if the page is temporarily unstable.
    public void clickWhenReady(By locator) {

        // Selenium repeatedly executes this function until it returns true.
        wait.until(webDriver -> {

            try {
                // Finds the element again and attempts the click.
                webDriver.findElement(locator).click();

                // Click succeeded, stop waiting.
                return true;

            } catch (ElementNotInteractableException
                     | StaleElementReferenceException
                     | NoSuchElementException e) {

                // Element is temporarily unavailable.
                // Returning false makes Selenium retry.
                return false;
            }
        });
    }


    // Scrolls down until the target element becomes visible.
    public void scrollUntilVisible(By locator) {

        // Selenium keeps checking until the element is found and visible.
        wait.until(webDriver -> {

            List<WebElement> elements = webDriver.findElements(locator);

            if (!elements.isEmpty() && elements.get(0).isDisplayed()) {

                // Element found and visible, stop waiting.
                return elements.get(0);
            }

            // Scroll down if the element is not visible yet.
            ((JavascriptExecutor) webDriver)
                    .executeScript("window.scrollBy(0, 600);");


            // Returning null tells Selenium to continue waiting.
            return null;
        });
    }


    // Waits until a checkbox or radio button becomes selected.
    public void waitUntilSelected(By locator) {

        wait.until(webDriver -> {

            try {
                // Checks whether the element is selected.
                return webDriver.findElement(locator).isSelected();

            } catch (StaleElementReferenceException
                     | NoSuchElementException e) {

                // Element changed or is not ready yet, retry.
                return false;
            }
        });
    }


    // Waits until the browser URL contains the expected text after navigation.
    public void waitForUrlContains(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }


    // Waits until the expected number of browser tabs/windows are open.
    public void waitForWindowCount(int expectedCount) {

        // Repeatedly checks if the number of open windows is enough.
        wait.until(webDriver ->
                webDriver.getWindowHandles().size() >= expectedCount
        );
    }


    // Waits until all elements matching the locator are visible.
    // Useful for lists such as product cards.
    public List<WebElement> waitForAllVisible(By locator) {
        return wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(locator)
        );
    }


    // Waits until an element disappears or becomes hidden.
    // Useful after closing popups or removing cart items.
    public boolean waitForInvisibility(By locator) {
        return wait.until(
                ExpectedConditions.invisibilityOfElementLocated(locator)
        );
    }
}