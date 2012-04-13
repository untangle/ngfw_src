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
import com.untangle.uvm.logging.LogWorker;
import com.untangle.uvm.logging.LogWorkerFacility;
import com.untangle.uvm.logging.LoggingSettings;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.logging.LogEvent;

/**
 * Manages event logging.
 */
class LoggingManagerImpl implements LoggingManager
{
    private static final boolean LOGGING_DISABLED = Boolean.parseBoolean(System.getProperty("uvm.logging.disabled"));

    private final List<String> initQueue = new LinkedList<String>();

    private final Logger logger = Logger.getLogger(getClass());

    private LoggingSettings loggingSettings;

    private LogWorker logWorker = null;
    private long lastLoggedWarningTime = System.currentTimeMillis();
    
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
        UvmRepositorySelector.instance().reconfigureAll();
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

    public void logEvent(LogEvent evt)
    {
        if (this.logWorker == null)
            getLogWorker();
        if (this.logWorker == null)
            return;

        this.logWorker.logEvent(evt);
    }
    
    public boolean isConversionComplete()
    {
        return conversionComplete;
    }

    // package protected methods ----------------------------------------------

    void initSchema(final String name)
    {
        synchronized (initQueue) {
            conversionComplete = false;

            initQueue.add(name);
        }

        new Thread(new Runnable() {
                public void run()
                {
                    UvmContextImpl uvmContext = UvmContextImpl.getInstance();
                    uvmContext.waitForStartup();

                    synchronized (initQueue) {
                        if (initQueue.size() > 0) {
                            for (Iterator<String> i = initQueue.iterator(); i.hasNext(); ) {
                                String n = i.next();
                                i.remove();
                                logger.info("Initializeing events schema: \"" + n + "\"");
                                uvmContext.schemaUtil().initSchema("events", n);
                            }
                            uvmContext.schemaUtil().close();

                            initQueue.notifyAll();
                        }
                        conversionComplete = true;
                    }
                }
            }).start();
    }

    private void getLogWorker()
    {
        synchronized(this) {
            if (this.logWorker == null) {
                try {
                    LogWorkerFacility reports = (LogWorkerFacility) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
                    if (reports == null) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("Reporting node not found, discarding event");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }

                    this.logWorker = reports.getLogWorker();
                    if (this.logWorker == null) {
                        if (System.currentTimeMillis() - this.lastLoggedWarningTime > 10000) {
                            logger.warn("LogWorker node not found, discarding event");
                            this.lastLoggedWarningTime = System.currentTimeMillis();
                        }
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Unable to initialize logWorker", e);
                    return;
                }
            }
        }
    }
}
