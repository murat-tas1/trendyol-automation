package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private final By emailInput = By.id("login-email");
    private final By continueButton = By.xpath("//button[@data-testid='email-check-button']");
    private final By passwordInput = By.id("login-password");
    private final By loginButton = By.xpath("//button[@data-testid='login-button']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String email) {
        type(emailInput, email);
    }

    public void clickContinue() {
        click(continueButton);
    }

    public void enterPassword(String password) {
        type(passwordInput, password);
    }

    public void clickLoginButton() {
        click(loginButton);
    }

    public HomePage login(String email, String password) {
        enterEmail(email);
        clickContinue();
        enterPassword(password);
        clickLoginButton();
        return new HomePage(driver);
    }
}
