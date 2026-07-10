# Trendyol UI Automation — Framework Study Guide

A walk-through of how this test framework is designed, why each layer exists, how the
key code works, and the questions a reviewer is likely to ask — with answers.

**Stack:** Java 17 · Selenium 4.43 · Cucumber 7.20 (BDD) · JUnit 4 runner · Maven · WebDriverManager · dotenv for secrets

---

## Table of contents

1. [The big picture](#1-the-big-picture)
2. [Architecture & layers](#2-architecture--layers)
3. [Design decisions (and the reasoning)](#3-design-decisions-and-the-reasoning)
4. [How a single run executes](#4-how-a-single-run-executes)
5. [Code walkthrough](#5-code-walkthrough)
6. [Handling a real, hostile website](#6-handling-a-real-hostile-website)
7. [Questions your reviewer will likely ask](#7-questions-your-reviewer-will-likely-ask)

---

## 1. The big picture

The framework automates a **real customer journey on Trendyol**: open the site, log in,
dismiss whatever popups appear, drill down through categories and filters to a specific
Skechers men's sport shoe, add it to the cart, verify it's there, remove it, and verify
it's gone. The journey is written in plain business language and executed by a browser,
exactly as a human tester would do it — no URL shortcuts, no fake sorting.

It is deliberately built the way a professional framework is structured, so the value
isn't the script — it's the **architecture**: behaviour is separated from page
interaction, page interaction is separated from low-level Selenium plumbing, and
configuration is separated from code.

> **The 20-second pitch**
> A Cucumber feature file describes the journey in Given/When/Then. Step definitions
> translate each line into calls on **Page Objects**. Page objects describe *what* a page
> can do; a shared `BasePage` + `WaitUtil` handle *how* to click/type/wait reliably. A
> `DriverFactory` owns the browser, and configuration lives outside the code.

---

## 2. Architecture & layers

Each layer has one responsibility and talks only to the layer directly below it. This is
the single most important thing to be able to draw on a whiteboard.

```
┌─────────────────────────────────────────────────────────────────────┐
│  .feature file        The journey in Given/When/Then. No code.        │
│                       homepage_login_test.feature                     │
└─────────────────────────────────────────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Runner + Hooks       RunCucumberTest starts Cucumber; Hooks open the │
│                       browser before each scenario and quit it after. │
└─────────────────────────────────────────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step Definitions     TrendyolSteps — glue code. Each Given/When/Then │
│                       maps to a Java method. ASSERTIONS LIVE HERE.     │
└─────────────────────────────────────────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Page Objects         HomePage, LoginPage, CategoryPage,              │
│                       ProductListingPage, ProductDetailPage, CartPage │
│                       — locators + actions for one page each.         │
└─────────────────────────────────────────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  BasePage             Reusable building blocks every page inherits:   │
│                       click, type, scrollToElement, overlay dismissal.│
└─────────────────────────────────────────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Utilities            WaitUtil (explicit waits), DriverFactory        │
│                       (browser lifecycle), ConfigReader (config).     │
└─────────────────────────────────────────────────────────────────────┘
```

> **Why this matters**
> If a locator changes on the Trendyol login page, you fix it in **one place**
> (`LoginPage`) and nothing else moves. If Trendyol adds a new popup, you handle it in
> `BasePage` and every page benefits. Responsibilities don't leak across layers.

### Project structure

```
src/test/java/
  factory/          DriverFactory.java      // creates & owns the WebDriver
  hooks/            Hooks.java              // @Before / @After lifecycle
  pageObjects/      BasePage.java           // shared reusable actions
                    HomePage.java  LoginPage.java  CategoryPage.java
                    ProductListingPage.java  ProductDetailPage.java  CartPage.java
  runners/          RunCucumberTest.java    // entry point for Maven / JUnit
  stepDefinitions/  TrendyolSteps.java      // glue + assertions
  utils/            WaitUtil.java  ConfigReader.java

src/test/resources/
  features/         homepage_login_test.feature   // the scenario
  config/           config.properties             // url, browser, wait time
.env                                              // email + password (git-ignored)
```

---

## 3. Design decisions (and the reasoning)

A reviewer rarely asks "what did you use" — they ask "why." Here is the why for each choice.

| Decision | What it is | Why it's there |
| --- | --- | --- |
| **Page Object Model** | One class per page = its locators + its actions. | Locators change constantly on a live site. POM keeps each fix in one file and makes tests read like sentences. |
| **BasePage (inheritance)** | Common actions every page shares. | DRY — `click`/`type`/`scroll` written once, not copy-pasted into six pages. |
| **Explicit waits only** | `WaitUtil` wrapping `WebDriverWait`. No `Thread.sleep()`. | Sleeps are either too short (flaky) or too slow. Explicit waits wait for the *exact condition* and continue the instant it's true. |
| **DriverFactory** | One class creates/configures/quits the browser. | Browser creation isn't scattered everywhere; swapping Chrome→Firefox is a config change, not a code hunt. |
| **config.properties vs .env** | Non-secret settings vs. credentials. | URL/browser/timeout can be committed; email/password must never be. `.env` is git-ignored. |
| **Cucumber (BDD)** | Given/When/Then feature files. | The scenario is readable by non-programmers and doubles as living documentation of the journey. |
| **Assertions in step defs** | Page objects never assert; steps do. | A page object is reusable machinery; it shouldn't decide pass/fail. Keeping *verification* in the test layer keeps pages reusable across scenarios. |

---

## 4. How a single run executes

Follow the control flow once and the whole framework clicks into place.

1. **Maven runs** `RunCucumberTest` (Surefire is configured to pick exactly this class).
2. Cucumber reads the `.feature` file and, using the **glue** packages, matches each
   Given/When/Then line to a Java method by its annotation text.
3. Before the scenario, the `@Before` hook calls `DriverFactory.createDriver()` → a
   configured Chrome opens.
4. Each step method asks a **page object** to do something and gets the *next* page object
   back (e.g. `login()` returns a `HomePage`). This "return the next page" style is called
   **fluent chaining**.
5. Page objects call `BasePage` helpers, which call `WaitUtil`, which waits for the right
   condition before touching the element.
6. **Then** steps run JUnit `Assert` checks to verify the outcome.
7. After the scenario, the `@After` hook calls `quitDriver()` → the browser closes, win or lose.

> **Key idea — the driver is shared**
> `DriverFactory` holds the WebDriver in a `static` field, so the hook, every step, and
> every page object all operate on the *same* browser window throughout the scenario.

---

## 5. Code walkthrough

The snippets worth being able to explain line-by-line. Each one demonstrates a specific concept.

### a. The feature file — the journey in business language

```gherkin
Feature: Trendyol homepage and login

  Scenario: Successful login to Trendyol
    Given I open the Trendyol homepage
    When I close any popup if present
    And I log in with valid credentials
    Then I should be logged in successfully
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
```

No Java here at all. This reads like a requirements document — that's the point of BDD.

### b. Step definition — connecting a sentence to code

```java
@And("I log in with valid credentials")
public void iLogInWithValidCredentials() {
    LoginPage loginPage = homePage.openLoginPage();
    homePage = loginPage.login(ConfigReader.getEmail(), ConfigReader.getPassword());
    homePage.closePopupIfPresent();
}

@Then("the product should be in the cart")
public void theProductShouldBeInTheCart() {
    cartPage = productDetailPage.goToCart();
    Assert.assertTrue("Expected the cart to contain: " + selectedProductTitle,
            cartPage.isProductInCart(selectedProductTitle));   // verification lives HERE, not in the page
}
```

The annotation string must match the feature line exactly. Notice the step is thin: it
orchestrates page objects and asserts — it contains no locators and no Selenium calls.

### c. ConfigReader — configuration separated from code

```java
private static final Properties properties = new Properties();
private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

static {   // runs once when the class is first used — loads config.properties
    try (InputStream input = ConfigReader.class.getClassLoader()
            .getResourceAsStream("config/config.properties")) {
        if (input == null) throw new RuntimeException("config.properties not found");
        properties.load(input);
    } catch (IOException e) { throw new RuntimeException("Failed to load config.properties", e); }
}

public static String getBaseUrl()  { return properties.getProperty("baseUrl"); }
public static String getEmail()    { return dotenv.get("TRENDYOL_EMAIL"); }     // secret, from .env
```

Two sources on purpose: non-secret settings from `config.properties` (committed),
credentials from `.env` (git-ignored). No password is ever hard-coded.

### d. DriverFactory — the Factory pattern for the browser

```java
public static WebDriver createDriver() {
    String browser = ConfigReader.getBrowser();
    switch (browser.toLowerCase()) {
        case "firefox":
            WebDriverManager.firefoxdriver().setup();   // auto-downloads the right driver binary
            driver = new FirefoxDriver();
            break;
        case "chrome":
        default:
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(chromeOptionsWithNotificationsDisabled());
    }
    driver.manage().window().maximize();
    return driver;
}
```

One place decides which browser to build and how to configure it (notifications disabled
so Chrome's "Allow notifications?" prompt can't block clicks). `WebDriverManager` removes
the old pain of manually downloading `chromedriver.exe` and matching versions.

### e. Hooks — browser lifecycle around every scenario

```java
@Before  public void setUp()    { DriverFactory.createDriver(); }  // fresh browser per scenario
@After   public void tearDown() { DriverFactory.quitDriver();  }  // always closes, even on failure
```

### f. Page object — actions, no assertions, fluent return

```java
private final By emailInput     = By.id("login-email");                          // id first — most stable
private final By continueButton = By.xpath("//button[@data-testid='email-check-button']");
private final By passwordInput  = By.id("login-password");

public HomePage login(String email, String password) {
    enterEmail(email);
    clickContinue();
    enterPassword(password);
    clickLoginButton();
    return new HomePage(driver);   // hand back the page you're now on → fluent chaining
}
```

Locators use the priority **id → data-testid → stable attribute → XPath**. The method
returns the page the user lands on next, so steps read as a flow.

### g. WaitUtil — explicit waits, including a custom retrying click

```java
public WebElement waitForVisibility(By locator) {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
}

// A click that keeps retrying until the element actually accepts it.
public void clickWhenReady(By locator) {
    wait.until(webDriver -> {
        try {
            webDriver.findElement(locator).click();
            return true;                                       // success → stop waiting
        } catch (ElementNotInteractableException
                 | StaleElementReferenceException
                 | NoSuchElementException e) {
            return false;                                      // not ready yet → poll again
        }
    });
}
```

This is the heart of reliability. Instead of a blind `sleep`, it re-attempts the click
every polling interval and only gives up after the timeout — automatically absorbing the
brief moments when a live page isn't quite ready.

---

## 6. Handling a real, hostile website

Trendyol throws cookie banners, campaign modals, coaching overlays, and hover effects at
you. This is where most of the engineering actually went — and it makes a great story to tell.

### Optional popups — wait a bounded time, act only if present

```java
public void closePromoPopup() {
    if (waitUtil.isVisibleWithin(promoPopupCloseButton, 5)) {  // appears only sometimes
        click(promoPopupCloseButton);
        waitUtil.waitForInvisibility(promoPopupCloseButton);   // wait until it's truly gone
    }
}
```

The popup doesn't appear on every visit, so we can't hard-wait for it (that would fail when
it's absent) and we can't ignore it (it covers the *Sil* button when present). The bounded
"is it visible within 5s?" check solves both.

### Robust clicking — layered fallbacks

```java
try { waitUtil.clickWhenReady(locator); return; }        // 1. normal retrying click
catch (TimeoutException stillBlocked) { /* a hover overlay may be covering it */ }

new Actions(driver).moveToElement(body, 0, 0).perform();   // 2. move mouse away to un-hover

try { waitUtil.clickWhenReady(locator); }
catch (TimeoutException e) { jsClick(locator); }         // 3. last resort: JS click
```

> **⚠ The bug we found & fixed — good to mention**
> The product card sometimes threw `ElementNotInteractableException`. The retrying click
> only caught three *other* exception types, so this one escaped the retry loop *and* the
> hover-recovery fallback (which only catches `TimeoutException`) — and failed the run. The
> fix: add `ElementNotInteractableException` to the caught types. Because
> `ElementClickInterceptedException` is a **subclass** of it, catching the parent covers
> both the "not interactable" and the "click intercepted" cases. Now it retries and, if
> needed, falls through to the JS-click fallback.

This is also a lesson in **flaky tests**: the same code passed five times the day before
because the failure only surfaced intermittently, depending on which live product landed in
slot 2 and on hover/lazy-load timing. "Passed 5×" doesn't mean "correct" for a
non-deterministic click.

### Dynamic, content-based locators

```java
// The cart holds several products. Start from our product's name, climb to the
// nearest row that also contains a "Sil" button, then take that row's "Sil".
By.xpath("(//*[contains(\"" + productTitle + "\", normalize-space(.))]"
    + "/ancestor::*[.//*[normalize-space(text())='Sil']][1]"
    + "//*[normalize-space(text())='Sil'])[1]");
```

Because the product is chosen at runtime (whatever is in slot 2 today), locators are built
*from the captured product title*, not hard-coded — so the "remove" button clicked is
guaranteed to be the one belonging to *our* product.

---

## 7. Questions your reviewer will likely ask

### Design & architecture

**Why the Page Object Model? What problem does it solve?**
On a live site, locators break often. POM confines each page's locators and actions to one
class, so a change is a one-file fix, tests read like sentences instead of Selenium noise,
and page logic is reusable across many scenarios. Without it you get duplicated
`findElement` calls scattered everywhere and unmaintainable tests.

**Why do assertions live in step definitions, not in page objects?**
A page object is reusable machinery — it shouldn't decide what counts as pass or fail,
because different scenarios verify different things. Verification is a *test* concern, so it
belongs in the step layer. Keeping pages assertion-free lets them be reused by any scenario.

**What is BasePage and why inheritance?**
It holds the actions every page needs — `click`, `type`, `scrollToElement`, `isVisible`,
overlay dismissal, tab switching. Every page `extends BasePage`, so that code is written
once (DRY). If I improve how clicking works, all six pages get the improvement for free.

**Why a DriverFactory, and why is its constructor private?**
It centralizes browser creation, configuration, and teardown so WebDriver setup isn't
copy-pasted around. The private constructor + static methods mean nobody can instantiate it
— it's a utility/factory, used as `DriverFactory.createDriver()`. It also decouples the
tests from *which* browser is used; that's read from config.

### Waits & reliability

**Why explicit waits instead of Thread.sleep()?**
`Thread.sleep()` is a fixed guess: too short and the test is flaky, too long and every run
is slow. An explicit wait polls for a *specific condition* (visible, clickable, invisible)
and continues the instant it's true, failing only after a timeout. It's both faster and
more reliable.

**Explicit vs implicit waits — what's the difference?**
An implicit wait is a global "retry findElement for up to N seconds" setting. An explicit
wait waits for a *particular condition* on a *particular element*. Explicit is more precise,
and mixing the two can cause unpredictable combined timeouts — so this project uses explicit
waits only.

**visibilityOfElementLocated vs elementToBeClickable?**
Visibility only checks the element is in the DOM and displayed. Clickable additionally
checks it's enabled. An element can be visible but still not truly interactable — which is
exactly the edge case that produced the bug we fixed, where I fall back to a retrying click
and finally a JS click.

**Tell me about a bug you hit and how you fixed it.**
The product card intermittently threw `ElementNotInteractableException`. My retrying click
caught three other exception types but not that one, so it escaped every fallback and failed
the run. I added it to the caught exceptions; since `ElementClickInterceptedException` is a
subclass, catching the parent covers both cases. The deeper lesson was recognizing it as a
*flaky* failure — identical code had passed five times before because the condition only
surfaced intermittently.

### Cucumber & config

**How does a plain-English feature line reach Java code?**
Cucumber scans the **glue** packages (`stepDefinitions`, `hooks`) for methods annotated with
`@Given/@When/@Then/@And` whose text matches the feature line. Match found → it invokes that
method. The runner (`RunCucumberTest`) wires the feature folder to the glue.

**Why split config.properties and .env?**
Different sensitivity. URL, browser, and timeout are safe to commit and belong in
`config.properties`. Email and password are secrets — they go in `.env`, which is
git-ignored, and are read via the dotenv library. This keeps credentials out of source control.

**How is the same browser shared across all the steps?**
`DriverFactory` stores the WebDriver in a `static` field. The hook creates it, and every
step and page object retrieves it via `getDriver()`, so they all drive the one window.
`quitDriver()` in `@After` nulls it out afterward.

### Reflection — what would you improve?

**What are the limitations of this framework?**
It's a learning project with one scenario. Honest limitations: the static driver isn't
thread-safe (no parallel runs); it depends on Trendyol's live catalog, so it's inherently
somewhat flaky; there's no screenshot-on-failure or HTML report yet; and it's single-browser
in practice. Those are exactly the things I'd harden for a real suite.

**How would you scale this for a real project?**
Move the driver into a `ThreadLocal` for parallel execution; add a failure hook that
captures a screenshot and attaches it to a report (e.g. the Cucumber HTML/JSON plugin); run
it in CI headless; externalize test data; and add tags to run subsets. The layered design
already makes all of that additive rather than a rewrite.
