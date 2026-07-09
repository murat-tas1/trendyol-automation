package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {

    private final By popupCloseButton = By.xpath("//div[contains(@class,'modal-section-close')]");
    private final By accountMenuLink = By.xpath("//a[@data-testid='navigation-menu-user']");
    private final By loggedInUserInfo = By.xpath("//p[@data-testid='user-info-title-line']");
    private final By homeLogoLink = By.xpath("//a[@data-testid='navigation-logo-component']");
    private final By categoryDiscountsBanner = By.xpath("//p[contains(text(),'Kategorilerdeki İndirimleri Keşfet')]");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void closePopupIfPresent() {
        if (isVisible(popupCloseButton)) {
            click(popupCloseButton);
        }
    }

    public LoginPage openLoginPage() {
        click(accountMenuLink);
        return new LoginPage(driver);
    }

    public void openAccountPanel() {
        click(accountMenuLink);
    }

    public String getLoggedInUserInfoText() {
        return getText(loggedInUserInfo);
    }

    public HomePage goToHomePage() {
        click(homeLogoLink);
        return new HomePage(driver);
    }

    public CategoryPage navigateToCategoryDiscounts() {
        scrollUntilVisible(categoryDiscountsBanner);
        click(categoryDiscountsBanner);
        return new CategoryPage(driver);
    }
}
