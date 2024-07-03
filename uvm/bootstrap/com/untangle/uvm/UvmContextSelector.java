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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom ContextSelector implementation to create multiple logger context for apps
 */
public class UvmContextSelector implements ContextSelector {
    public static final String UVM_LOG = "uvm";
    private static final String APP_HYPHEN = "app-";
    private static final String LOG4J_XML = "log4j2.xml";
    private static final String SYSLOG = "SYSLOG";
    private static final String LOCALHOST = "localhost";
    private static final String APP_PATTERN_TEMPLATE = "CONTEXTNAME: [%c{1}] &lt;%X{SessionID}&gt; %-5p %m%n%uvm{CONTEXTNAME}";
    private static final String UVM_PATTERN_TEMPLATE = "uvm: [%c{1}] %-5p %m%n%uvm{CONTEXTNAME}";
    private static final String CONTEXTNAME = "CONTEXTNAME";

    private static final UvmContextSelector INSTANCE;
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private final ConcurrentMap<String, LoggerContext> loggerContexts = new ConcurrentHashMap<>();
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
            if(contextName == null)
                contextName = UVM_LOG;
            synchronized (loggerContexts) {
                if (!loggerContexts.containsKey(contextName)) {
                    UvmLoggerContext context = new UvmLoggerContext(contextName);
                    context.start();
                    loggerContexts.put(contextName, context);
                }
            }
            return loggerContexts.get(contextName);
        } catch (Exception e) {
            LOGGER.error("Exception: ", e);
        }
        return loggerContexts.get(UVM_LOG);
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
        synchronized (loggerContexts) {
            loggerContexts.remove(context.getName());
        }
    }

    /**
     * Overrides getLoggerContexts method of ContextSelector interface
     * @return List<LoggerContext>
     */
    @Override
    public List<LoggerContext> getLoggerContexts() {
        return new ArrayList<>(loggerContexts.values());
    }

    /**
     * Set the current thread's logging config to the "App" settings
     * @param appId appId
     */
    public void setLoggingApp(Long appId) {
        this.setThreadLoggingInformation(APP_HYPHEN + appId.toString());
    }

    /**
     * Set the current thread's logging config to the "UVM" settings
     */
    public void setLoggingUvm() {
        this.setThreadLoggingInformation(UVM_LOG);
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
         synchronized (loggerContexts) {
             for (LoggerContext loggerContext : loggerContexts.values()) {
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
                configLocation = getClass().getClassLoader().getResource(LOG4J_XML).toURI();
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
         * Updates the configuration according to contextName.
         * Removes current syslog appender and on the basis of contextName (appName) adds a new appender
         * finally updates the loggerConfigs
         * @param config config
         * @param contextName contextName
         */
        private void updateConfiguration(Configuration config, String contextName) {
            Appender oldAppender = config.getAppender(SYSLOG);

            if (oldAppender instanceof SyslogAppender) {
                SyslogAppender oldSyslogAppender = (SyslogAppender) oldAppender;

                // Creating a new PatternLayout based on contextName
                PatternLayout newPatternLayout;
                Facility facility;
                if (contextName != null && !contextName.equals(UVM_LOG)) {
                    newPatternLayout = PatternLayout.newBuilder()
                        .withPattern(APP_PATTERN_TEMPLATE.replaceAll(CONTEXTNAME, contextName))
                        .withAlwaysWriteExceptions(false)
                        .withConfiguration(config)
                        .build();
                    facility = Facility.LOCAL1;
                } else {
                    newPatternLayout = PatternLayout.newBuilder()
                        .withPattern(UVM_PATTERN_TEMPLATE.replaceAll(CONTEXTNAME, contextName))
                        .withAlwaysWriteExceptions(false)
                        .withConfiguration(config)
                        .build();
                    facility = Facility.LOCAL0;
                }
                // Creating a new SyslogAppender with the new PatternLayout
                SyslogAppender newSyslogAppender = SyslogAppender.newSyslogAppenderBuilder()
                        .setName(oldSyslogAppender.getName())
                        .setConfiguration(config)
                        .setProtocol(Protocol.UDP)
                        .setHost(LOCALHOST)
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