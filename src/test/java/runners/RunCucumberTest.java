package runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;


// Entry point for running Cucumber tests with JUnit.
@RunWith(Cucumber.class)


// Cucumber configuration settings. 
@CucumberOptions(

        // Location of the feature files containing Gherkin scenarios.
        features = "src/test/resources/features",

        // Packages where Cucumber searches for step definitions and hooks.
        glue = {"stepDefinitions", "hooks"},

        // Defines test output format.
        // "pretty" and "summary" print to the console; "json" writes a structured
        // machine-readable report (including failure stack traces) that the AI
        // failure-analysis PoC reads to explain why a scenario failed.
        plugin = {"pretty", "summary", "json:target/cucumber-report.json"},

        // Makes console output easier to read by removing unnecessary characters.
        monochrome = true
)
public class RunCucumberTest {
}