package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CartPage extends BasePage {

    // Closes the Trendyol Plus promotional popup that may block cart interactions.
    private final By promoPopupCloseButton =
            By.xpath("//button[contains(@class,'modal-close-button') and @aria-label='Close modal']");

    // Locator for the "Sepetim" (Cart) link in the page header.
    private final By sepetimLink = By.xpath("//a[@aria-label='Sepetim']");


    // Initializes the CartPage.
    public CartPage(WebDriver driver) {
        super(driver);
    }


    // Closes the promo popup if it appears after the cart page loads.
    public void closePromoPopup() {
        if (waitUtil.isVisibleWithin(promoPopupCloseButton, 5)) {
            click(promoPopupCloseButton);
            waitUtil.waitForInvisibility(promoPopupCloseButton);
        }
    }


    // Builds a locator for the product name in the cart.
    // Matches part of the full product title because the cart displays the title
    // across multiple elements.
    //to check whether we add this item to the cart.It is an helper function

    private By productTitleInCart(String productTitle) {
        return By.xpath("(//p[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))]"
                + " | //a[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))]"
                + " | //span[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))])[1]");
    }


    // Verifies whether the specified product is currently displayed in the cart.
    public boolean isProductInCart(String productTitle) {
        return isVisible(productTitleInCart(productTitle));
    }


    // Builds a locator for the "Sil" button belonging to the specified product.
    private By removeButtonForProduct(String productTitle) {
        return By.xpath("(//*[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))]"
                + "/ancestor::*[.//*[normalize-space(text())='Sil']][1]"
                + "//*[normalize-space(text())='Sil'])[1]");
    }


    // Removes the specified product from the cart.
    public void removeProduct(String productTitle) {
        click(removeButtonForProduct(productTitle));
    }


    // Reopens the cart page and waits until navigation is complete.
    public void reopenCart() {
        click(sepetimLink);
        waitUtil.waitForUrlContains("sepet");
    }


    // Waits until the specified product disappears from the cart.
    public boolean waitUntilProductRemoved(String productTitle) {
        return waitUtil.waitForInvisibility(productTitleInCart(productTitle));
    }
}