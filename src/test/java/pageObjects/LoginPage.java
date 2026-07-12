package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    // Locator for the email input field.
    private final By emailInput = By.id("login-email");

    // Locator for the button that proceeds to the password step.
    private final By continueButton = By.xpath("//button[@data-testid='email-check-button']");

    // Locator for the password input field.
    private final By passwordInput = By.id("login-password");

    // Locator for the login button.
    private final By loginButton = By.xpath("//button[@data-testid='login-button']");


    // Initializes the LoginPage with the active WebDriver.
    public LoginPage(WebDriver driver) {
        super(driver);
    }


    // Enters the user's email address.
    public void enterEmail(String email) {
        type(emailInput, email);
    }


    // Proceeds from the email step to the password step.
    public void clickContinue() {
        click(continueButton);
    }


    // Enters the user's password.
    public void enterPassword(String password) {
        type(passwordInput, password);
    }


    // Clicks the login button to submit the credentials.
    public void clickLoginButton() {
        click(loginButton);
    }


    // Performs the complete login process and returns the HomePage.
    public HomePage login(String email, String password) {
        enterEmail(email);
        clickContinue();
        enterPassword(password);
        clickLoginButton();

        // After a successful login, the user is redirected to the homepage.
        return new HomePage(driver);
    }
}