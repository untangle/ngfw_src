/**
 * $Id: PluginManager.java,v 1.00 2016/10/28 11:46:12 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.File;

public interface PluginManager
{
    void loadPlugins();

    void unloadPlugin( String className );
}
