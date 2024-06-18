/**
 * $Id$
 */
package com.untangle.uvm;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Selects logging context based on the fileName String.
 */
public class UvmContextSelector implements ContextSelector {

    private final ConcurrentHashMap<String, LoggerContext> contexts = new ConcurrentHashMap<>();
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private final ReentrantLock lock = new ReentrantLock();
    public static final String UVM_LOG = "uvm";
    private static final UvmContextSelector INSTANCE;
    private static final ThreadLocal<String> THREAD_LOG_INFO = new InheritableThreadLocal<>();
    ;

    static {
        INSTANCE = new UvmContextSelector();
    }

    /**
     * instance() provides the UvmContextSelector singleton
     *
     * @return UvmContextSelector
     */
    public static UvmContextSelector instance() {
        return INSTANCE;
    }

    /**
     * provides the LoggerContext based on logFileName
     *
     * @param fqcn
     * @param loader
     * @param currentContext
     * @return LoggerContext
     */
    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
        lock.lock();
        try {
            String contextName = THREAD_LOG_INFO.get();
            if (!contexts.containsKey(contextName)) {
                UvmHierarchy context = new UvmHierarchy(contextName, contextName);
                context.start();
                contexts.put(contextName, context);
            }
            return contexts.get(contextName);
        } catch (Exception e) {
            LOGGER.error("Exception: ", e);
        } finally {
            lock.unlock();
        }
        return contexts.get(UVM_LOG);
    }

    /**
     * provides the LoggerContext based on logFileName
     *
     * @param fqcn
     * @param loader
     * @param currentContext
     * @param configLocation
     * @return LoggerContext
     */
    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
        return getContext(fqcn, loader, currentContext);
    }

    /**
     * Removes the LoggerContext
     *
     * @param context
     */
    @Override
    public void removeContext(LoggerContext context) {
        lock.lock();
        try {
            LoggerContext remove = contexts.remove(context.getName());
        } finally {
            lock.unlock();
        }
    }

    /**
     * returns all LoggerContexts in a unmodifiable list
     *
     * @return List<LoggerContext>
     */
    @Override
    public List<LoggerContext> getLoggerContexts() {
        Collection<LoggerContext> values = contexts.values();
        return new ArrayList<>(values);
    }

    /**
     * Set the current thread's logging config to the "App" settings
     *
     * @param appId
     */
    public void setLoggingApp(Long appId) {
        this.setThreadLoggingInformation("app-" + appId.toString());
    }

    /**
     * Set the current thread's logging config to the "UVM" settings
     */
    public void setLoggingUvm() {
        this.setThreadLoggingInformation("uvm");
    }

    /**
     * Sets the current thread's logging config
     *
     * @param fileName
     */
    private void setThreadLoggingInformation(String fileName) {
        THREAD_LOG_INFO.set(fileName);
    }

    /**
     * Causes all logging repositories to reconfigure themselves from
     * the configuration file specified in the {@link UvmLoggingContext}.
     */
    public void reconfigureAll() {
        // synchronized (repositories) {
        //     for (UvmHierarchy hier : repositories.values()) {
        //         hier.configure();
        //     }
        // }
    }

    /**
     * A {@link org.apache.logging.log4j.core.LoggerContext} that associates the
     * current {@link UvmLoggingContext} and allows configuration
     * based on the contexts configuration file.
     */
    private class UvmHierarchy extends LoggerContext {
        private final String contextName;

        /**
         * UvmHierarchy
         *
         * @param name
         * @param contextName
         */
        public UvmHierarchy(String name, String contextName) {
            super(name);
            this.contextName = contextName;
        }

        /**
         * reconfigure
         */
        @Override
        public void reconfigure() {
            URI configLocation = null;
            ConfigurationSource source = null;
            try {
                configLocation = getClass().getClassLoader().getResource("log4j.xml").toURI();
                source = new ConfigurationSource(configLocation.toURL().openStream(), new File(configLocation));
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
            // Initialize the configuration
            Configuration configuration = new XmlConfiguration(this, source);
            configuration.initialize();
            Configuration config = updateConfiguration(configuration, contextName);
            this.setConfiguration(config);
        }

        /**
         * provides the LoggerContext based on logFileName
         *
         * @param config      config
         * @param contextName contextName
         * @return LoggerContext
         */
        private Configuration updateConfiguration(Configuration config, String contextName) {
            Appender oldAppender = config.getAppender("SYSLOG");

            if (oldAppender instanceof SyslogAppender) {
                SyslogAppender oldSyslogAppender = (SyslogAppender) oldAppender;

                // Create a new PatternLayout with the desired pattern
                PatternLayout newPatternLayout;
                Facility facility;
                if (contextName != null && !contextName.equals("uvm")) {
                    newPatternLayout = PatternLayout.newBuilder().withPattern(contextName + ": [%c{1}:%L] &lt;%X{SessionID}&gt; %-5p %m%n").withConfiguration(config).build();
                    facility = Facility.LOCAL1;
                } else {
                    newPatternLayout = PatternLayout.newBuilder().withPattern("uvm: [%c{1}:%L] %-5p %m%n").withConfiguration(config).build();
                    facility = Facility.LOCAL0;
                }

                // Create a new SyslogAppender with the new PatternLayout
                SyslogAppender newSyslogAppender = SyslogAppender.newSyslogAppenderBuilder().setName(oldSyslogAppender.getName()).setConfiguration(config).setProtocol(Protocol.UDP).setHost("localhost").setPort(514).setLayout(newPatternLayout).setFacility(facility).build();

                // Stop the old SyslogAppender
                oldSyslogAppender.stop();

                // Remove the old SyslogAppender from configuration
                config.getAppenders().remove(oldSyslogAppender.getName());

                // Add the new SyslogAppender to configuration
                config.addAppender(newSyslogAppender);

                // Replace the appender references in the loggers
                for (LoggerConfig loggerConfig : config.getLoggers().values()) {
                    loggerConfig.removeAppender(oldSyslogAppender.getName());
                    loggerConfig.addAppender(newSyslogAppender, null, null);
                }
                this.updateLoggers();

                // Start the new SyslogAppender
                newSyslogAppender.start();
            }
            return config;
        }

    }
}














