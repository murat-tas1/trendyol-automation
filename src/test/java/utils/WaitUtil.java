package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
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

    private final WebDriver driver;
    private final WebDriverWait wait;

    public WaitUtil(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getExplicitWaitSeconds()));
    }

    public WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Bounded check for something that may or may not appear (e.g. an optional
     * popup): waits up to {@code seconds} for the element to become visible and
     * returns whether it did, instead of throwing when it never shows up.
     */
    public boolean isVisibleWithin(By locator, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException elementNeverAppeared) {
            return false;
        }
    }

    public WebElement waitForClickability(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void clickWhenReady(By locator) {
        wait.until(webDriver -> {
            try {
                webDriver.findElement(locator).click();
                return true;
            } catch (ElementClickInterceptedException | StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });
    }

    public WebElement scrollUntilVisible(By locator) {
        return wait.until(webDriver -> {
            List<WebElement> elements = webDriver.findElements(locator);
            if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
                return elements.get(0);
            }
            ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, 600);");
            return null;
        });
    }

    public void waitUntilSelected(By locator) {
        wait.until(webDriver -> {
            try {
                return webDriver.findElement(locator).isSelected();
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });
    }

    public void waitForUrlContains(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    public void waitForWindowCount(int expectedCount) {
        wait.until(webDriver -> webDriver.getWindowHandles().size() >= expectedCount);
    }

    public List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public boolean waitForInvisibility(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }
}
