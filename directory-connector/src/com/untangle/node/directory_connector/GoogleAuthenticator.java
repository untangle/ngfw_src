/**
 * $Id: FacebookAuthenticator.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */
package com.untangle.node.directory_connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.untangle.uvm.UvmContextFactory;

import org.apache.log4j.Logger;
import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
    
public class GoogleAuthenticator
{
    private static final Logger logger = Logger.getLogger(GoogleAuthenticator.class);

    public static boolean authenticate(String username, String password)
    {
        DirectoryConnectorApp directoryConnector = (DirectoryConnectorApp)UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
        if ( directoryConnector != null )
            directoryConnector.startXvfbIfNecessary();

        logger.debug("Initializing WebDriver...");

        String port = ":1.5";
        Path   tmpDir;
        String tmpDirName;
        try {
            tmpDir = Files.createTempDirectory( "tmp-chromedriver" );
            tmpDirName = "/tmp/" + tmpDir.getFileName().toString() + "/";
        } catch ( Exception e ) {
            logger.warn("Failed to create temp directory.",e);
            return false;
        }
        
        ChromeDriverService service = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File("/usr/lib/chromium/chromedriver"))
            .usingAnyFreePort()
            .withEnvironment(ImmutableMap.of("DISPLAY",port))
            .build();
        System.setProperty("webdriver.chrome.driver", "/usr/lib/chromium/chromedriver");
        System.setProperty("webdriver.chrome.bin", "/usr/bin/chromium");
        System.setProperty("webdriver.chrome.logfile", "/tmp/chrome.log");
        System.setProperty("webdriver.chrome.verboseLogging", "true");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--user-data-dir=" + tmpDirName);
        WebDriver driver = new ChromeDriver(service, options);
        // WebDriver driver = new ChromeDriver(service);
            
        logger.debug("Initializing WebDriver... done (" + port + "," + tmpDirName + ")");

        try {
            Wait<WebDriver> wait = new WebDriverWait(driver, 30);

            String baseUrl = "https://accounts.google.com/ServiceLogin#identifier";
            logger.debug("[" + username + "] Authenticating...");

            logger.debug("[" + username + "] Loading page...");
            driver.get(baseUrl);

            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-0.png");
            try {
                waitForElementId(driver, wait, "next");
            } catch (Exception e) {
                if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-0-exception.png");
                throw e;
            }
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-1.png");
        
            List<WebElement> emailElements = driver.findElements(By.name("Email"));
            List<WebElement> nextButtonElements = driver.findElements(By.id("next"));
            if ( emailElements.size() < 1 ) {
                logger.warn("Email element missing");
                return false;
            }
            if ( nextButtonElements.size() < 1 ) {
                logger.warn("Next button missing");
                return false;
            }
            logger.debug("[" + username + "] Sending email...");
            emailElements.get(0).sendKeys(username);

            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-2.png");
            nextButtonElements.get(0).click();
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-3.png");

            try {
                waitForPassword(driver, wait);
            } catch (Exception e) {
                if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-3-exception.png");
                throw e;
            }
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-4.png");

            List<WebElement> passwdElements = driver.findElements(By.name("Passwd"));
            List<WebElement> signInButtonElements = driver.findElements(By.id("signIn"));
            if ( passwdElements.size() < 1 ) {
                logger.warn("Passwd element missing");
                return false;
            }
            if ( signInButtonElements.size() < 1 ) {
                logger.warn("Next button missing");
                return false;
            }
            logger.debug("[" + username + "] Sending password...");
            passwdElements.get(0).sendKeys(password);

            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-5.png");
            signInButtonElements.get(0).click();
            try {
                waitForLogin(driver, wait);
            } catch (Exception e) {
                if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-5-exception.png");
                //printElements(driver);
                throw e;
            }
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-6.png");

            //printElements(driver);
        
            logger.debug("[" + username + "] Analyzing page...");
            boolean succeeded = wasLoginSuccessful( driver );
            if ( succeeded ) 
                logger.debug("[" + username + "] Login successful.");
            else
                logger.debug("[" + username + "] Login failed.");

            return succeeded;
        } catch (Exception e) {
            logger.warn("Exception",e);
            return false;
        } finally {
            try { driver.close(); } catch (Exception e) {}
            try { driver.quit(); } catch (Exception e) {}
            if ( ! logger.isDebugEnabled() )
                UvmContextFactory.context().execManager().exec("/bin/rm -rf " + tmpDirName);
        }
        
    }

    private static boolean wasLoginSuccessful( WebDriver driver )
    {
        List<WebElement> elements;

        /**
         * If prompting for email or passwd, login must have failed
         */
        elements = driver.findElements(By.name("Email"));
        if ( elements.size() > 0 )
            return false;
        elements = driver.findElements(By.name("Passd"));
        if ( elements.size() > 0 )
            return false;

        /**
         * Look for elements on the account page
         */
        elements = driver.findElements(By.id("xb"));
        if ( elements.size() > 0 )
            return true;
        elements = driver.findElements(By.id("roster-for-Google-Help"));
        if ( elements.size() > 0 )
            return true;

        /**
         * Look for common phrases on the account page
         * This will only work in english so its a last resort
         */
        String pageSource = driver.getPageSource();
        if ( pageSource.contains("Account Settings") )
            return true;
        if ( pageSource.contains("Welcome") )
            return true;

        return false;
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

    private static void waitForLogin(final WebDriver driver, Wait<WebDriver> wait)
    {
        final String elementName1 = "Passwd";
        final String elementId2 = "xb";
        
        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver webDriver) {
                logger.debug("Waiting for login...");
                
                List<WebElement> one = driver.findElements(By.name(elementName1));
                if ( one.size() > 0 ) {
                    logger.debug("Found element name: " + elementName1);
                    return true;
                }
                List<WebElement> two = driver.findElements(By.id(elementId2));
                if ( two.size() > 0 ) {
                    logger.debug("Found element ID: " + elementId2);
                    return true;
                }
                if ( driver.getPageSource().contains("Welcome") ) {
                    logger.debug("Found string: " + "Welcome");
                    return true;
                }
                if ( driver.getPageSource().contains("My Account") ) {
                    logger.debug("Found string: " + "My Account");
                    return true;
                }
                return false;
            }
        });
    }

    private static void waitForPassword(final WebDriver driver, Wait<WebDriver> wait)
    {
        final String elementName1 = "Passwd";
        final String elementId2 = "errormsg_0_Email";

        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver webDriver) {
                logger.debug("Searching for name:" + elementName1 + " or id:" + elementId2 + " ... ");

                List<WebElement> one = driver.findElements(By.name(elementName1));
                if ( one.size() > 0 && one.get(0).isDisplayed() ) {
                    //logger.warn("Passwd field found");
                    return true;
                }
                List<WebElement> two = driver.findElements(By.id(elementId2));
                try {
                    if ( two.size() > 0 && two.get(0).isDisplayed() ) {
                        //logger.warn("Error message found");
                        return true;
                    }
                } catch (Exception e) {}
                return false;
            }
        });
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
    
    private static void takeScreenshot(final WebDriver driver, String filename)
    {
        logger.debug("Saving screenshot: " + filename);
        File scrFile = ((org.openqa.selenium.TakesScreenshot)driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
        copyFile(scrFile, new File(filename));
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
