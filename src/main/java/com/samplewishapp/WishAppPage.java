package com.samplewishapp;



import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WishAppPage {

    private WebDriver driver;
    private WebDriverWait wait;

    // ── All Locators defined here ─────────────────────────
    private By usernameField = By.id("username");
    private By passwordField = By.id("password");
    private By loginButton   = By.id("loginBtn");
    private By wishInputField = By.id("wishInput");
    private By addWishButton  = By.id("addWishBtn");
    private By wishList       = By.id("wishList");
    private By logoutButton   = By.linkText("Logout");

    public WishAppPage(WebDriver driver) {
        this.driver = driver;        
        this.wait=new WebDriverWait(driver, Duration.ofSeconds(10));
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
    }

    public void addWish(String wishText) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(wishInputField));
        driver.findElement(wishInputField).clear();
        driver.findElement(wishInputField).sendKeys(wishText);
        driver.findElement(addWishButton).click();
    }

    public boolean verifyWish(String wishText) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(wishList));
        return driver.findElement(wishList).getText().contains(wishText);
    }

    public void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(logoutButton));
        driver.findElement(logoutButton).click();
    }
}
