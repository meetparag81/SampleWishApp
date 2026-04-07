package com.samplewishapp;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;

public class WishAppPage {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    private By usernameField  = By.id("username");
    private By passwordField  = By.id("password");
    private By loginButton    = By.id("loginBtn");
    private By wishInputField = By.id("wishInput");
    private By addWishButton  = By.id("addWishBtn");
    private By wishList       = By.id("wishList");
    private By logoutButton   = By.id("logoutLink");

    public WishAppPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.js   = (JavascriptExecutor) driver;   // ✅ JS executor for overlay clicks
    }

    public void navigateTo(String url) {
        driver.get(url);
    }

    public void login(String username, String password) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField));
        driver.findElement(usernameField).clear();
        driver.findElement(usernameField).sendKeys(username);
        driver.findElement(passwordField).clear();
        driver.findElement(passwordField).sendKeys(password);
        driver.findElement(loginButton).click();
        // ✅ Wait for wishlist page to appear after login
        wait.until(ExpectedConditions.visibilityOfElementLocated(wishInputField));
    }

    public void addWish(String wishText) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(wishInputField));
        driver.findElement(wishInputField).clear();
        driver.findElement(wishInputField).sendKeys(wishText);
        // ✅ JS click bypasses chatbot overlay blocking the button
        js.executeScript("arguments[0].click();",
            driver.findElement(addWishButton));
    }

    public boolean verifyWish(String wishText) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(wishList));
        return driver.findElement(wishList).getText().contains(wishText);
    }

    public void logout() {
        driver.get("http://localhost:6060/SampleWishApp/LoginServlet?action=logout");
        System.out.println("After logout URL: " + driver.getCurrentUrl());
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField));
    }
}