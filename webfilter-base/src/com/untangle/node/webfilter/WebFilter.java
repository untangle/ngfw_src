/**
 * $Id$
 */
package com.untangle.node.webfilter;

import java.util.List;

import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

/**
 * Interface the the WebFilter Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface WebFilter extends Node
{
    WebFilterSettings getSettings();
    void setSettings(WebFilterSettings settings);

    List<GenericRule> getPassedUrls();
    void setPassedUrls(List<GenericRule> passedUrls);

    List<GenericRule> getPassedClients();
    void setPassedClients(List<GenericRule> passedClients);

    List<GenericRule> getBlockedUrls();
    void setBlockedUrls(List<GenericRule> blockedUrls);

    List<GenericRule> getBlockedMimeTypes();
    void setBlockedMimeTypes(List<GenericRule> blockedMimeTypes);

    List<GenericRule> getBlockedExtensions();
    void setBlockedExtensions(List<GenericRule> blockedExtensions);

    List<GenericRule> getCategories();
    void setCategories(List<GenericRule> newCategories);

    WebFilterBlockDetails getDetails(String nonce);

    boolean unblockSite(String nonce, boolean global);

    String getUnblockMode();

    String getName();

    EventLogQuery[] getEventQueries();
}
