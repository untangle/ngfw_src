/**
 * $Id$
 */
package com.untangle.uvm;

import org.apache.logging.log4j.ThreadContext;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects logging context based on the fileName String.
 */
public class UvmContextSelector implements ContextSelector {

    public static final String DEFAULT_LOG = "uvm";
    private static final UvmContextSelector INSTANCE;

    private final ConcurrentHashMap<String, LoggerContext> contextMap = new ConcurrentHashMap<>();

    // private final Map<String, UvmHierarchy> repositories;
    // private final ThreadLocal<String> threadLogInfo;


    static {
        INSTANCE = new UvmContextSelector();
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
        // String fileName = ThreadContext.get("logFileName");
        // if (fileName != null) {
        //     try {
        //         URI configURI = new URI(fileName);
        //         return contextMap.computeIfAbsent(fileName, key -> {
        //             try {
        //                 URL configURL = configURI.toURL();
        //                 File configFile = new File(configURI);
        //                 ConfigurationSource source = new ConfigurationSource(configURL.openStream(), configFile);
        //                 return Configurator.initialize(null, source).getContext();
        //                 // URL configURL = new URL(fileName);
        //                 // return Configurator.initialize(null, configURL);
        //             } catch (IOException e) {
        //                 e.printStackTrace();
        //                 return null;
        //             }
        //         });
        //     } catch (URISyntaxException e) {
        //         e.printStackTrace();
        //     }
        // }
        // return LoggerContext.getContext(currentContext);
        String appName = ThreadContext.get("appName");
        try{
            URL configURL = getClass().getClassLoader().getResource("log4j.xml");
            if (null == configURL) {
                throw new IllegalArgumentException("file not found!");
            }
            URI configURI = configURL.toURI();
            if(null != appName) {
                // Load the existing configuration file
                ConfigurationSource source = new ConfigurationSource(configURL.openStream(), new File(configURI));
                ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

                // Set the properties dynamically
                builder.setConfigurationSource(source);
                builder.addProperty("appLogFileName", appName);
                builder.addProperty("appendRef", appName == DEFAULT_LOG ? "UVMLOG" : "APPLOG") ;

                // Initialize the configuration
                BuiltConfiguration configuration = builder.build();
                LoggerContext context = Configurator.initialize(configuration);
            }
            return contextMap.computeIfAbsent(appName, k -> new LoggerContext(k, null, configURI));
        } catch (Exception e) {

        }
        return null;
        // return LoggerContext.getContext(currentContext);
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
        System.out.println(configLocation);
        return getContext(fqcn, loader, currentContext);
    }

    /**
     * Removes the LoggerContext
     * @param context
     */
    @Override
    public void removeContext(LoggerContext context) {
        contextMap.values().remove(context);
        context.stop();
    }

    /**
     * returns all LoggerContexts in a unmodifiable list
     * @return List<LoggerContext>
     */
    @Override
    public List<LoggerContext> getLoggerContexts() {
        return Collections.unmodifiableList(contextMap.values().stream().toList());
    }

    /**
     * Set the current thread's logging config to the "App" settings
     * @param appId
     */
    public void setLoggingApp(Long appId) {
        ThreadContext.put("appName", "app-" + appId.toString());
    }

    /**
     * Set the current thread's logging config to the "UVM" settings
     */
    public void setLoggingUvm() {
        // String filePath = "file:/usr/share/untangle/conf/log4j.xml";
        ThreadContext.put("appName", DEFAULT_LOG);
    }

    /**
     * Sets the current thread's logging config
     * @param fileName
     */
    // private void setThreadLoggingInformation(String fileName) {
    //     threadLogInfo.set(fileName);
    // }
    
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














