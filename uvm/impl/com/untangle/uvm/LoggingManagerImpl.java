/**
 * $Id$
 */

package com.untangle.uvm;

import org.apache.log4j.Logger;

import com.untangle.uvm.logging.LoggingManager;

/**
 * Manages event logging.
 */
public class LoggingManagerImpl implements LoggingManager
{
    private final Logger logger = Logger.getLogger(getClass());

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
        UvmRepositorySelector.instance().setLoggingApp(appId);
    }

    /**
     * Set the logging UVM instance
     */
    public void setLoggingUvm()
    {
        UvmRepositorySelector.instance().setLoggingUvm();
    }

    /**
     * Reset all logs
     */
    public void resetAllLogs()
    {
        UvmRepositorySelector.instance().reconfigureAll();
    }
}
