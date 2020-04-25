/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.GenericRule;

/**
 * WebFilter settings.
 */
@SuppressWarnings("serial")
public class WebFilterSettings implements Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(getClass());

    public static final String UNBLOCK_MODE_NONE   = "None";
    public static final String UNBLOCK_MODE_HOST   = "Host";
    public static final String UNBLOCK_MODE_GLOBAL = "Global";

    private Integer version = 4;

    private Boolean enableHttpsSni = true;
    private Boolean enableHttpsSniCertFallback = true;
    private Boolean enableHttpsSniIpFallback = false;
    private Boolean unblockPasswordEnabled = false;
    private Boolean unblockPasswordAdmin = false;
    private String  unblockPassword = "";
    private String  unblockMode = UNBLOCK_MODE_NONE;
    private Integer unblockTimeout = 60*60; /* 1 hour */
    private Boolean enforceSafeSearch = true;
    private Boolean forceKidFriendly = false;
    private Boolean restrictYoutube = false;
    private Boolean blockQuic = true;
    private Boolean logQuic = false;
    private Boolean blockAllIpHosts = false;
    private Boolean passReferers = true;
    private Boolean restrictGoogleApps = false;
    private String restrictGoogleAppsDomain = "";

    private Boolean customBlockPageEnabled = false;
    private String customBlockPageUrl = "";
    private Boolean closeHttpsBlockEnabled = false;

    private List<WebFilterRule> filterRules = new LinkedList<WebFilterRule>();
    private List<GenericRule> passedClients = new LinkedList<GenericRule>();
    private List<GenericRule> passedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> blockedUrls = new LinkedList<GenericRule>();
    private List<GenericRule> categories = new LinkedList<GenericRule>();
    private List<GenericRule> searchTerms = new LinkedList<GenericRule>();

    // these are needed during V1 to V2 settings conversion 
    private List<GenericRule> blockedMimeTypes = new LinkedList<GenericRule>();
    private List<GenericRule> blockedExtensions = new LinkedList<GenericRule>();

    // constructors -----------------------------------------------------------

    public WebFilterSettings() { }

    // accessors --------------------------------------------------------------

    public Integer getVersion() { return this.version; }
    public void setVersion( Integer version ) { this.version = version; }

    public List<WebFilterRule> getFilterRules() { return filterRules; }
    public void setFilterRules( List<WebFilterRule> filterRules ) { this.filterRules = filterRules; }

    public List<GenericRule> getPassedClients() { return passedClients; }
    public void setPassedClients( List<GenericRule > passedClients) { this.passedClients = passedClients; }

    public List<GenericRule> getPassedUrls() { return passedUrls; }
    public void setPassedUrls( List<GenericRule > passedUrls) { this.passedUrls = passedUrls; }

    public List<GenericRule> getBlockedUrls() { return blockedUrls; }
    public void setBlockedUrls( List<GenericRule > blockedUrls) { this.blockedUrls = blockedUrls; }

    public List<GenericRule> getCategories() { return this.categories; }
    public void setCategories( List<GenericRule > categories) { this.categories = categories; }

    public List<GenericRule> getSearchTerms() { return this.searchTerms; }
    public void setSearchTerms( List<GenericRule > searchTerms) { this.searchTerms = searchTerms; }

    public List<GenericRule> V1_getBlockedMimeTypes() { return blockedMimeTypes; }
    public void setBlockedMimeTypes( List<GenericRule > blockedMimeTypes) { this.blockedMimeTypes = blockedMimeTypes; }

    public List<GenericRule> V1_getBlockedExtensions() { return blockedExtensions; }
    public void setBlockedExtensions( List<GenericRule > blockedExtensions) { this.blockedExtensions = blockedExtensions; }

    public Boolean getBlockAllIpHosts() { return blockAllIpHosts; }
    public void setBlockAllIpHosts( Boolean blockAllIpHosts ) { this.blockAllIpHosts = blockAllIpHosts; }

    public Boolean getPassReferers() { return passReferers; }
    public void setPassReferers( Boolean passReferers ) { this.passReferers = passReferers; }

    public Boolean getRestrictGoogleApps() { return restrictGoogleApps; }
    public void setRestrictGoogleApps( Boolean restrictGoogleApps ) { this.restrictGoogleApps = restrictGoogleApps; }

    public String getRestrictGoogleAppsDomain() { return restrictGoogleAppsDomain; }
    public void setRestrictGoogleAppsDomain( String restrictGoogleAppsDomain ) { this.restrictGoogleAppsDomain = restrictGoogleAppsDomain; }

    public Boolean getEnableHttpsSni() { return enableHttpsSni; }
    public void setEnableHttpsSni( Boolean enableHttpsSni ) { this.enableHttpsSni = enableHttpsSni; }

    public Boolean getEnableHttpsSniCertFallback() { return enableHttpsSniCertFallback; }
    public void setEnableHttpsSniCertFallback( Boolean newValue ) { this.enableHttpsSniCertFallback = newValue; }

    public Boolean getEnableHttpsSniIpFallback() { return enableHttpsSniIpFallback; }
    public void setEnableHttpsSniIpFallback( Boolean newValue ) { this.enableHttpsSniIpFallback = newValue; }
    
    public Boolean getEnforceSafeSearch() { return enforceSafeSearch; }
    public void setEnforceSafeSearch( Boolean newValue ) { this.enforceSafeSearch = newValue; }

    public Boolean getForceKidFriendly() { return forceKidFriendly; }
    public void setForceKidFriendly( Boolean newValue ) { this.forceKidFriendly = newValue; }

    public Boolean getRestrictYoutube() { return restrictYoutube; }
    public void setRestrictYoutube( Boolean newValue ) { this.restrictYoutube = newValue; }
    
    public Boolean getBlockQuic() { return blockQuic; }
    public void setBlockQuic( Boolean newValue ) { this.blockQuic = newValue; }
    
    public Boolean getLogQuic() { return logQuic; }
    public void setLogQuic( Boolean newValue ) { this.logQuic = newValue; }
    
    public Boolean getUnblockPasswordEnabled() { return this.unblockPasswordEnabled; }
    public void setUnblockPasswordEnabled( Boolean newValue ) { this.unblockPasswordEnabled = newValue; }

    public Boolean getUnblockPasswordAdmin() { return this.unblockPasswordAdmin; }
    public void setUnblockPasswordAdmin( Boolean newValue ) { this.unblockPasswordAdmin = newValue; }
    
    public String getUnblockPassword() { return this.unblockPassword; }
    public void setUnblockPassword( String newValue ) { this.unblockPassword = newValue; }

    public String getUnblockMode() { return this.unblockMode; }
    public void setUnblockMode( String unblockMode ) { this.unblockMode = unblockMode; }

    public Integer getUnblockTimeout() { return this.unblockTimeout; }
    public void setUnblockTimeout( Integer unblockTimeout ) { this.unblockTimeout = unblockTimeout; }

    public Boolean getCustomBlockPageEnabled() { return customBlockPageEnabled; }
    public void setCustomBlockPageEnabled( Boolean customBlockPageEnabled ) { this.customBlockPageEnabled = customBlockPageEnabled; }

    public String getCustomBlockPageUrl() { return this.customBlockPageUrl; }
    public void setCustomBlockPageUrl( String customBlockPageUrl ) { this.customBlockPageUrl = customBlockPageUrl; }

    public Boolean getCloseHttpsBlockEnabled() { return closeHttpsBlockEnabled; }
    public void setCloseHttpsBlockEnabled( Boolean closeHttpsBlockEnabled ) { this.closeHttpsBlockEnabled = closeHttpsBlockEnabled; }

    public GenericRule getCategory(Integer id)
    {
        for (GenericRule cat : getCategories()) {
            if (id.equals(cat.getId())){
                return cat;
            }
        }

        return null;
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
