/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.util.List;
import java.util.Date;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.EventLogQuery;

public interface Spyware extends Node
{
    SpywareSettings getSettings();

    void setSettings(SpywareSettings baseSettings);

    List<GenericRule> getCookies();
    
    void setCookies( List<GenericRule> newCookies );
    
    List<GenericRule> getSubnets();

    void setSubnets( List<GenericRule> newSubnets );
    
    List<GenericRule> getPassedUrls();

    void setPassedUrls( List<GenericRule> newPassedUrls );

    String getUnblockMode();
    
    SpywareBlockDetails getBlockDetails(String nonce);

    boolean unblockSite(String nonce, boolean global);
    
    EventLogQuery[] getCookieEventQueries();

    EventLogQuery[] getUrlEventQueries();

    EventLogQuery[] getSuspiciousEventQueries();

    Date getLastSignatureUpdate();
}
