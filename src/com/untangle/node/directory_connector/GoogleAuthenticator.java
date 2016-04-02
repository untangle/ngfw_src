/*
 * $Id: GoogleAuthenticator.java,v 1.00 2016/04/02 13:18:29 dmorris Exp $
 */
package com.untangle.node.directory_connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import org.apache.log4j.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class GoogleAuthenticator
{
    private static final Logger logger = Logger.getLogger(GoogleAuthenticator.class);

    public static boolean authenticate(String username, String password)
    {
        WebDriver driver = new PhantomJSDriver();
        Wait<WebDriver> wait = new WebDriverWait(driver, 30);
        String baseUrl = "https://accounts.google.com/ServiceLogin?sacu=1";
        driver.get(baseUrl);

        waitForElementId(driver, wait, "next");
        
        String actualTitle = driver.getTitle();
        logger.debug("Title: " + actualTitle);

        WebElement emailElement = driver.findElement(By.name("Email"));
        WebElement nextButtonElement = driver.findElement(By.id("next"));
        logger.debug("emailElement: " + emailElement);
        logger.debug("nextButtonElement: " + nextButtonElement);

        emailElement.sendKeys(username);
        nextButtonElement.click();

        waitForElementName(driver, wait, "Passwd");

        WebElement passwdElement = driver.findElement(By.name("Passwd"));
        WebElement signInButtonElement = driver.findElement(By.id("signIn"));
        logger.debug("passwdElement: " + passwdElement);
        logger.debug("signInButtonElement: " + signInButtonElement);

        passwdElement.sendKeys(password);
        signInButtonElement.click();

        waitForLogin(driver, wait, "Email","xb");

        //printElements(driver);
        
        boolean succeeded = wasLoginSuccessful( driver );
        if ( succeeded ) 
            logger.debug("Login successful.");
        else
            logger.debug("Login FAILED.");
        
        takeScreenshot(driver);

        driver.close();

        return succeeded;
    }


    private static void printElements( WebDriver driver )
    {
        List<WebElement> elements = driver.findElements(By.cssSelector("*"));
        for ( WebElement element : elements ) {
            logger.debug("element: " + element.getTagName() + " id: " + element.getAttribute("id") + " class: " + element.getAttribute("class"));
            String text = element.getText();
            if ("".equals(text))
                continue;
            String[] lines = text.split("\n");
            if (lines.length > 0 )
                logger.debug("text: " + lines[0]);
        }
    }
    
    private static boolean wasLoginSuccessful( WebDriver driver )
    {
        /**
         * If prompting for email, login must have failed
         */
        List<WebElement> emails = driver.findElements(By.name("Email"));
        if ( emails.size() > 0 )
            return false;

        /**
         * The XB element is only on the account page
         */
        List<WebElement> xbs = driver.findElements(By.id("xb"));
        if ( xbs.size() > 0 )
            return true;
        /**
         * If it says account settings, must have succeeded
         * This may not be in english so this may not work
         */
        if ( driver.getPageSource().contains("Account Settings") )
            return true;
        return false;
    }
    
    private static void waitForLogin(final WebDriver driver, Wait<WebDriver> wait, final String elementName1, final String elementId2)
    {
        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver webDriver) {
                logger.debug("Searching for name:" + elementName1 + " or id:" + elementId2 + " ... ");
                
                List<WebElement> one = driver.findElements(By.name(elementName1));
                if ( one.size() > 0 )
                    return true;
                List<WebElement> two = driver.findElements(By.id(elementId2));
                if ( two.size() > 0 )
                    return true;
                return false;
            }
        });
    }

    private static void waitForElementId(final WebDriver driver, Wait<WebDriver> wait, final String elementId)
    {
        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver webDriver) {
                logger.debug("Searching for id:" + elementId + " ...");
                return webDriver.findElement(By.id(elementId)) != null;
            }
        });
    }

    private static void waitForElementName(final WebDriver driver, Wait<WebDriver> wait, final String elementName)
    {
        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver webDriver) {
                logger.debug("Searching for name:" + elementName + " ...");
                return webDriver.findElement(By.name(elementName)) != null;
            }
        });
    }

    private static void takeScreenshot(final WebDriver driver)
    {
        logger.warn("Saving screenshot: /tmp/screenshot.png");
        File scrFile = ((org.openqa.selenium.TakesScreenshot)driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
        copyFile(scrFile, new File("/tmp/screenshot.png"));
    }
    
    private static void copyFile(File sourceFile, File destFile) 
    {
        try {
            if(!destFile.exists()) {
                destFile.createNewFile();
            }

            FileChannel source = null;
            FileChannel destination = null;

            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
            }
            finally {
                if(source != null) {
                    source.close();
                }
                if(destination != null) {
                    destination.close();
                }
            }
        } catch (Exception e) {}
        
    }
}
