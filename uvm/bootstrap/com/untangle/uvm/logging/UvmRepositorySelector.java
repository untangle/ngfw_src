/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Selects logging repository based on the LoggingInformation.
 */
public class UvmRepositorySelector implements RepositorySelector
{
    private static final UvmRepositorySelector INSTANCE;

    private final Map<LoggingInformation, UvmHierarchy> repositories;
    private final ThreadLocal<LoggingInformation> threadLogInfo;

    private static final LoggingInformation DEFAULT_LOGGING_INFO = new LoggingInformation("log4j-uvm.xml", "uvm" );
    
    private UvmRepositorySelector()
    {
        repositories = new HashMap<LoggingInformation, UvmHierarchy>();
        threadLogInfo = new InheritableThreadLocal<LoggingInformation>();
    }

    public static UvmRepositorySelector instance()
    {
        return INSTANCE;
    }

    public LoggerRepository getLoggerRepository()
    {
        LoggingInformation logInfo = threadLogInfo.get();
        if (logInfo == null)
            logInfo = DEFAULT_LOGGING_INFO;
        
        UvmHierarchy hier;

        synchronized (repositories) {
            hier = repositories.get(logInfo);
            if (hier == null) {
                hier = new UvmHierarchy(logInfo);
                hier.configure();
                repositories.put(logInfo, hier);
            }
        }

        return hier;
    }

    /**
     * Causes all logging repositories to reconfigure themselves from
     * the configuration file specified in the {@link
     * UvmLoggingContext}.
     */
    public void reconfigureAll()
    {
        synchronized (repositories) {
            for (UvmHierarchy hier : repositories.values()) {
                hier.configure();
            }
        }
    }

    /**
     * Sets the current logging context factory.
     *
     * @param ctx the {@link UvmLoggingContextFactory} to use.
     */
    public void setThreadLoggingInformation(LoggingInformation logInfo)
    {
        threadLogInfo.set(logInfo);
    }

    /**
     * A {@link org.apache.log4j.Hierarchy} that associates the
     * current {@link UvmLoggingContext} and allows configuration
     * based on the contexts configuration file.
     */
    private class UvmHierarchy extends Hierarchy
    {
        private final LoggingInformation logInfo;

        UvmHierarchy(LoggingInformation logInfo)
        {
            super(new RootLogger(Level.DEBUG));

            this.logInfo = logInfo;
        }

        public String convertStreamToString(InputStream is) throws java.io.IOException
        {
            if (is != null) {
                Writer writer = new StringWriter();
 
                char[] buffer = new char[1024];
                try {
                    Reader reader = new BufferedReader(
                                                       new InputStreamReader(is, "UTF-8"));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                    is.close();
                }
                return writer.toString();
            } else {       
                return "";
            }
        }

        void configure()
        {
            String n = logInfo.getConfigName();
            InputStream is = getClass().getClassLoader().getResourceAsStream(n);
            if (null == is) {
                LogLog.warn("could not open: " + n);
                return;
            }

            DOMConfigurator configurator = new DOMConfigurator();
            if (logInfo != null) {
                try {
                    String fileStr = convertStreamToString(is);
                    fileStr = fileStr.replace("@NodeLogFileName@", logInfo.getFileName());
                    InputStream newInputStream = new ByteArrayInputStream(fileStr.getBytes("UTF-8"));
                    configurator.doConfigure(newInputStream, this);
                    this.setThrowableRenderer(new UtThrowableRenderer("node-" + logInfo.getFileName() + ": "));
                }
                catch (java.io.IOException e) {
                    System.err.println("Exceptiong configuring logging exception: " + e);
                }
            } else {
                configurator.doConfigure(is, this);
            }
        }
    }

    static {
        INSTANCE = new UvmRepositorySelector();
    }
}
 