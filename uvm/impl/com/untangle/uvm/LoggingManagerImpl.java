/**
 * $Id$
 */

package com.untangle.uvm;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.logging.LoggingManager;

/**
 * Manages event logging.
 */
public class LoggingManagerImpl implements LoggingManager
{
    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Constructor
     */
    public LoggingManagerImpl()
    {
    }

    /**
     * Set the app logging ID
     * 
     * @param appId
     *        The ID value
     */
    public void setLoggingApp(Long appId)
    {
        UvmContextSelector.instance().setLoggingApp(appId);
    }

    /**
     * Set the logging UVM instance
     */
    public void setLoggingUvm()
    {
        UvmContextSelector.instance().setLoggingUvm();
    }

    /**
     * Reset all logs
     */
    public void resetAllLogs()
    {
        UvmContextSelector.instance().reconfigureAll();
    }
}
