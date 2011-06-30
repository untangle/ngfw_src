/*
 * $Id$
 */
package com.untangle.node.webfilter;

import java.util.List;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.Validator;

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

    List<IPMaddrRule> getPassedClients(int start, int limit, String... sortColumns);
    void setPassedUrls(List<GenericRule> passedUrls);

    List<GenericRule> getPassedUrls(int start, int limit, String... sortColumns);
    void setPassedClients(List<IPMaddrRule> passedClients);

    List<GenericRule> getBlockedUrls(int start, int limit, String... sortColumns);
    void setBlockedUrls(List<GenericRule> blockedUrls);

    List<GenericRule> getBlockedMimeTypes(int start, int limit, String... sortColumns);
    void setBlockedMimeTypes(List<GenericRule> blockedMimeTypes);

    List<GenericRule> getBlockedExtensions(int start, int limit, String... sortColumns);
    void setBlockedExtensions(List<GenericRule> blockedExtensions);

    List<GenericRule> getCategories(int start, int limit, String... sortColumns);
    void setCategories(List<GenericRule> newCategories);

    Validator getValidator();

    WebFilterBlockDetails getDetails(String nonce);

    boolean unblockSite(String nonce, boolean global);

    String getUnblockMode();

    EventManager<WebFilterEvent> getEventManager();

    EventManager<UnblockEvent> getUnblockEventManager();

}
