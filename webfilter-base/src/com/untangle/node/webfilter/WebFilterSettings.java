/*
 * $Id$
 */
package com.untangle.node.webfilter;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.NodeSettings;

/**
 * WebFilter settings.
 */
@SuppressWarnings("serial")
public class WebFilterSettings implements Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    public static final String UNBLOCK_MODE_NONE   = "None";
    public static final String UNBLOCK_MODE_HOST   = "Host";
    public static final String UNBLOCK_MODE_GLOBAL = "Global";

    private Integer version = new Integer(1);

    private Boolean enableHttps = false;
    private Boolean enableHttpsSni = true;
    private Boolean unblockPasswordEnabled = false;
    private Boolean unblockPasswordAdmin = false;
    private String  unblockPassword = "";
    private String  unblockMode = UNBLOCK_MODE_NONE;
    private Integer unblockTimeout = 60*60; /* 1 hour */
    private Boolean enforceSafeSearch = true;
    private Boolean enforceYoutubeForSchools = false;
    private String  youtubeForSchoolsIdentifier = "";
    private Boolean blockAllIpHosts = false;
    
    private List<GenericRule> passedClients = new LinkedList<GenericRule>();
    private List<GenericRule> passedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> blockedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> blockedMimeTypes = new LinkedList<GenericRule>();
    private List<GenericRule> blockedExtensions = new LinkedList<GenericRule>();
    private List<GenericRule> categories = new LinkedList<GenericRule>();

    // constructors -----------------------------------------------------------

    public WebFilterSettings() { }

    // accessors --------------------------------------------------------------

    public Integer getVersion() { return this.version; }
    public void setVersion( Integer version ) { this.version = version; }

    public List<GenericRule> getPassedClients() { return passedClients; }
    public void setPassedClients( List<GenericRule > passedClients) { this.passedClients = passedClients; }

    public List<GenericRule> getPassedUrls() { return passedUrls; }
    public void setPassedUrls( List<GenericRule > passedUrls) { this.passedUrls = passedUrls; }

    public List<GenericRule> getBlockedUrls() { return blockedUrls; }
    public void setBlockedUrls( List<GenericRule > blockedUrls) { this.blockedUrls = blockedUrls; }

    public List<GenericRule> getBlockedMimeTypes() { return blockedMimeTypes; }
    public void setBlockedMimeTypes( List<GenericRule > blockedMimeTypes) { this.blockedMimeTypes = blockedMimeTypes; }

    public List<GenericRule> getBlockedExtensions() { return blockedExtensions; }
    public void setBlockedExtensions( List<GenericRule > blockedExtensions) { this.blockedExtensions = blockedExtensions; }

    public List<GenericRule> getCategories() { return this.categories; }
    public void setCategories( List<GenericRule > categories) { this.categories = categories; }

    /**
     * Block all requests to hosts identified only by an IP address.
     */
    public Boolean getBlockAllIpHosts() { return blockAllIpHosts; }
    public void setBlockAllIpHosts( Boolean blockAllIpHosts ) { this.blockAllIpHosts = blockAllIpHosts; }

    /**
     * If true, enables checking of HTTPS traffic.
     */
    public Boolean getEnableHttps() { return enableHttps; }
    public void setEnableHttps( Boolean enableHttps ) { this.enableHttps = enableHttps; }

    public Boolean getEnableHttpsSni() { return enableHttpsSni; }
    public void setEnableHttpsSni( Boolean enableHttpsSni ) { this.enableHttpsSni = enableHttpsSni; }
    
    /**
     * If true, enforces safe search on popular search engines.
     */
    public Boolean getEnforceSafeSearch() { return enforceSafeSearch; }
    public void setEnforceSafeSearch( Boolean enforceSafeSearch ) { this.enforceSafeSearch = enforceSafeSearch; }

    /**
     * If true, enforces safe search on popular search engines.
     */
    public Boolean getEnforceYoutubeForSchools() { return enforceYoutubeForSchools; }
    public void setEnforceYoutubeForSchools( Boolean enforceYoutubeForSchools ) { this.enforceYoutubeForSchools = enforceYoutubeForSchools; }

    /**
     * If true, enforces safe search on popular search engines.
     */
    public String getYoutubeForSchoolsIdentifier() { return youtubeForSchoolsIdentifier; }
    public void setYoutubeForSchoolsIdentifier( String youtubeForSchoolsIdentifier ) { this.youtubeForSchoolsIdentifier = youtubeForSchoolsIdentifier; }

    /**
     * If true, ask for a password to unblock a site.
     */
    public Boolean getUnblockPasswordEnabled() { return this.unblockPasswordEnabled; }
    public void setUnblockPasswordEnabled( Boolean newValue ) { this.unblockPasswordEnabled = newValue; }

    /**
     * If true, ask for a password to unblock a site.
     */
    public Boolean getUnblockPasswordAdmin() { return this.unblockPasswordAdmin; }
    public void setUnblockPasswordAdmin( Boolean newValue ) { this.unblockPasswordAdmin = newValue; }
    
    /**
     * String to use for the unblock password
     */
    public String getUnblockPassword() { return this.unblockPassword; }
    public void setUnblockPassword( String newValue ) { this.unblockPassword = newValue; }

    /**
     * The mode for bypass
     */
    public String getUnblockMode() { return this.unblockMode; }
    public void setUnblockMode( String unblockMode ) { this.unblockMode = unblockMode; }

    /**
     * The timeout for bypass (seconds)
     * If unblock mode is set to Host, then the unblock will last for this many seconds
     */
    public Integer getUnblockTimeout() { return this.unblockTimeout; }
    public void setUnblockTimeout( Integer unblockTimeout ) { this.unblockTimeout = unblockTimeout; }
    
    public GenericRule getCategory(String idString)
    {
        if (idString == null)
            return null;
        
        for (GenericRule cat : getCategories()) {
            if (idString.equals(cat.getString()))
                return cat;
        }

            return null;
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
