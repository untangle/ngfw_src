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

/**
 * Custom ContextSelector implementation to create multiple logger context for apps
 */
public class UvmContextSelector implements ContextSelector {

    private final ConcurrentHashMap<String, LoggerContext> contexts = new ConcurrentHashMap<>();
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    public static final String UVM_LOG = "uvm";
    private static final UvmContextSelector INSTANCE;
    private static final ThreadLocal<String> THREAD_LOG_INFO = new InheritableThreadLocal<>();

    static {
        INSTANCE = new UvmContextSelector();
    }

    /**
     * instance() Provides the UvmContextSelector singleton
     * @return UvmContextSelector
     */
    public static UvmContextSelector instance() {
        return INSTANCE;
    }

    /**
     * Overrides getContext method of ContextSelector interface
     * @param fqcn fqcn
     * @param loader loader
     * @param currentContext boolean isCurrentContext
     * @return LoggerContext
     */
    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
        try{
            String contextName = THREAD_LOG_INFO.get();
            synchronized (contexts) {
                if (!contexts.containsKey(contextName)) {
                    UvmLoggerContext context = new UvmLoggerContext(contextName);
                    context.start();
                    contexts.put(contextName, context);
                }
            }
            return contexts.get(contextName);
        } catch (Exception e) {
            LOGGER.error("Exception: ", e);
        }
        return contexts.get(UVM_LOG);
    }

    /**
     * Overrides getContext method of ContextSelector interface
     * @param fqcn fqcn
     * @param loader loader
     * @param currentContext boolean isCurrentContext
     * @param configLocation configLocation
     * @return LoggerContext
     */
    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
        return getContext(fqcn, loader, currentContext);
    }

    /**
     * Overrides removeContext method of ContextSelector interface
     * @param context LoggerContext
     */
    @Override
    public void removeContext(LoggerContext context) {
        synchronized (contexts) {
            LoggerContext remove = contexts.remove(context.getName());
        }
    }

    /**
     * Overrides getLoggerContexts method of ContextSelector interface
     * @return List<LoggerContext>
     */
    @Override
    public List<LoggerContext> getLoggerContexts() {
        Collection<LoggerContext> values = contexts.values();
        return new ArrayList<>(values);
    }

    /**
     * Set the current thread's logging config to the "App" settings
     * @param appId appId
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
     * Sets the current thread's logging context
     * @param contextName contextName
     */
    private void setThreadLoggingInformation(String contextName) {
        THREAD_LOG_INFO.set(contextName);
    }

    /**
     * Causes all logging contexts to reconfigure themselves from
     * the configuration context specified in the UvmLoggerContext.
     */
    public void reconfigureAll() {
         synchronized (contexts) {
             for (LoggerContext loggerContext : contexts.values()) {
                 loggerContext.reconfigure();
             }
         }
    }

    /**
     * A LoggerContext that associates the
     * current UvmLoggerContext and allows configuration
     * based on the contexts configuration file.
     */
    private static class UvmLoggerContext extends LoggerContext {
        /**
         * UvmLoggerContext constructor
         * @param name name
         */
        public UvmLoggerContext(String name) {
            super(name);
        }

        /**
         * Overrides reconfigure method of LoggerContext
         */
        @Override
        public void reconfigure() {
            URI configLocation = null;
            ConfigurationSource source = null;
            try {
                configLocation = getClass().getClassLoader().getResource("log4j.xml").toURI();
                source = new ConfigurationSource(configLocation.toURL().openStream(), new File(configLocation));
            } catch (URISyntaxException | IOException e) {
                LOGGER.error("Exception: ", e);
                throw new RuntimeException(e);
            }
            // Initialize the configuration
            Configuration configuration = new XmlConfiguration(this, source);
            configuration.initialize();
            updateConfiguration(configuration, this.getName());
            this.setConfiguration(configuration);
        }

        /**
         * Updates the configuration according to contextName
         * @param config config
         * @param contextName contextName
         */
        private void updateConfiguration(Configuration config, String contextName) {
            Appender oldAppender = config.getAppender("SYSLOG");

            if (oldAppender instanceof SyslogAppender oldSyslogAppender) {

                // Creating a new PatternLayout based on contextName
                PatternLayout newPatternLayout;
                Facility facility;
                if (contextName != null && !contextName.equals(UVM_LOG)) {
                    newPatternLayout = PatternLayout.newBuilder()
                        .withPattern(contextName + ": [%c{1}:%L] &lt;%X{SessionID}&gt; %-5p %m%n")
                        .withConfiguration(config)
                        .build();
                    facility = Facility.LOCAL1;
                } else {
                    newPatternLayout = PatternLayout.newBuilder()
                        .withPattern("uvm: [%c{1}:%L] %-5p %m%n")
                        .withConfiguration(config)
                        .build();
                    facility = Facility.LOCAL0;
                }
                // Creating a new SyslogAppender with the new PatternLayout
                SyslogAppender newSyslogAppender = SyslogAppender.newSyslogAppenderBuilder()
                        .setName(oldSyslogAppender.getName())
                        .setConfiguration(config)
                        .setProtocol(Protocol.UDP).setHost("localhost")
                        .setPort(514).setLayout(newPatternLayout)
                        .setFacility(facility)
                        .build();

                // Stop the old SyslogAppender and remove from configuration
                oldSyslogAppender.stop();
                config.getAppenders().remove(oldSyslogAppender.getName());
                // Add the new SyslogAppender to configuration
                config.addAppender(newSyslogAppender);

                // Replace the appender references in the loggers
                for (LoggerConfig loggers : config.getLoggers().values()) {
                    loggers.removeAppender(oldSyslogAppender.getName());
                    loggers.addAppender(newSyslogAppender, null, null);
                    loggers.setAdditive(false);
                }
                this.updateLoggers(config);
                // Start the new SyslogAppender
                newSyslogAppender.start();
            }
        }
    }
}














