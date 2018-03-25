/**
 * $Id: PluginManager.java,v 1.00 2016/10/28 11:46:12 dmorris Exp $
 */
package com.untangle.uvm;

public interface PluginManager
{
    void loadPlugins();

    void unloadPlugin(String className);

    Plugin getPlugin(String className);
}
