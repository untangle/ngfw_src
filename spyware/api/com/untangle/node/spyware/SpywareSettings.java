/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.node.GenericRule;

/**
 * Settings for the Spyware node.
 */
@SuppressWarnings("serial")
public class SpywareSettings implements Serializable
{
    public static final String UNBLOCK_MODE_NONE   = "None";
    public static final String UNBLOCK_MODE_HOST   = "Host";
    public static final String UNBLOCK_MODE_GLOBAL = "Global";

    private Boolean scanCookies = Boolean.TRUE;
    private Boolean scanSubnets = Boolean.TRUE;
    private Boolean scanUrls  = Boolean.TRUE;

    private String unblockMode = UNBLOCK_MODE_NONE;

    private List<GenericRule> cookies = new LinkedList<GenericRule>();
    private List<GenericRule> subnets = new LinkedList<GenericRule>();
    private List<GenericRule> passedUrls = new LinkedList<GenericRule>();
    
    public SpywareSettings() { }

    public Boolean getScanCookies()
    {
        return this.scanCookies;
    }

    public void setScanCookies(Boolean scanCookies)
    {
        this.scanCookies = scanCookies;
    }

    public Boolean getScanSubnets()
    {
        return this.scanSubnets;
    }

    public void setScanSubnets(Boolean scanSubnets)
    {
        this.scanSubnets = scanSubnets;
    }

    public Boolean getScanUrls()
    {
        return this.scanUrls;
    }

    public void setScanUrls(Boolean scanUrls)
    {
        this.scanUrls = scanUrls;
    }
    
    public String getUnblockMode()
    {
        return this.unblockMode;
    }

    public void setUnblockMode(String unblockMode)
    {
        this.unblockMode = unblockMode;
    }

    public List<GenericRule> getCookies()
    {
        return this.cookies;
    }

    public void setCookies(List<GenericRule> cookies)
    {
        this.cookies = cookies;
    }

    public List<GenericRule> getSubnets()
    {
        return this.subnets;
    }

    public void setSubnets(List<GenericRule> subnets)
    {
        this.subnets = subnets;
    }

    public List<GenericRule> getPassedUrls()
    {
        return this.passedUrls;
    }

    public void setPassedUrls(List<GenericRule> passedUrls)
    {
        this.passedUrls = passedUrls;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
