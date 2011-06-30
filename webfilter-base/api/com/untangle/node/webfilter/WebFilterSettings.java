/*
 * $Id$
 */
package com.untangle.node.webfilter;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.security.NodeId;

/**
 * WebFilter settings.
 */
@SuppressWarnings("serial")
public class WebFilterSettings implements Serializable
{
    public static final String UNBLOCK_MODE_NONE   = "None";
    public static final String UNBLOCK_MODE_HOST   = "Host";
    public static final String UNBLOCK_MODE_GLOBAL = "Global";

    private BlockTemplate blockTemplate = new BlockTemplate();

    private boolean enableHttps = false;
    private boolean unblockPasswordEnabled = false;
    private boolean unblockPasswordAdmin = false;
    private String  unblockPassword = "";
    private String  unblockMode = UNBLOCK_MODE_NONE;
    private boolean enforceSafeSearch = true;
    private boolean blockAllIpHosts = true;

    private List<IPMaddrRule> passedClients = new LinkedList<IPMaddrRule>();
    private List<GenericRule> passedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> blockedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> blockedMimeTypes = new LinkedList<GenericRule>();
    private List<GenericRule> blockedExtensions = new LinkedList<GenericRule>();
    private List<GenericRule> categories = new LinkedList<GenericRule>();

    // constructors -----------------------------------------------------------

    public WebFilterSettings() { }

    // accessors --------------------------------------------------------------

    public List<IPMaddrRule> getPassedClients()
    {
        return passedClients;
    }

    public void setPassedClients(List<IPMaddrRule> passedClients)
    {
        this.passedClients = passedClients;
    }

    public List<GenericRule> getPassedUrls()
    {
        return passedUrls;
    }

    public void setPassedUrls(List<GenericRule> passedUrls)
    {
        this.passedUrls = passedUrls;
    }

    public List<GenericRule> getBlockedUrls()
    {
        return blockedUrls;
    }

    public void setBlockedUrls(List<GenericRule> blockedUrls)
    {
        this.blockedUrls = blockedUrls;
    }

    public List<GenericRule> getBlockedMimeTypes()
    {
        return blockedMimeTypes;
    }

    public void setBlockedMimeTypes(List<GenericRule> blockedMimeTypes)
    {
        this.blockedMimeTypes = blockedMimeTypes;
    }

    public List<GenericRule> getBlockedExtensions()
    {
        return blockedExtensions;
    }

    public void setBlockedExtensions(List<GenericRule> blockedExtensions)
    {
        this.blockedExtensions = blockedExtensions;
    }

    public List<GenericRule> getCategories()
    {
        return this.categories;
    }

    public void setCategories(List<GenericRule> categories)
    {
        this.categories = categories;
    }

    public GenericRule getCategory(String name)
    {
        if (name == null)
            return null;
        
        for (GenericRule cat : getCategories()) {
            if (name.equals(cat.getDescription()))
                return cat;
        }

            return null;
    }

    /**
     * Template for block messages.
     *
     * @return the block message.
     */
    public BlockTemplate getBlockTemplate()
    {
        return blockTemplate;
    }

    public void setBlockTemplate(BlockTemplate blockTemplate)
    {
        this.blockTemplate = blockTemplate;
    }

    /**
     * Block all requests to hosts identified only by an IP address.
     *
     * @return true when IP requests are blocked.
     */
    public boolean getBlockAllIpHosts()
    {
        return blockAllIpHosts;
    }

    public void setBlockAllIpHosts(boolean blockAllIpHosts)
    {
        this.blockAllIpHosts = blockAllIpHosts;
    }

    /**
     * If true, enables checking of HTTPS traffic.
     *
     * @return true to block.
     */
    public boolean getEnableHttps()
    {
        return enableHttps;
    }

    public void setEnableHttps(boolean enableHttps)
    {
        this.enableHttps = enableHttps;
    }

    /**
     * If true, enforces safe search on popular search engines.
     *
     * @return true to block.
     */
    public boolean getEnforceSafeSearch()
    {
        return enforceSafeSearch;
    }

    public void setEnforceSafeSearch(boolean enforceSafeSearch)
    {
        this.enforceSafeSearch = enforceSafeSearch;
    }

    /**
     * If true, ask for a password to unblock a site.
     *
     * @return true to block.
     */
    public boolean getUnblockPasswordEnabled()
    {
        return this.unblockPasswordEnabled;
    }

    public void setUnblockPasswordEnabled(boolean newValue)
    {
        this.unblockPasswordEnabled = newValue;
    }

    /**
     * If true, ask for a password to unblock a site.
     *
     * @return true to block.
     */
    public boolean getUnblockPasswordAdmin()
    {
        return this.unblockPasswordAdmin;
    }

    public void setUnblockPasswordAdmin(boolean newValue)
    {
        this.unblockPasswordAdmin = newValue;
    }
    
    /**
     * String to use for the unblock password
     *
     * @return Unblock password for this node..
     */
    public String getUnblockPassword()
    {
        return this.unblockPassword;
    }

    public void setUnblockPassword(String newValue)
    {
        this.unblockPassword = newValue;
    }

    /**
     * The mode for bypass
     *
     * @return The bypass settings
     */
    public String getUnblockMode()
    {
        return this.unblockMode;
    }

    public void setUnblockMode(String unblockMode)
    {
        this.unblockMode = unblockMode;
    }


    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
