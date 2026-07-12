package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CategoryPage extends BasePage {

    // The Ayakkabı card lives in the "Kategorilerdeki İndirimleri Keşfet" banner
    // carousel. Its position shifts between sessions, so we match it by its image
    // alt text rather than a fixed slot index.
    private final By ayakkabiCategoryCard = By.xpath(
            "//p[normalize-space()='Kategorilerdeki İndirimleri Keşfet']"
                    + "/ancestor::div[@data-testid='banner-slider']"
                    + "//img[contains(@alt,'Ayakkabı')]/ancestor::a[1]"); // since img is not clickable we can arrive clickable element a
/*
Why not just use

//img[contains(@alt,'Ayakkabı')]/ancestor::a[1]
Most likely because when you inspected the Trendyol homepage, there were multiple elements related to "Ayakkabı", or you wanted to make the locator resilient to future page changes.
"I don't just want any Ayakkabı image. I want the Ayakkabı image that belongs to the 'Kategorilerdeki İndirimleri Keşfet' banner slider."
instead of the longer XPath?
 */
    // The "Next slide" arrow of that same carousel. When the Ayakkabı card starts
    // off-screen we click this to slide it into view, just like a real user.
    private final By nextArrow = By.xpath(
            "//p[normalize-space()='Kategorilerdeki İndirimleri Keşfet']"
                    + "/ancestor::div[@data-testid='banner-slider']"
                    + "//button[@aria-label='Next slide']");

    public CategoryPage(WebDriver driver) {
        super(driver);
    }
    //after we select ayakkabi then we will be in productlistingpage therefore it returns ProductListingPage
    public ProductListingPage selectAyakkabiCategory() {
        slideUntilAyakkabiCardIsVisible();
        click(ayakkabiCategoryCard);
        return new ProductListingPage(driver);
    }

    private void slideUntilAyakkabiCardIsVisible() {
        int maxSlides = 10;
        for (int slide = 0; slide < maxSlides && !isDisplayedNow(ayakkabiCategoryCard); slide++) {
            click(nextArrow);
            // give the carousel a short moment to bring the next cards into view
            waitUtil.isVisibleWithin(ayakkabiCategoryCard, 2);
        }
    }
}
