package hooks;

import factory.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;

public class Hooks {


    // Runs automatically before each Cucumber scenario.
    // Creates and initializes the WebDriver before the test starts.
    @Before
    public void setUp() {
        DriverFactory.createDriver();
    }


    // Runs automatically after each Cucumber scenario.
    // Closes the browser and cleans up the WebDriver session.
    @After
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}