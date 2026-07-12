package hooks;

import factory.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Hooks {

    // Root folder where evidence for failed scenarios is collected.
    // The AI failure-analysis PoC reads this folder to explain each failure.
    private static final Path FAILURE_DIR = Paths.get("target", "failure-artifacts");

    // Runs automatically before each Cucumber scenario.
    // Creates and initializes the WebDriver before the test starts.
    @Before
    public void setUp() {
        DriverFactory.createDriver();
    }

    // Runs automatically after each Cucumber scenario.
    // If the scenario failed, collects evidence (screenshot + page source) BEFORE
    // the browser is closed, so the AI has real data to analyze. Then quits the driver.
    @After
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed()) {
            collectFailureArtifacts(scenario);
        }
        DriverFactory.quitDriver();
    }

    // Saves a screenshot and the page HTML into a per-scenario folder.
    // Never throws: artifact collection must not hide the original test failure.
    private void collectFailureArtifacts(Scenario scenario) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver == null) {
                return;
            }

            // A safe, unique folder name built from the scenario name.
            String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
            Path folder = FAILURE_DIR.resolve(safeName);
            Files.createDirectories(folder);

            // Screenshot: what the page looked like at the moment of failure.
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(folder.resolve("screenshot.png"), screenshot);

            // Page source: the HTML the test was looking at (useful for locator issues).
            Files.write(folder.resolve("page-source.html"),
                    driver.getPageSource().getBytes());

            System.out.println("[failure-artifacts] saved evidence to " + folder.toAbsolutePath());
        } catch (IOException | RuntimeException artifactCaptureFailed) {
            // Log and move on; the test result itself is what matters.
            System.out.println("[failure-artifacts] could not save evidence: "
                    + artifactCaptureFailed.getMessage());
        }
    }
}
