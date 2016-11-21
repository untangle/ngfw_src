/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.File;
import java.io.FileNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.untangle.uvm.util.IOUtil;

/*
 * Use web browser
 */
public class WebBrowser
{
    private final Logger logger = Logger.getLogger(getClass());

    private WebDriver driver = null;
    private Wait<WebDriver> wait = null;

    private String tempDirectory = "/tmp/webbrowser";
    private static String chromeDriver = "/usr/lib/chromium/chromedriver";
    private static String chromeBrowser = "/usr/bin/chromium";

    private Integer displaySequence = 1;
    private Integer displayScreen = 5;
    private Integer screenWidth = 1024;
    private Integer screenHeight = 768;
    private Integer screenDepth = 8;
    private String xCommand = "";

    /**
     * Start browser
     * @param displaySequence display sequence to use
     * @param displayScreen display screen to use
     * @param screenWidth Screen width to use
     * @param screenHeight Screen height to use
     * @param screenDepth Screen depth to use
    */
	public WebBrowser(Integer displaySequence, Integer displayScreen, Integer screenWidth, Integer screenHeight, Integer screenDepth)
    throws java.io.FileNotFoundException
	{
		this.displaySequence = displaySequence;
		this.displayScreen = displayScreen;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.screenDepth = screenDepth;

        Path tmpPath;
        try {
            tmpPath = Files.createTempDirectory("webbrowser-");
            tempDirectory = "/tmp/" + tmpPath.getFileName().toString() + "/";
        } catch ( Exception e ) {
            logger.warn("Failed to create temp directory.",e);
        }

        this.xCommand = "Xvfb" + 
        	" :" + this.displaySequence.toString() 
        	+ " -screen " + this.displayScreen.toString() 
        	+ " " 
        	+ this.screenWidth.toString() + "x" + this.screenHeight.toString() + "x" + this.screenDepth.toString();

		System.setProperty("webdriver.chrome.driver", chromeDriver);
		System.setProperty("webdriver.chrome.bin", chromeBrowser);
		System.setProperty("webdriver.chrome.logfile", tempDirectory + "/chrome.log");
		System.setProperty("webdriver.chrome.verboseLogging", "true");
        
        if(!WebBrowser.exists()){
            throw new FileNotFoundException("Chrome driver does not exist: " + chromeDriver);
        }

        try{
            UvmContextFactory.context().execManager().exec("pkill -f \"" + this.xCommand+ "\"");
            UvmContextFactory.context().execManager().execOutput("nohup " + this.xCommand + " >/dev/null 2>&1 &");
            ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(chromeDriver))
                .usingAnyFreePort()
                .withEnvironment(ImmutableMap.of("DISPLAY",":" + this.displaySequence.toString() + "." + displayScreen.toString()))
                .build();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--user-data-dir=" + tempDirectory);
            driver = new ChromeDriver(service, options);
            wait = new WebDriverWait(driver, 120);
        }catch(UnreachableBrowserException e){
            logger.warn("Unable to open driver, ", e);
        }

	}

    /**
     * Close  the web browser and remove its temporary directory
     *
     */
	public void close()
	{
        wait = null;
        try { 
            driver.close(); 
        } catch (Exception e) {
        	logger.warn("Could not close driver");
        }
        try { 
            driver.quit(); 
        } catch (Exception e) {
        	logger.warn("Could not quit driver");
        }
       UvmContextFactory.context().execManager().exec("pkill -f \"" + this.xCommand+ "\"");
        // debug option to not remove
       IOUtil.rmDir(new File(tempDirectory));
	}

    /**
     * Allow others to determine what we know without forcing an exception check.
     */
    static public Boolean exists(){
        File f = new File(chromeDriver);
        return f.exists();
    }

    /**
     * Open the web browser to the specified URL
     *
     */
	public void openUrl(String url)
	{
    	driver.get(url);
	}

	/**
	 * Get temp directory path
	 */
	public String getTempDirectory()
	{
		return tempDirectory;
	}

	/**
	 * Wait for element.  
     * @param elementId id to wait upon.
     * @return true if found, false if not found.
	 */
	public Boolean waitForElement(String elementId)
	{
		Boolean found = false;
        wait.until(new ExpectedCondition<Boolean>() {
        	public Boolean apply(WebDriver driver) {
            	return driver.findElement(By.id(elementId)) != null;
            }
        });
		return found;
	}

	/**
	 * Take a screenshot of the current screen to the specified filename
	 *
	 * @param filename Name of file to save screenshot.
	 *
	 * @return true if saved successfully, false if not saved
	 */
	public Boolean takeScreenshot(String filename)
	{
		Boolean success = false;
		File scrFile = ((org.openqa.selenium.TakesScreenshot)driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
		try{
			IOUtil.copyFile(scrFile, new File(filename));
            scrFile.delete();
		}catch(Exception e){
			logger.error("Unable to copy " + e);
		}
		return success;
	}

}