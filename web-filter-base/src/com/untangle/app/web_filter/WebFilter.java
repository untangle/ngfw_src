/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.util.List;

import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.App;

/**
 * Interface the the WebFilter Node.
 *
 */
public interface WebFilter extends App
{
    WebFilterSettings getSettings();
    void setSettings(WebFilterSettings settings);

    List<GenericRule> getPassedUrls();
    void setPassedUrls(List<GenericRule> passedUrls);

    List<GenericRule> getPassedClients();
    void setPassedClients(List<GenericRule> passedClients);

    List<GenericRule> getBlockedUrls();
    void setBlockedUrls(List<GenericRule> blockedUrls);

    List<GenericRule> getCategories();
    void setCategories(List<GenericRule> newCategories);

    List<WebFilterRule> getFilterRules();
    void setFilterRules(List<WebFilterRule> newRules);

    WebFilterBlockDetails getDetails(String nonce);

    boolean unblockSite(String nonce, boolean global);

    String getUnblockMode();

    String getName();
}
