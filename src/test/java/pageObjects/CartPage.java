package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CartPage extends BasePage {

    // "trendyol plus - SADECE 1 TL" promo modal that pops up on the cart page and
    // covers the Sil (remove) button; the "x" in its top-right corner closes it
    private final By promoPopupCloseButton =
            By.xpath("//button[contains(@class,'modal-close-button') and @aria-label='Close modal']");
    private final By sepetimLink = By.xpath("//a[@aria-label='Sepetim']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Closes the "trendyol plus" promo modal that can block the cart page. It shows
     * up a moment after the cart loads and does NOT appear on every visit, so we
     * wait a short bounded time for it and close it only if it actually appears.
     */
    public void closePromoPopup() {
        if (waitUtil.isVisibleWithin(promoPopupCloseButton, 5)) {
            click(promoPopupCloseButton);
            waitUtil.waitForInvisibility(promoPopupCloseButton);
        }
    }

    private By productTitleInCart(String productTitle) {
        // In the cart the brand ("Skechers") and the product name live in separate
        // elements, so no single element holds the full h1 title. Instead we look
        // for a name element whose own text is PART of the full title. The length
        // guard skips short stray words (e.g. the colour on its own) so we only
        // match the real product-name line - which also keeps us from matching a
        // different product that happens to share the model number.
        return By.xpath("(//p[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))]"
                + " | //a[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))]"
                + " | //span[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))])[1]");
    }

    public boolean isProductInCart(String productTitle) {
        return isVisible(productTitleInCart(productTitle));
    }

    private By removeButtonForProduct(String productTitle) {
        // The cart holds several products, so we must click the "Sil" that belongs
        // to OUR product's row: start from our product-name element, climb to the
        // nearest ancestor row that also contains a "Sil", then take that Sil.
        return By.xpath("(//*[string-length(normalize-space(.)) > 15 and contains(\"" + productTitle + "\", normalize-space(.))]"
                + "/ancestor::*[.//*[normalize-space(text())='Sil']][1]"
                + "//*[normalize-space(text())='Sil'])[1]");
    }

    public void removeProduct(String productTitle) {
        click(removeButtonForProduct(productTitle));
    }

    /**
     * Re-opens the cart from the header ("Sepetim") so we verify removal against a
     * freshly loaded cart, the way a real user would double-check.
     */
    public void reopenCart() {
        click(sepetimLink);
        waitUtil.waitForUrlContains("sepet");
    }

    public boolean waitUntilProductRemoved(String productTitle) {
        return waitUtil.waitForInvisibility(productTitleInCart(productTitle));
    }
}
