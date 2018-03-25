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

    public LoggingManagerImpl() { }

    public void setLoggingApp(Long appId)
    {
        UvmRepositorySelector.instance().setLoggingApp( appId );
    }

    public void setLoggingUvm()
    {
        UvmRepositorySelector.instance().setLoggingUvm();
    }

    public void resetAllLogs()
    {
        UvmRepositorySelector.instance().reconfigureAll();
    }
}
