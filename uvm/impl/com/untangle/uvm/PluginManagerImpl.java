/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.FileVisitOption;

import org.apache.log4j.Logger;

/**
 * The plugin manager manages the dynamically loadable class files "plugins"
 * found in the plugins directory
 */
public class PluginManagerImpl implements PluginManager
{
    private final Logger logger = Logger.getLogger(PluginManagerImpl.class);

    /**
     * The singleton instance
     */
    private static final PluginManagerImpl INSTANCE = new PluginManagerImpl();

    private HashMap<String, Plugin> loadedPlugins = new HashMap<String, Plugin>();

    /**
     * Constructor
     */
    private PluginManagerImpl()
    {
    }

    /**
     * Create a URL class loader
     * 
     * @return
     */
    private URLClassLoader createClassLoader()
    {
        try {
            List<URL> urls = new ArrayList<URL>();

            /* Add everything in lib */
            File uvmLibDir = new File(System.getProperty("uvm.lib.dir"));
            for (File f : uvmLibDir.listFiles()) {
                URL url = f.toURI().toURL();
                urls.add(url);
            }

            urls.add(new URL("file://" + System.getProperty("uvm.lang.dir") + "/"));
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
        } catch (Throwable exn) {
            logger.warn("Failed to create pluginClassLoader", exn);
            return null;
        }
    }

    /**
     * Get our singleton instance
     * 
     * @return The instance
     */
    public synchronized static PluginManagerImpl getInstance()
    {
        return INSTANCE;
    }

    /**
     * This finds all the class files (excluding inner classes) in the plugins
     * directory and calls loadPlugin on each class
     */
    public void loadPlugins()
    {
        Stream<Path> walker = null;
        try {
            String pathStr = System.getProperty("uvm.lib.dir") + "/plugins";
            walker = Files.walk(Paths.get(pathStr), FileVisitOption.FOLLOW_LINKS);
            walker.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("$"))
                .filter(path -> path.toString().contains("Plugin"))
                .forEach(path -> {
                        String name = path.toString();
                        name = name.substring(0, name.lastIndexOf('.')); // remove file extension
                        name = name.replaceAll("/","."); // replace / with . to deduce class name
                        name = name.substring(name.lastIndexOf("com.")); // remove first part of path
                        loadPlugin( name );
                    });
        } catch (Throwable t) {
            logger.warn("Extension exception: ", t);
        }finally{
            if(walker != null){
                try{
                    walker.close();
                }catch(Exception e){
                    logger.warn(e);
                }
            }
        }
    }

    /**
     * Unload the plugin (if it exists)
     * 
     * @param className
     *        - the className of the plugin to unload
     */
    public void unloadPlugin(String className)
    {
        try {
            Plugin previousInstance = loadedPlugins.get(className);
            if (previousInstance != null) {
                logger.info("Unloading plugin: " + className + "@" + previousInstance.hashCode());
                previousInstance.stop();
                loadedPlugins.remove(className);
                logger.info("Unloaded  plugin: " + className + "@" + previousInstance.hashCode());
            }
        } catch (Throwable t) {
            logger.warn("Extension exception: ", t);
        }
    }

    /**
     * Get the plugin of the specified className
     * 
     * @param className
     *        - the className of the plugin
     * @return the plugin or null if not found
     */
    public Plugin getPlugin(String className)
    {
        return loadedPlugins.get(className);
    }

    /**
     * This loads the specified class name It will call the instance method to
     * create an instance, then create a thread and call run()
     * 
     * If the class has already been loaded it will stop the previous instance
     * 
     * @param className
     *        The class name
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void loadPlugin(String className)
    {
        unloadPlugin(className);

        try {
            logger.info("Loading   plugin: " + className);
            URLClassLoader urlClassLoader = createClassLoader();
            if(urlClassLoader != null){    
                Class clazz = urlClassLoader.loadClass(className);
                if (clazz == null) {
                    logger.warn("Failed to find ExamplePluginImpl");
                } else {
                    Plugin plugin = (Plugin) clazz.getMethod("instance").invoke(null);
                    logger.info("Loaded    plugin: " + className);
                    logger.info("Starting  plugin: " + className);
                    Thread thread = new Thread(plugin);
                    thread.start();
                    logger.info("Started   plugin: " + className + " " + thread);

                    loadedPlugins.put(className, plugin);
                }
            }
        } catch (Throwable t) {
            logger.warn("Extension exception: ", t);
        }
    }
}
