package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {


    // Locator for the close button of the popup/modal shown on the homepage.
    private final By popupCloseButton =
            By.xpath("//div[contains(@class,'modal-section-close')]");


    // Locator for the account menu button used to open login/account options.
    private final By accountMenuLink =
            By.xpath("//a[@data-testid='navigation-menu-user']");


    // Locator for the logged-in user's information displayed on the homepage.
    private final By loggedInUserInfo =
            By.xpath("//p[@data-testid='user-info-title-line']");


    // Locator for the Trendyol logo which navigates back to the homepage.
    private final By homeLogoLink =
            By.xpath("//a[@data-testid='navigation-logo-component']");


    // Locator for the category discounts banner used to navigate to discount categories.
    private final By categoryDiscountsBanner =
            By.xpath("//p[contains(text(),'Kategorilerdeki İndirimleri Keşfet')]");


    // super(driver): runs BasePage's constructor so driver/waitUtil get initialized here too.
    public HomePage(WebDriver driver) {
        super(driver);
    }


    // Closes the homepage popup if it is currently visible.
    // The popup is optional, so the test continues if it does not exist.
    public void closePopupIfPresent() {
        if (isVisible(popupCloseButton)) {
            click(popupCloseButton);
        }
    }


    // Opens the login page from the account menu.
    // Returns a LoginPage object to continue the login flow.
    public LoginPage openLoginPage() {
        click(accountMenuLink);
        return new LoginPage(driver);
    }


    // Opens the account panel from the homepage.
    public void openAccountPanel() {
        click(accountMenuLink);
    }


    // Retrieves the visible text containing logged-in user information.
    // Used for validating that login was successful.
    public String getLoggedInUserInfoText() {
        return getText(loggedInUserInfo);
    }


    // Navigates back to the homepage by clicking the Trendyol logo.
    // Returns a new HomePage object representing the current page.
    public HomePage goToHomePage() {
        click(homeLogoLink);
        return new HomePage(driver);
    }


    // Navigates to the category discounts page.
    // Scrolls until the banner is visible, clicks it, and returns the CategoryPage object.
    public CategoryPage navigateToCategoryDiscounts() {
        scrollUntilVisible(categoryDiscountsBanner);
        click(categoryDiscountsBanner);
        return new CategoryPage(driver);
    }
}