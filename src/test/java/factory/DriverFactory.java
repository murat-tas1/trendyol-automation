package factory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import utils.ConfigReader;

import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    // Stores the single WebDriver instance shared across the test framework.
    private static WebDriver driver;

    // Private constructor prevents creating DriverFactory objects.
    // This class is used through static methods only.
    private DriverFactory() {
    }

    // Creates and configures the browser based on the browser setting in config.properties.
    public static WebDriver createDriver() {
        String browser = ConfigReader.getBrowser();

        // Selects the browser type dynamically from configuration.
        switch (browser.toLowerCase()) {

            case "firefox":
                // Automatically downloads and configures the correct Firefox driver.
                WebDriverManager.firefoxdriver().setup();

                // Creates a Firefox browser instance.
                driver = new FirefoxDriver();
                break;

            case "chrome":
            default:
                // Automatically downloads and configures the correct Chrome driver.
                WebDriverManager.chromedriver().setup();

                // Creates Chrome with custom browser preferences.
                driver = new ChromeDriver(chromeOptionsWithNotificationsDisabled());
                break;
        }

        // Opens the browser in maximized mode for better element visibility.
        driver.manage().window().maximize();

        // Returns the created WebDriver instance to the caller.
        return driver;
    }

    // Creates Chrome settings used before starting the browser.
    // Currently disables notification popups that may interrupt automation.
    private static ChromeOptions chromeOptionsWithNotificationsDisabled() { // private function bcs it is helper function

        // Stores Chrome preferences as key-value pairs.
        Map<String, Object> prefs = new HashMap<>();

        // Blocks website notification permission popups.
        prefs.put("profile.default_content_setting_values.notifications", 2);

        // Creates a Chrome configuration object.
        ChromeOptions options = new ChromeOptions();

        // Applies the custom preferences to Chrome.
        options.setExperimentalOption("prefs", prefs);

        // Returns the configured Chrome options.
        return options;
    }

    // Provides access to the current WebDriver instance.
    public static WebDriver getDriver() {
        return driver;
    }

    // Closes the browser and cleans up the WebDriver instance after tests.
    public static void quitDriver() {
        if (driver != null) {

            // Ends the browser session and closes the browser.
            driver.quit();

            // Removes the old driver reference so a new session can be created later.
            driver = null;
        }
    }
}