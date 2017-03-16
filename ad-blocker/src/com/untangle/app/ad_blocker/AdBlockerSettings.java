/*
 * $Id$
 */
package com.untangle.app.ad_blocker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import com.untangle.uvm.app.GenericRule;

/**
 * The settings for Ad Blocker
 */
@SuppressWarnings("serial")
public class AdBlockerSettings implements Serializable
{
    private List<GenericRule> blockingRules = new LinkedList<GenericRule>();
    private List<GenericRule> userBlockingRules = new LinkedList<GenericRule>();
    private List<GenericRule> passingRules = new LinkedList<GenericRule>();
    private List<GenericRule> userPassingRules = new LinkedList<GenericRule>();
    private List<GenericRule> passedClients = new LinkedList<GenericRule>();
    private List<GenericRule> passedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> cookies = new LinkedList<GenericRule>();
    private List<GenericRule> userCookies = new LinkedList<GenericRule>();
    
    private String lastUpdate;

    private Boolean scanCookies = Boolean.TRUE;
    private Boolean scanAds = Boolean.TRUE;

    public AdBlockerSettings() {
    }

    /**
     * The timestamp read from easylist
     * @return
     */
    public String getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
    
    public Boolean getScanCookies() { return scanCookies; }
    public void setScanCookies(Boolean scanCookies) { this.scanCookies = scanCookies; }
    
    public Boolean getScanAds() { return scanAds; }
    public void setScanAds(Boolean scanAds) { this.scanAds = scanAds; }

    /**
     * The list IPs/subnets that are exempt from ad blocking
     * 
     * @return the set of Ad Blocker rules.
     */
    public List<GenericRule> getPassedClients() { return passedClients; }
    public void setPassedClients(List<GenericRule> passedClients) { this.passedClients = passedClients; }

    /**
     * The list of server URLs that are exempt from ad blocking
     * 
     * @return the set of Ad Blocker rules.
     */
    public List<GenericRule> getPassedUrls() { return passedUrls; }
    public void setPassedUrls(List<GenericRule> passedUrls) { this.passedUrls = passedUrls; }
    
    /**
     * Cookies.
     * 
     * @return the list of cookies.
     */
    public List<GenericRule> getCookies() { return cookies; }
    public void setCookies(List<GenericRule> cookies) { this.cookies = cookies; }

    public List<GenericRule> getUserCookies() { return userCookies; }
    public void setUserCookies(List<GenericRule> userCookies) { this.userCookies = userCookies; }

    /**
     * Ad Blocker combined set of rules.
     * 
     * @return the set of Ad Blocker rules.
     */
    public List<GenericRule> getRules()
    {
        List<GenericRule> result = new LinkedList<GenericRule>();
        result.addAll(blockingRules);
        result.addAll(passingRules);
        return result;
    }

    public void setRules(List<GenericRule> rules)
    {
        blockingRules = new LinkedList<GenericRule>();
        passingRules = new LinkedList<GenericRule>();
        for (GenericRule r : rules) {
            if (r.getBlocked() != null && !r.getBlocked()) {
                passingRules.add(r);
            } else {
                blockingRules.add(r);
            }
        }
    }

    public List<GenericRule> getUserRules()
    {
        List<GenericRule> result = new ArrayList<GenericRule>();
        result.addAll(userBlockingRules);
        result.addAll(userPassingRules);
        return result;
    }

    public void setUserRules(List<GenericRule> userRules)
    {
        userBlockingRules = new LinkedList<GenericRule>();
        userPassingRules = new LinkedList<GenericRule>();
        for (GenericRule r : userRules) {
            if (r.getBlocked() != null && !r.getBlocked()) {
                userPassingRules.add(r);
            } else {
                userBlockingRules.add(r);
            }
        }
    }

    
    /**
     * Ad Blocker blocking rules.
     * 
     * @return the set of Ad Blocker blocking rules.
     */
    public List<GenericRule> _getBlockingRules()
    {
        return blockingRules;
    }

    public void _setBlockingRules(List<GenericRule> blockingRules)
    {
        this.blockingRules = blockingRules;
    }

    public List<GenericRule> _getUserBlockingRules()
    {
        return userBlockingRules;
    }

    /**
     * The passingRules, as obtained from the easylist file
     * 
     * @return the passingRules
     */
    public List<GenericRule> _getPassingRules()
    {
        return passingRules;
    }

    public void _setPassingRules(List<GenericRule> passingRules)
    {
        this.passingRules = passingRules;
    }

    public List<GenericRule> _getUserPassingRules()
    {
        return userPassingRules;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
