package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class ProductListingPage extends BasePage {

    private static final String SPOR_AYAKKABI_CHECKBOX_ID = "checkbox-web-aggregations-checkbox-WebCategory-109-109";
    private static final String ERKEK_CHECKBOX_ID = "checkbox-web-aggregations-checkbox-WebGender-2-2";
    private static final String SKECHERS_CHECKBOX_ID = "checkbox-web-aggregations-checkbox-WebBrand-658-658";

    private final By brandSearchInput = By.xpath("//input[contains(@placeholder,'Marka')]");
    private final By onboardingOverlay = By.cssSelector("div[data-testid='onboarding-overlay']");
    // Match the real "10 Günün En Düşük Fiyatı" badge by its own data-testid, NOT by
    // text: the "Son 10 Günün En Düşük Fiyatı" price-history label appears on almost
    // every card and a text/contains match would wrongly count those too.
    private final By secondLowestPriceBadgeProductCard = By.xpath(
            "(//a[@data-testid='product-card'][.//*[@data-testid='lowest-price-duration-badge']])[2]");

    public ProductListingPage(WebDriver driver) {
        super(driver);
    }

    public void filterBySporAyakkabiCategory() {
        applyCheckboxFilter(SPOR_AYAKKABI_CHECKBOX_ID);
    }

    public void filterByErkekGender() {
        applyCheckboxFilter(ERKEK_CHECKBOX_ID);
    }

    public void filterBySkechersBrand() {
        // the brand list is virtualized, so search instead of scrolling: narrowing the list
        // forces the Skechers row to render
        dismissOnboardingOverlayIfPresent();
        type(brandSearchInput, "Skechers");
        applyCheckboxFilter(SKECHERS_CHECKBOX_ID);
    }

    public ProductDetailPage selectSecondLowestPriceBadgeProduct() {
        // a coaching overlay dims the grid and intercepts clicks; close it first,
        // then scroll the card into view so it is actually visible before clicking
        dismissOnboardingOverlayIfPresent();
        scrollUntilVisible(secondLowestPriceBadgeProductCard);
        scrollToElement(secondLowestPriceBadgeProductCard);
        dismissOnboardingOverlayIfPresent();
        clickWithHoverRecovery(secondLowestPriceBadgeProductCard);
        switchToNewTab();
        return new ProductDetailPage(driver);
    }

    private void applyCheckboxFilter(String checkboxInputId) {
        // close the dimming coach-mark overlay so the click lands like a real user's,
        // instead of being intercepted and silently forced through with JavaScript
        dismissOnboardingOverlayIfPresent();

        By label = By.cssSelector("label[for='" + checkboxInputId + "']");
        By checkboxInput = By.id(checkboxInputId);
        scrollToElement(label);

        try {
            click(label);
            waitUtil.waitUntilSelected(checkboxInput);
        } catch (TimeoutException labelClickDidNotStick) {
            // custom checkbox UI swallowed the click; dispatch it straight to the input instead
            jsClick(checkboxInput);
            waitUtil.waitUntilSelected(checkboxInput);
        }
    }

    private void dismissOnboardingOverlayIfPresent() {
        dismissOverlayIfPresent(onboardingOverlay);
    }
}
