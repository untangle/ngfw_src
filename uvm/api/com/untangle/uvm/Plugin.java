/**
 * $Id: Plugin.java,v 1.00 2016/10/28 23:16:13 dmorris Exp $
 */
package com.untangle.uvm;

/**
 * A plugin is just runnable with a stop method added
 */
public interface Plugin extends Runnable
{
    // this must be define for all plugins
    // public static Plugin instance();

    public void stop();
}
