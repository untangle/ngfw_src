/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.logging.LogEvent;

/**
 * Manages event logging.
 */
public class LoggingManagerImpl implements LoggingManager
{
    private final Logger logger = Logger.getLogger(getClass());

    public LoggingManagerImpl() { }

    public void setLoggingApp(Long nodeId)
    {
        UvmRepositorySelector.instance().setLoggingApp( nodeId );
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
