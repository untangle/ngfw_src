/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.util.List;

import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.App;

/**
 * Interface the the WebFilter App.
 * 
 */
public interface WebFilter extends App
{
    /**
     * Get the settings
     * 
     * @return The settings
     */
    WebFilterSettings getSettings();

    /**
     * Set the settings
     * 
     * @param settings
     *        The new settings
     */
    void setSettings(WebFilterSettings settings);

    /**
     * Get the passed URL list
     * 
     * @return The passed URL list
     */
    List<GenericRule> getPassedUrls();

    /**
     * Set the passed URL list
     * 
     * @param passedUrls
     *        The new list
     */
    void setPassedUrls(List<GenericRule> passedUrls);

    /**
     * Get the passed clients
     * 
     * @return The passed clients
     */
    List<GenericRule> getPassedClients();

    /**
     * Set the passed clients
     * 
     * @param passedClients
     *        The new list
     */
    void setPassedClients(List<GenericRule> passedClients);

    /**
     * Get the blocked URLs
     * 
     * @return The blocked URL's
     */
    List<GenericRule> getBlockedUrls();

    /**
     * Set the blocked URL's
     * 
     * @param blockedUrls
     *        The new list
     */
    void setBlockedUrls(List<GenericRule> blockedUrls);

    /**
     * Get the categories
     * 
     * @return The categories
     */
    List<GenericRule> getCategories();

    /**
     * Set the categories
     * 
     * @param newCategories
     *        The new list
     */
    void setCategories(List<GenericRule> newCategories);

    /**
     * Get the filter rules
     * 
     * @return The filter rules
     */
    List<WebFilterRule> getFilterRules();

    /**
     * Set the filter rules
     * 
     * @param newRules
     *        The new rules
     */
    void setFilterRules(List<WebFilterRule> newRules);

    /**
     * Get the details
     * 
     * @param nonce
     *        The nonce
     * @return The details for the nonce
     */
    WebFilterBlockDetails getDetails(String nonce);

    /**
     * Unblock a site
     * 
     * @param nonce
     *        The nonce
     * @param global
     *        Global flag
     * @return Result
     */
    boolean unblockSite(String nonce, boolean global);

    /**
     * Get the unblock mode
     * 
     * @return The unblock mode
     */
    String getUnblockMode();

    /**
     * Get the name
     * 
     * @return The name
     */
    String getName();
}
