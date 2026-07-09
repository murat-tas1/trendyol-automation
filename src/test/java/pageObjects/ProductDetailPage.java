package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProductDetailPage extends BasePage {

    private final By productTitle = By.tagName("h1");
    private final By addToCartButton = By.cssSelector("button[data-testid='add-to-cart-button']");
    private final By headerCartLink = By.cssSelector("a[href*='/sepet']");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    public String getProductTitle() {
        return getText(productTitle);
    }

    public void addToCart() {
        click(addToCartButton);
    }

    public CartPage goToCart() {
        click(headerCartLink);
        waitUtil.waitForUrlContains("sepet");
        CartPage cartPage = new CartPage(driver);
        cartPage.closePromoPopup();
        return cartPage;
    }
}
