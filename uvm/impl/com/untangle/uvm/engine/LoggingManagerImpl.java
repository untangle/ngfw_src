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
import com.untangle.uvm.logging.UvmRepositorySelector;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.logging.LogEvent;

/**
 * Manages event logging.
 */
class LoggingManagerImpl implements LoggingManager
{
    private static final boolean LOGGING_DISABLED = Boolean.parseBoolean(System.getProperty("uvm.logging.disabled"));

    private final List<String> initQueue = new LinkedList<String>();
    private final LogWorker logWorker = new LogWorker(this);
    private final UvmRepositorySelector repositorySelector;

    private final Logger logger = Logger.getLogger(getClass());

    private LoggingSettings loggingSettings;

    private volatile boolean conversionComplete = true;

    LoggingManagerImpl(UvmRepositorySelector repositorySelector)
    {
        this.repositorySelector = repositorySelector;

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

    static boolean isLoggingDisabled()
    {
        return LOGGING_DISABLED;
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

    public void resetAllLogs()
    {
        repositorySelector.reconfigureAll();
    }

    public void logError(String errorText)
    {
        if (null == errorText) {
            logger.error("This is the default error text.");
        } else {
            logger.error(errorText);
        }

        return;
    }

    public void forceFlush()
    {
        this.logWorker.forceFlush();
    }
    
    public BlockingQueue<LogEvent> getInputQueue()
    {
        return logWorker.getInputQueue();
    }

    // package protected methods ----------------------------------------------

    void start()
    {
        logWorker.start();
    }

    void stop()
    {
        logWorker.stop();
    }

    void initSchema(final String name)
    {
        synchronized (initQueue) {
            conversionComplete = false;

            initQueue.add(name);
        }

        // XXX not using newThread, called from UvmContextImpl constructor
        new Thread(new Runnable()
            {
                public void run()
                {
                    UvmContextImpl mctx = UvmContextImpl.getInstance();
                    mctx.waitForStartup();

                    synchronized (initQueue) {
                        for (Iterator<String> i = initQueue.iterator(); i.hasNext(); ) {
                            String n = i.next();
                            i.remove();
                            mctx.schemaUtil().initSchema("events", n);
                        }

                        conversionComplete = true;
                        initQueue.notifyAll();
                    }
                }
            }).start();
    }

    boolean isConversionComplete()
    {
        return conversionComplete;
    }
}
