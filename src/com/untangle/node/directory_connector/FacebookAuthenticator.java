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

public class FacebookAuthenticator
{
    private static final Logger logger = Logger.getLogger(FacebookAuthenticator.class);

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

            
        logger.debug("Initializing WebDriver... done (" + port + "," + tmpDirName + ")");

        try {
            Wait<WebDriver> wait = new WebDriverWait(driver, 30);

            String baseUrl = "https://facebook.com/login";
            logger.debug("[" + username + "] Authenticating...");

            logger.debug("[" + username + "] Loading page...");
            driver.get(baseUrl);

            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-0.png");
            waitForElementName(driver, wait, "email");
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-1.png");

            logger.debug("[" + username + "] Sending email/password...");
            List<WebElement> emailElements = driver.findElements(By.name("email"));
            List<WebElement> passElements = driver.findElements(By.name("pass"));
            List<WebElement> loginButtonElements = driver.findElements(By.id("loginbutton"));
            if ( emailElements.size() < 1 ) {
                logger.warn("Email element missing");
                return false;
            }
            if ( passElements.size() < 1 ) {
                logger.warn("Password element missing");
                return false;
            }
            if ( loginButtonElements.size() < 1 ) {
                logger.warn("login button missing");
                return false;
            }
            emailElements.get(0).sendKeys(username);
            passElements.get(0).sendKeys(password);
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-2.png");

            loginButtonElements.get(0).click();
            waitForElementId(driver, wait, "facebook");
            if ( logger.isDebugEnabled() ) takeScreenshot(driver,tmpDirName + "screenshot-3.png");

            //printElements(driver);
            //takeScreenshot(driver,"/tmp/screenshot1.png");

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
        } 
        finally {
            try { driver.close(); } catch (Exception e) {}
            try { driver.quit(); } catch (Exception e) {}
            driver.quit();
            if ( ! logger.isDebugEnabled() )
                UvmContextFactory.context().execManager().exec("/bin/rm -rf " + tmpDirName);
        }
    }

    private static boolean wasLoginSuccessful( WebDriver driver )
    {
        /**
         * If prompting for password, login must have failed
         */
        List<WebElement> passwords = driver.findElements(By.name("pass"));
        if ( passwords.size() > 0 )
            return false;

        /**
         * The nav element is only on the account page
         */
        List<WebElement> nav = driver.findElements(By.id("pagesNav"));
        if ( nav.size() > 0 ) {
            logger.debug("Found NAV");
            return true;
        }
        /**
         * If it says Home, must have succeeded
         * This may not be in english so this may not work
         */
        if ( driver.getPageSource().contains("logout_menu") ) {
            logger.debug("Found logout_menu");
            return true;
        }

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
        logger.warn("Saving screenshot: " + filename);
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
