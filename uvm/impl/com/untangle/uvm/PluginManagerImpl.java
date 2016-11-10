/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import org.apache.log4j.Logger;

/**
 * The plugin manager manages the dynamically loadable class files "plugins" found in the
 * plugins directory
 */
public class PluginManagerImpl implements PluginManager
{
    private final Logger logger = Logger.getLogger(PluginManagerImpl.class);

    /**
     * The singleton instance
     */
    private static final PluginManagerImpl INSTANCE = new PluginManagerImpl();

    private HashMap<String,Plugin> loadedPlugins = new HashMap<String,Plugin>();
    
    private PluginManagerImpl() {}

    public synchronized static PluginManagerImpl getInstance()
    {
        return INSTANCE;
    }
    
    /**
     * This finds all the class files (excluding inner classes)
     * in the plugins directory and calls loadPlugin on each class
     */
    public void loadPlugins()
    {
        try {
            String pathStr = System.getProperty("uvm.lib.dir") + "/plugins";
            Files.walk(Paths.get(pathStr))
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("$"))
                .forEach(path -> {
                        String name = path.toString();
                        name = name.substring(0, name.lastIndexOf('.')); // remove file extension
                        name = name.replaceAll("/","."); // replace / with . to deduce class name
                        name = name.substring(name.lastIndexOf("com.")); // remove first part of path
                        loadPlugin( name );
                    });
        } catch (Throwable t) {
            logger.warn("Extension exception: ", t);
        }
    }

    /**
     * Unload the plugin (if it exists)
     */
    public void unloadPlugin( String className )
    {
        try {
            Plugin previousInstance = loadedPlugins.get( className );
            if ( previousInstance != null ) {
                logger.info("Unloading plugin: " + className + "@" + previousInstance.hashCode());
                previousInstance.stop();
                loadedPlugins.remove( className );
                logger.info("Unloaded  plugin: " + className + "@" + previousInstance.hashCode());
            }
        } catch (Throwable t) {
            logger.warn("Extension exception: ", t);
        }
    }
    
    /**
     * This loads the specified class name
     * It will call the instance method to create an instance, then create a thread and call run()
     *
     * If the class has already been loaded it will stop the previous instance
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private void loadPlugin( String className )
    {
        unloadPlugin( className );

        try {
            logger.info("Loading   plugin: " + className);
            Class clazz = UvmContextImpl.getInstance().loadClass( className );
            if ( clazz == null ) {
                logger.warn("Failed to find ExamplePluginImpl");
            } else {
                Plugin plugin = (Plugin)clazz.getMethod("instance").invoke(null);
                logger.info("Loaded    plugin: " + className + "@" + plugin.hashCode());
                logger.info("Starting  plugin: " + className + "@" + plugin.hashCode());
                Thread thread = new Thread(plugin);
                thread.start();
                logger.info("Started   plugin: " + className + "@" + plugin.hashCode() + " " + thread);

                loadedPlugins.put( className, plugin );
            }
        } catch (Throwable t) {
            logger.warn("Extension exception: ", t);
        }
    }

}
