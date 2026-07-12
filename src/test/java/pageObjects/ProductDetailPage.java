package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProductDetailPage extends BasePage {

    // Locator for the product title.
    private final By productTitle = By.tagName("h1");

    // Locator for the "Add to Cart" button.
    private final By addToCartButton = By.cssSelector("button[data-testid='add-to-cart-button']");

    // Locator for the cart icon in the page header.
    private final By headerCartLink = By.cssSelector("a[href*='/sepet']");


    // Initializes the ProductDetailPage.
    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }


    // Returns the product title.
    public String getProductTitle() {
        return getText(productTitle);
    }


    // Adds the current product to the cart.
    public void addToCart() {
        click(addToCartButton);
    }


    // Navigates to the shopping cart.
    // Waits for the cart page to load and closes the promo popup if it appears.
    public CartPage goToCart() {
        click(headerCartLink);
        waitUtil.waitForUrlContains("sepet");

        CartPage cartPage = new CartPage(driver);
        cartPage.closePromoPopup();

        return cartPage;
    }
}