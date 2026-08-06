# Trendyol Automation Project

## Purpose

This project is a **learning project** assigned by my mentor to help me understand how modern UI automation frameworks are designed and implemented before working on a real-world automation project.

The primary objective is **not** to create a production-ready automation framework, but to gain hands-on experience with the technologies, architecture, and design patterns commonly used in automation testing.

Throughout the project, the focus is on understanding:

* Selenium WebDriver
* Cucumber (BDD)
* Page Object Model (POM)
* Maven project structure
* Explicit waits
* Test architecture
* Writing maintainable automation code

The project is intended as preparation for larger automation projects where the same principles and architecture will be applied.

---

# Business Scenario

The automation reproduces a realistic customer journey on Trendyol.

The intended workflow is:

Open Trendyol.
Log in using valid credentials.
Handle possible popups if they appear.
Navigate to Kategorilerdeki İndirimleri Keşfet.
Select the Ayakkabı category.
Filter products by:
Sport Shoes
Male
Skechers
Select the second product that has the "10 Günün En Düşük Fiyatı" badge.
Add the product to the cart.
Verify that the correct product was added.
Remove the product.
Verify that the product was removed successfully.

During this flow, the website may display dynamic popups such as cookie banners, notification prompts, campaign popups, login verification screens, or modal dialogs. These popups must be handled carefully because they can block clicks, hide elements, or interrupt the normal user journey.

The scenario intentionally follows the same steps a real user would perform instead of taking shortcuts through URLs or artificial sorting.

---

# Learning Objectives

This project is designed to teach the following concepts.

## Project Architecture

Understanding how a Selenium automation framework is organized.

Topics include:

* Maven project structure
* Java packages
* Resource folders
* Separation of responsibilities

---

## Page Object Model

Each page of the application has its own class.

Examples:

* HomePage
* LoginPage
* ProductListingPage
* ProductDetailPage
* CartPage

Each page object contains:

* page locators
* page actions

Page objects should not contain business assertions.

---

## Reusability

Instead of duplicating Selenium code, common functionality is centralized.

Examples include:

* click()
* type()
* scrollToElement()
* isVisible()

These reusable methods are implemented in `BasePage`.

---

## Explicit Waits

The project avoids:

* `Thread.sleep()`

Instead, it uses explicit waits through a dedicated `WaitUtil` class.

Learning when and why to wait for:

* visibility
* clickability
* page load

is one of the key goals of the project.

---

## Configuration Management

Project configuration is separated into two categories.

### config.properties

Contains application configuration such as:

* base URL
* browser
* wait duration

### .env

Contains sensitive information such as:

* email
* password

This demonstrates good practices for separating configuration from source code.

---

## Driver Management

A dedicated `DriverFactory` class is responsible for:

* creating WebDriver
* configuring the browser
* closing the browser

This prevents WebDriver creation from being scattered throughout the project.

---

## Behaviour Driven Development

The project introduces Cucumber.

Feature files describe behaviour in business language.

Example:

* Given
* When
* Then

Step Definitions connect those business steps to Java methods.

---

## Assertions

Assertions are used to verify expected behaviour.

Examples:

* login succeeded
* product added successfully
* product removed successfully

Assertions belong in Step Definitions rather than Page Objects.

---

## Locator Strategy

Element locators should be written using the following priority whenever possible:

1. `id`
2. `data-testid`
3. other stable unique attributes
4. XPath based on stable HTML

Locators should always be created from inspected HTML instead of assumptions.

---

# Coding Principles

The project follows several important principles.

* Keep methods short and focused.
* Avoid duplicated code.
* Write readable method names.
* Separate responsibilities between classes.
* Prefer explicit waits.
* Avoid hard-coded credentials.
* Build locators from actual HTML.
* Keep automation code easy to understand.

Because this is a learning project, readability is considered more important than writing overly complex or highly optimized solutions.

---

# Personal Learning Goals

Through this project I aim to become comfortable with:

* designing a Selenium framework from scratch
* understanding Maven project organization
* implementing Page Object Model correctly
* writing maintainable automation code
* using Cucumber effectively
* creating reusable utility classes
* managing waits correctly
* inspecting HTML and creating reliable locators
* understanding how a professional automation framework is structured

The knowledge gained from this project will serve as the foundation for future real-world automation projects where these concepts will be applied on larger and more complex systems.
