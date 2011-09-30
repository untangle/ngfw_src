/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.util.List;
import java.util.Date;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.IPMaskedAddressRule;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.GenericRule;


public interface Spyware extends Node
{
    SpywareSettings getSettings();

    void setSettings(SpywareSettings baseSettings);

    public List<GenericRule> getCookies();
    
    public void setCookies( List<GenericRule> newCookies );
    
    public List<GenericRule> getSubnets();

    public void setSubnets( List<GenericRule> newSubnets );
    
    public List<GenericRule> getPassedUrls();

    public void setPassedUrls( List<GenericRule> newPassedUrls );

    SpywareBlockDetails getBlockDetails(String nonce);

    boolean unblockSite(String nonce, boolean global);

    String getUnblockMode();

    EventManager<SpywareEvent> getEventCookieManager();
    EventManager<SpywareEvent> getEventBlacklistManager();
    EventManager<SpywareEvent> getEventSuspiciousManager();

    public Date getLastSignatureUpdate();
}
