/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * The WanFailoverTester is a thread that handles the execution of a single test
 * It wakes every getDelayMilliseconds milliseconds and runs the test If the WAN
 * status changes it notifies the monitor
 */
public class WanFailoverTester implements Runnable
{
    private static final int SLEEP_DELAY_MS = 5000;

    private static final String ARP_TEST = System.getProperty("uvm.bin.dir") + "/wan-failover-arp-test.sh";
    private static final String PING_TEST = System.getProperty("uvm.bin.dir") + "/wan-failover-ping-test.sh";
    private static final String HTTP_TEST = System.getProperty("uvm.bin.dir") + "/wan-failover-http-test.sh";
    private static final String DNS_TEST = System.getProperty("uvm.bin.dir") + "/wan-failover-dns-test.sh";

    private static final Logger logger = Logger.getLogger(WanFailoverTester.class);

    private WanFailoverTesterMonitor monitor;
    private WanFailoverApp app;
    private WanTestSettings testSettings;

    private boolean exitFlag = false;
    private LinkedList<Boolean> resultList = new LinkedList<Boolean>();
    private Boolean lastWanStatus = null;

    private int totalTestsRun = 0;
    private int totalTestsFailed = 0;
    private int totalTestsPassed = 0;

    /**
     * Constructor
     * 
     * @param settings
     *        Application settings
     * @param monitor
     *        Wan Failover test monitor
     * @param app
     *        Wan Failover application
     */
    public WanFailoverTester(WanTestSettings settings, WanFailoverTesterMonitor monitor, WanFailoverApp app)
    {
        this.testSettings = settings;
        this.monitor = monitor;
        this.app = app;
    }

    /**
     * Main Runnable function
     */
    public void run()
    {
        long lastWakeTimeMillis;

        if (this.testSettings.getDelayMilliseconds() == null) {
            this.warn("Invalid delay - thread exiting");
            return;
        }
        if (this.testSettings.getTestHistorySize() == null) {
            this.warn("Invalid test history size - thread exiting");
            return;
        }

        InterfaceSettings intfConf = UvmContextFactory.context().networkManager().findInterfaceId(testSettings.getInterfaceId());
        if (intfConf == null) {
            this.warn("Unable to locate Interface: " + testSettings.getInterfaceId());
            return;
        }

        this.info("start()");
        lastWakeTimeMillis = System.currentTimeMillis();
        while (true) {
            // sleep for set amount of time
            // we subtract the time it took to run the test so if we are supposed to run every 5 seconds
            // and the test took 3 seconds to run, then we only sleep for 2 seconds
            long sleepTime = (lastWakeTimeMillis + this.testSettings.getDelayMilliseconds()) - System.currentTimeMillis();
            if (sleepTime <= 0) sleepTime = 0;

            try {
                Thread.sleep(sleepTime);
            } catch (java.lang.InterruptedException e) {
            }
            lastWakeTimeMillis = System.currentTimeMillis();

            if (exitFlag) {
                this.info("stop()");
                // set WAN status back to true if test is shutting down
                this.monitor.wanStateChange(testSettings.getInterfaceId(), true);
                return;
            }

            // run the test
            TestResult results = _runTest(this.testSettings);
            this.debug(results.toString());

            // append the test results to the end of the list and prune as necessary
            resultList.addFirst(results.success);
            while (resultList.size() > testSettings.getTestHistorySize())
                resultList.removeLast();

            // update statistics
            totalTestsRun++;
            if (results.success) totalTestsPassed++;
            else totalTestsFailed++;

            // check to determine the status of the WAN
            int failureTestCount = 0;
            boolean wanStatus = true;
            for (Boolean result : resultList) {
                if (!result) failureTestCount++;
            }
            if (failureTestCount >= testSettings.getFailureThreshold()) wanStatus = false;

            // check if the new status is different than the last run
            // if so, alert the monitor thread
            // if lastWanStatus is null, then we just started so send the event using initial state
            if (lastWanStatus == null || wanStatus != lastWanStatus) {
                if (wanStatus) {
                    this.warn("WAN CHANGE: active.");
                } else {
                    this.warn("WAN CHANGE: down.");
                }
                this.monitor.wanStateChange(testSettings.getInterfaceId(), wanStatus);
            }

            // log a test event
            this.app.logEvent(new WanFailoverTestEvent(testSettings.getInterfaceId(), intfConf.getName(), intfConf.getSystemDev(), testSettings.getDescription(), results.success));

            lastWanStatus = wanStatus;
        }
    }

    /**
     * Called to stop the wan failover tester
     */
    public void stop()
    {
        exitFlag = true;
    }

    /**
     * Caleld to run a WAN test
     * 
     * @param test
     *        The test to run
     * @return The test result
     */
    public static String runTest(WanTestSettings test)
    {
        TestResult result = _runTest(test);
        return result.message;
    }

    /**
     * Get the interface ID
     * 
     * @return The interface ID
     */
    protected Integer getInterfaceId()
    {
        return this.testSettings.getInterfaceId();
    }

    /**
     * Get the number of tests to run
     * 
     * @return The number of tests to run
     */
    protected int getTotalTestsRun()
    {
        return this.totalTestsRun;
    }

    /**
     * Get the number of tests that have passed
     * 
     * @return The number of tests that have passed
     */
    protected int getTotalTestsPassed()
    {
        return this.totalTestsPassed;
    }

    /**
     * Get the number of tests that have failed
     * 
     * @return The number of tests that have failed
     */
    protected int getTotalTestsFailed()
    {
        return this.totalTestsFailed;
    }

    /**
     * Run a WAN test
     * 
     * @param test
     *        The test to run
     * @return The test result
     */
    private static TestResult _runTest(WanTestSettings test)
    {
        return _runTest(test.getType(), test.getInterfaceId(), test.getTimeoutMilliseconds(), test.getPingHostname(), test.getHttpUrl());
    }

    /**
     * Run a WAN test
     * 
     * @param testType
     *        The type of test
     * @param interfaceId
     *        The interface ID
     * @param timeoutMs
     *        The timeout
     * @param pingHost
     *        The ping host
     * @param httpUrl
     *        The URL target
     * @return The test result
     */
    private static TestResult _runTest(String testType, Integer interfaceId, Integer timeoutMs, String pingHost, String httpUrl)
    {
        TestResult result = new TestResult();
        result.success = false;
        result.message = I18nUtil.marktr("Test Failed");
        result.output = "";
        String osName;

        if (testType == null || interfaceId == null || timeoutMs == null) {
            result.message = "Invalid arguments";
            return result;
        }

        InterfaceSettings ic = UvmContextFactory.context().networkManager().findInterfaceId(interfaceId);
        if (ic == null) {
            result.message = "Invalid Interface: " + interfaceId;
            return result;
        }
        osName = ic.getSystemDev();

        try {
            if ("arp".equals(testType)) {
                result.output = WanFailoverApp.execManager.execOutput(ARP_TEST + " " + String.valueOf(interfaceId) + " " + osName + " " + String.valueOf(timeoutMs));
            }

            else if ("ping".equals(testType)) {
                if (pingHost == null) {
                    result.message = "Error: Missing ping hostname";
                    return result;
                }
                pingHost = pingHost.trim();
                if (pingHost.length() == 0) {
                    result.message = "Error: Empty ping hostname";
                    return result;
                }
                result.output = WanFailoverApp.execManager.execOutput(PING_TEST + " " + String.valueOf(interfaceId) + " " + osName + " " + String.valueOf(timeoutMs) + " " + pingHost);
            }

            else if ("dns".equals(testType)) {
                result.output = WanFailoverApp.execManager.execOutput(DNS_TEST + " " + String.valueOf(interfaceId) + " " + osName + " " + String.valueOf(timeoutMs));
            }

            else if ("http".equals(testType)) {
                if (httpUrl == null) {
                    result.message = "Error: Missing URL";
                    return result;
                }
                httpUrl = httpUrl.trim();
                if (httpUrl.length() == 0) {
                    result.message = "Error: Empty URL";
                    return result;
                }
                result.output = WanFailoverApp.execManager.execOutput(HTTP_TEST + " " + String.valueOf(interfaceId) + " " + osName + " " + String.valueOf(timeoutMs) + " " + httpUrl);
            }

            else {
                logger.warn("Unknown test type: " + testType);
            }

            if (result.output.length() > 0) {
                logger.debug("Test failed with message: " + result.output);
                result.message = result.output;
                result.success = false;
            } else {
                result.message = I18nUtil.marktr("Test Successful.");
                result.success = true;
            }

        } catch (Exception e) {
            logger.warn("Unable to run test script.", e);
            result.success = false;
            result.message = I18nUtil.marktr("Unexpected error while running test.");
        }

        return result;
    }

    /**
     * Log a test warning message
     * 
     * @param text
     *        The message text
     */
    private void warn(String text)
    {
        logger.warn("Tester( " + this.testSettings.getInterfaceId() + ", " + this.testSettings.getType() + " ): " + text);
    }

    /**
     * Log a test information message
     * 
     * @param text
     *        The message text
     */
    private void info(String text)
    {
        logger.info("Tester(" + this.testSettings.getInterfaceId() + ", " + this.testSettings.getType() + "): " + text);
    }

    /**
     * Log a test debug message
     * 
     * @param text
     *        The message text
     */
    private void debug(String text)
    {
        logger.debug("Tester(" + this.testSettings.getInterfaceId() + ", " + this.testSettings.getType() + "): " + text);
    }

    /**
     * Used to store the results of a WAN test
     */
    protected static class TestResult
    {
        /**
         * Constructor
         */
        public TestResult()
        {
        }

        public String message;
        public String output;
        public Boolean success;

        /**
         * Return a test result as a string
         * 
         * @return Result in string format
         */
        public String toString()
        {
            return "Results ( Success: " + success + " Message: \"" + message + "\" Output: \"" + output + "\" )";
        }
    }
}