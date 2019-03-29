/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Selects logging repository based on the fileName String.
 */
public class UvmRepositorySelector implements RepositorySelector
{
    private static final UvmRepositorySelector INSTANCE;

    private final Map<String, UvmHierarchy> repositories;
    private final ThreadLocal<String> threadLogInfo;

    public static final String DEFAULT_LOG = "uvm";
    
    /**
     * UvmRepositorySelector constructor
     * Use instance() go get the singleton instance
     */
    private UvmRepositorySelector()
    {
        repositories = new HashMap<>();
        threadLogInfo = new InheritableThreadLocal<>();
    }

    /**
     * instance() provides the UvmRepositorySelector singleton
     * @return UvmRepositorySelector
     */
    public static UvmRepositorySelector instance()
    {
        return INSTANCE;
    }

    /**
     * RepositorySelector method, log4j leverages this
     * @return LoggerRepository
     */
    public LoggerRepository getLoggerRepository()
    {
        String fileName = threadLogInfo.get();
        if (fileName == null)
            fileName = DEFAULT_LOG;
        
        UvmHierarchy hier;

        synchronized (repositories) {
            hier = repositories.get(fileName);
            if (hier == null) {
                hier = new UvmHierarchy(fileName);
                hier.configure();
                repositories.put(fileName, hier);
            }
        }

        return hier;
    }

    /**
     * Set the current thread's logging config to the "App" settings
     * @param appId
     */
    public void setLoggingApp(Long appId)
    {
        this.setThreadLoggingInformation("app-" + appId.toString());
    }

    /**
     * Set the current thread's logging config to the "UVM" settings
     */
    public void setLoggingUvm()
    {
        this.setThreadLoggingInformation("uvm");
    }
    
    /**
     * Causes all logging repositories to reconfigure themselves from
     * the configuration file specified in the {@link UvmLoggingContext}.
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
     * Sets the current thread's logging config
     * @param fileName
     */
    private void setThreadLoggingInformation(String fileName)
    {
        threadLogInfo.set(fileName);
    }

    /**
     * A {@link org.apache.log4j.Hierarchy} that associates the
     * current {@link UvmLoggingContext} and allows configuration
     * based on the contexts configuration file.
     */
    private class UvmHierarchy extends Hierarchy
    {
        private final String fileName;

        /**
         * UvmHierarchy
         * @param fileName
         */
        UvmHierarchy(String fileName)
        {
            super(new RootLogger(Level.DEBUG));

            this.fileName = fileName;
        }

        /**
         * Reads the data from a stream and returns it as a string
         * UTF-8
         * @param is - input stream
         * @return String
         * @throws IOException
         */
        public String convertStreamToString(InputStream is) throws java.io.IOException
        {
            if (is != null) {
                Writer writer = new StringWriter();
 
                char[] buffer = new char[1024];
                try {
                    Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
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

        /**
         * configure
         */
        void configure()
        {
            InputStream is = getClass().getClassLoader().getResourceAsStream("log4j.xml");
            if (null == is) {
                LogLog.warn("could not open: log4j.xml");
                return;
            }

            DOMConfigurator configurator = new DOMConfigurator();
            if (fileName != null) {
                try {
                    String fileStr = convertStreamToString(is);

                    fileStr = fileStr.replace("@AppLogFileName@", this.fileName);

                    /* change the default appender for app logs */
                    if (!this.fileName.equals("uvm"))
                        fileStr = fileStr.replace("ref=\"UVMLOG\"", "ref=\"APPLOG\"");
                        
                    InputStream newInputStream = new ByteArrayInputStream(fileStr.getBytes("UTF-8"));

                    configurator.doConfigure(newInputStream, this);

                    this.setThrowableRenderer(new UtThrowableRenderer(this.fileName + ": "));

                    //System.out.println("NEW HIER: " + fileName + " = " + fileStr);
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

    /**
     * UtThrowableRenderer
     */
    private class UtThrowableRenderer implements org.apache.log4j.spi.ThrowableRenderer
    {
        private String prefix;

        /**
         * UtThrowableRenderer
         * @param prefix
         */
        public UtThrowableRenderer(String prefix)
        {
            this.prefix = prefix;
        }

        /**
         * doRender - prints the info and stack trace of a throwable
         * @param t - throwable
         * @return String[]
         */
        public String[] doRender(Throwable t)
        {
            LinkedList<String> l = new LinkedList<>();
            l.add(this.prefix + "      " + t.toString());
            for (StackTraceElement ste: t.getStackTrace()) {
                l.add(this.prefix + "      " + ste.toString());
            }
            return l.toArray(new String[0]);
        }

    }

}
 
