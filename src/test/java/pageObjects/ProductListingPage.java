package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class ProductListingPage extends BasePage {

    // HTML IDs of the filter checkboxes.
    private static final String SPOR_AYAKKABI_CHECKBOX_ID = "checkbox-web-aggregations-checkbox-WebCategory-109-109";
    private static final String ERKEK_CHECKBOX_ID = "checkbox-web-aggregations-checkbox-WebGender-2-2";
    private static final String SKECHERS_CHECKBOX_ID = "checkbox-web-aggregations-checkbox-WebBrand-658-658";

    // Locator for the brand search input.
    private final By brandSearchInput = By.xpath("//input[contains(@placeholder,'Marka')]");

    // Locator for the onboarding overlay that may block interactions.
    private final By onboardingOverlay = By.cssSelector("div[data-testid='onboarding-overlay']");

    // Locator for the second product with the "10 Günün En Düşük Fiyatı" badge.
    // Uses the badge's data-testid to avoid matching similar text on other products.
    private final By secondLowestPriceBadgeProductCard = By.xpath(
            "(//a[@data-testid='product-card'][.//*[@data-testid='lowest-price-duration-badge']])[2]");

    // Initializes the ProductListingPage.
    public ProductListingPage(WebDriver driver) {
        super(driver);
    }

    // Applies the "Spor Ayakkabı" filter.
    public void filterBySporAyakkabiCategory() {
        applyCheckboxFilter(SPOR_AYAKKABI_CHECKBOX_ID);
    }

    // Applies the "Erkek" filter.
    public void filterByErkekGender() {
        applyCheckboxFilter(ERKEK_CHECKBOX_ID);
    }

    // Searches for and applies the Skechers brand filter.
    public void filterBySkechersBrand() {
        dismissOnboardingOverlayIfPresent();
        type(brandSearchInput, "Skechers");
        applyCheckboxFilter(SKECHERS_CHECKBOX_ID);
    }

    // Opens the second product with the lowest-price badge.
    public ProductDetailPage selectSecondLowestPriceBadgeProduct() {
        dismissOnboardingOverlayIfPresent();
        scrollUntilVisible(secondLowestPriceBadgeProductCard);
        scrollToElement(secondLowestPriceBadgeProductCard);
        dismissOnboardingOverlayIfPresent();
        clickWithHoverRecovery(secondLowestPriceBadgeProductCard);
        switchToNewTab();
        return new ProductDetailPage(driver);
    }

    // Applies a checkbox filter using its HTML ID.
    // Falls back to JavaScript click if the normal click fails.
    private void applyCheckboxFilter(String checkboxInputId) {

        dismissOnboardingOverlayIfPresent();

        By label = By.cssSelector("label[for='" + checkboxInputId + "']");
        By checkboxInput = By.id(checkboxInputId);

        scrollToElement(label);

        try {
            click(label);
            waitUtil.waitUntilSelected(checkboxInput);

        } catch (TimeoutException labelClickDidNotStick) {

            jsClick(checkboxInput);
            waitUtil.waitUntilSelected(checkboxInput);
        }
    }

    // Closes the onboarding overlay if it is visible.
    private void dismissOnboardingOverlayIfPresent() {
        dismissOverlayIfPresent(onboardingOverlay);
    }
}