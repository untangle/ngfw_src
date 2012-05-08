/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LoggingSettings;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.logging.LogEvent;

/**
 * Manages event logging.
 */
class LoggingManagerImpl implements LoggingManager
{
    private final List<String> initQueue = new LinkedList<String>();

    private final Logger logger = Logger.getLogger(getClass());

    private LoggingSettings loggingSettings;

    private volatile boolean conversionComplete = true;

    public LoggingManagerImpl()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from LoggingSettings ls");
                    loggingSettings = (LoggingSettings)q.uniqueResult();

                    if (null == loggingSettings) {
                        loggingSettings = new LoggingSettings();
                        s.save(loggingSettings);
                    }

                    return true;
                }
            };
        UvmContextFactory.context().runTransaction(tw);

        SyslogManagerImpl.manager().reconfigure(loggingSettings);
    }

    public LoggingSettings getLoggingSettings()
    {
        return loggingSettings;
    }

    public void setLoggingSettings(final LoggingSettings loggingSettings)
    {
        this.loggingSettings = loggingSettings;

        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    s.merge(loggingSettings);
                    return true;
                }
            };

        UvmContextFactory.context().runTransaction(tw);

        SyslogManagerImpl.manager().reconfigure(loggingSettings);
    }

    public void setLoggingNode(Long nodeId)
    {
        UvmRepositorySelector.instance().setLoggingNode( nodeId );
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
