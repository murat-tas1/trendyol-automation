Feature: Trendyol homepage and login

  Scenario: Successful login to Trendyol
    Given I open the Trendyol homepage
    When I close any popup if present
    And I log in with valid credentials
    Then I should be logged in successfully
    And I return to the homepage
    And I navigate to Kategorilerdeki Indirimleri Kesfet
    And I select the Ayakkabi category
    And I filter by Spor Ayakkabi category
    And I filter by Erkek gender
    And I filter by Skechers brand
    And I select the second lowest price badge product
    And I add the product to the cart
    Then the product should be in the cart
    When I remove the product from the cart
    Then the product should be removed from the cart
