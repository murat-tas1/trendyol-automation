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

    protected final WebDriver driver;
    protected final WaitUtil waitUtil;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.waitUtil = new WaitUtil(driver);
    }

    protected void click(By locator) {
        waitUtil.clickWhenReady(locator);
    }

    protected void type(By locator, String text) {
        WebElement element = waitUtil.waitForVisibility(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return waitUtil.waitForVisibility(locator).getText();
    }

    protected boolean isVisible(By locator) {
        try {
            return waitUtil.waitForVisibility(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Immediate (non-blocking) check for whether an element is currently rendered.
     * Unlike {@link #isVisible} this does not wait, so it is safe to poll for
     * transient overlays without paying the full explicit-wait timeout when absent.
     */
    protected boolean isDisplayedNow(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    /**
     * Dismisses a dimming overlay the way a real user would: if it is on screen,
     * click it to close, then wait until it is gone before continuing.
     */
    protected void dismissOverlayIfPresent(By overlayLocator) {
        if (isDisplayedNow(overlayLocator)) {
            click(overlayLocator);
            waitUtil.waitForInvisibility(overlayLocator);
        }
    }

    protected void scrollToElement(By locator) {
        WebElement element = waitUtil.waitForVisibility(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    protected void scrollUntilVisible(By locator) {
        waitUtil.scrollUntilVisible(locator);
    }

    protected void jsClick(By locator) {
        WebElement element = waitUtil.waitForVisibility(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void clickWithHoverRecovery(By locator) {
        try {
            waitUtil.clickWhenReady(locator);
            return;
        } catch (TimeoutException firstAttemptFailed) {
            // still blocked after retrying; a hover-triggered overlay may be persistently covering the element
        }

        new Actions(driver)
                .moveToElement(driver.findElement(By.tagName("body")), 0, 0)
                .perform();

        try {
            waitUtil.clickWhenReady(locator);
        } catch (TimeoutException secondAttemptFailed) {
            jsClick(locator);
        }
    }

    protected void switchToNewTab() {
        String originalHandle = driver.getWindowHandle();
        waitUtil.waitForWindowCount(2);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(originalHandle)) {
                driver.switchTo().window(handle);
                break;
            }
        }
    }
}
