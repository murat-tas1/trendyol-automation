package stepDefinitions;

import factory.DriverFactory;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import pageObjects.CartPage;
import pageObjects.CategoryPage;
import pageObjects.HomePage;
import pageObjects.LoginPage;
import pageObjects.ProductDetailPage;
import pageObjects.ProductListingPage;
import utils.ConfigReader;

public class TrendyolSteps {

    private HomePage homePage;
    private CategoryPage categoryPage;
    private ProductListingPage productListingPage;
    private ProductDetailPage productDetailPage;
    private CartPage cartPage;
    private String selectedProductTitle;

    @Given("I open the Trendyol homepage")
    public void iOpenTheTrendyolHomepage() {
        DriverFactory.getDriver().get(ConfigReader.getBaseUrl());
        homePage = new HomePage(DriverFactory.getDriver());
    }

    @When("I close any popup if present")
    public void iCloseAnyPopupIfPresent() {
        homePage.closePopupIfPresent();
    }

    @And("I log in with valid credentials")
    public void iLogInWithValidCredentials() {
        LoginPage loginPage = homePage.openLoginPage();
        homePage = loginPage.login(ConfigReader.getEmail(), ConfigReader.getPassword());
        homePage.closePopupIfPresent();
    }

    @Then("I should be logged in successfully")
    public void iShouldBeLoggedInSuccessfully() {
        homePage.openAccountPanel();
        String userInfo = homePage.getLoggedInUserInfoText();
        Assert.assertTrue("Expected the account panel to display the logged-in user's email",
                userInfo.contains("@"));
    }

    @And("I return to the homepage")
    public void iReturnToTheHomepage() {
        homePage = homePage.goToHomePage();
    }

    @And("I navigate to Kategorilerdeki Indirimleri Kesfet")
    public void iNavigateToCategoryDiscounts() {
        categoryPage = homePage.navigateToCategoryDiscounts();
    }

    @And("I select the Ayakkabi category")
    public void iSelectTheAyakkabiCategory() {
        productListingPage = categoryPage.selectAyakkabiCategory();
    }

    @And("I filter by Spor Ayakkabi category")
    public void iFilterBySporAyakkabiCategory() {
        productListingPage.filterBySporAyakkabiCategory();
    }

    @And("I filter by Erkek gender")
    public void iFilterByErkekGender() {
        productListingPage.filterByErkekGender();
    }

    @And("I filter by Skechers brand")
    public void iFilterBySkechersBrand() {
        productListingPage.filterBySkechersBrand();
    }

    @And("I select the second lowest price badge product")
    public void iSelectTheSecondLowestPriceBadgeProduct() {
        productDetailPage = productListingPage.selectSecondLowestPriceBadgeProduct();
    }

    @And("I add the product to the cart")
    public void iAddTheProductToTheCart() {
        selectedProductTitle = productDetailPage.getProductTitle();
        productDetailPage.addToCart();
    }

    @Then("the product should be in the cart")
    public void theProductShouldBeInTheCart() {
        cartPage = productDetailPage.goToCart();
        Assert.assertTrue("Expected the cart to contain: " + selectedProductTitle,
                cartPage.isProductInCart(selectedProductTitle));
    }

    @When("I remove the product from the cart")
    public void iRemoveTheProductFromTheCart() {
        cartPage.removeProduct(selectedProductTitle);
    }

    @Then("the product should be removed from the cart")
    public void theProductShouldBeRemovedFromTheCart() {
        cartPage.reopenCart();
        Assert.assertTrue("Expected the product to disappear from the cart: " + selectedProductTitle,
                cartPage.waitUntilProductRemoved(selectedProductTitle));
    }
}
