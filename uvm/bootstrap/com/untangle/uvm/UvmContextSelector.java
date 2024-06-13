/**
 * $Id$
 */
package com.untangle.uvm;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.util.Loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Selects logging context based on the fileName String.
 */
public class UvmContextSelector implements ContextSelector {

    private final ConcurrentHashMap<String, LoggerContext> contexts = new ConcurrentHashMap<>();
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private final ReentrantLock lock = new ReentrantLock();
    public static final String DEFAULT_LOG = "default";
    public static final String UVM_LOG = "uvm";
    private static final UvmContextSelector INSTANCE;
    private final ThreadLocal<String> threadLogInfo;

    static {
        INSTANCE = new UvmContextSelector();
    }

    /**
     * UvmRepositorySelector constructor
     * Use instance() go get the singleton instance
     */
    private UvmContextSelector()
    {
        threadLogInfo = new InheritableThreadLocal<>();
    }

    /**
     * instance() provides the UvmContextSelector singleton
     * @return UvmContextSelector
     */
    public static UvmContextSelector instance() {
        return INSTANCE;
    }
    
    /**
     * provides the LoggerContext based on logFileName
     * @param fqcn
     * @param loader
     * @param currentContext
     * @return LoggerContext
     */
    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
        lock.lock();
        try{
            String contextName = ThreadContext.get("appName");
            if (contextName == null) {
                contextName = DEFAULT_LOG;
                this.setDefaultLogging();
            }

            return contexts.computeIfAbsent(contextName, key -> {
                LoggerContext context = new LoggerContext(key);
                URI configLocation = null;
                try {
                    configLocation = getClass().getClassLoader().getResource("log4j.xml").toURI();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                context.setConfigLocation(configLocation);
                return context;
            });
        } catch(Exception e) {
            LOGGER.error("Exception: ", e);
        } finally {
            lock.unlock();
        }
        return contexts.get(DEFAULT_LOG);
    }

    /**
     * provides the LoggerContext based on logFileName
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
     * @return List<LoggerContext>
     */
    @Override
    public List<LoggerContext> getLoggerContexts() {
        Collection<LoggerContext> values = contexts.values();
        return new ArrayList<>(values);
    }

    /**
     * Set the current thread's logging config to the "App" settings
     * @param appId
     */
    public void setLoggingApp(Long appId) {
        this.setThreadLoggingInformation("app-" + appId.toString());

        ThreadContext.put("appName", "app-" + appId.toString());
        ThreadContext.put("logType", "app");
    }

    /**
     * Set the current thread's logging config to the "UVM" settings
     */
    public void setLoggingUvm() {
        // String filePath = "file:/usr/share/untangle/conf/log4j.xml";
        this.setThreadLoggingInformation("uvm");

        ThreadContext.put("appName", UVM_LOG);
        ThreadContext.put("logType", UVM_LOG);
    }

    /**
     * Set the current thread's logging config to the "default" settings
     */
    public void setDefaultLogging() {
        this.setThreadLoggingInformation(DEFAULT_LOG);

        ThreadContext.put("appName", DEFAULT_LOG);
        ThreadContext.put("logType", null);
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
}














