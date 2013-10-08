/**
 * $Id: UvmContextBase.java 31978 2012-05-17 22:38:16Z dmorris $
 */
package com.untangle.uvm.engine;

import java.util.Map;

/**
 * This class is for Main to manipulate UvmContext without resorting
 * to reflection and allowing package protection of sensitive methods.
 */
public abstract class UvmContextBase
{
    protected Main main;
    
    public abstract Map<String, String> getTranslations(String module);
    
    public abstract String getCompanyName();

    /**
     * Initialize the UVM, starting up base services.
     */
    protected abstract void init();

    /**
     * Do final initialization, begin processing traffic.
     */
    protected abstract void postInit();

    /**
     * Destroy the UVM, stopping all services.
     */
    protected abstract void destroy();
}
