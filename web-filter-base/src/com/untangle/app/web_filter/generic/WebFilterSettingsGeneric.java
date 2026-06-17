/**
 * $Id$
 */
package com.untangle.app.web_filter.generic;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.app.web_filter.WebFilterRule;
import com.untangle.app.web_filter.WebFilterSettings;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Generic (V2) settings for the Web Filter app, consumed by the Vue UI.
 * Keeps V1 field names where structurally identical; transforms only the
 * filterRules list into the shared RuleGeneric shape.
 * V1-only fields (version, blockedMimeTypes, blockedExtensions) are omitted.
 */
@SuppressWarnings("serial")
public class WebFilterSettingsGeneric implements Serializable, JSONString
{
    private Boolean enableHttpsSni = true;
    private Boolean enableHttpsSniCertFallback = true;
    private Boolean enableHttpsSniIpFallback = false;
    private Boolean unblockPasswordEnabled = false;
    private Boolean unblockPasswordAdmin = false;
    private String  unblockPassword = "";
    private String  unblockMode = WebFilterSettings.UNBLOCK_MODE_NONE;
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
    private Boolean blockECH = false;

    private Boolean customBlockPageEnabled = false;
    private String customBlockPageUrl = "";
    private Boolean closeHttpsBlockEnabled = false;

    private LinkedList<RuleGeneric> filterRules = new LinkedList<>();
    private LinkedList<GenericRule> passClients = new LinkedList<>();
    private LinkedList<GenericRule> passList = new LinkedList<>();
    private LinkedList<GenericRule> blockList = new LinkedList<>();
    private LinkedList<GenericRule> categories = new LinkedList<>();
    private LinkedList<GenericRule> searchTerms = new LinkedList<>();

    public Boolean getEnableHttpsSni() { return enableHttpsSni; }
    public void setEnableHttpsSni(Boolean enableHttpsSni) { this.enableHttpsSni = enableHttpsSni; }

    public Boolean getEnableHttpsSniCertFallback() { return enableHttpsSniCertFallback; }
    public void setEnableHttpsSniCertFallback(Boolean enableHttpsSniCertFallback) { this.enableHttpsSniCertFallback = enableHttpsSniCertFallback; }

    public Boolean getEnableHttpsSniIpFallback() { return enableHttpsSniIpFallback; }
    public void setEnableHttpsSniIpFallback(Boolean enableHttpsSniIpFallback) { this.enableHttpsSniIpFallback = enableHttpsSniIpFallback; }

    public Boolean getUnblockPasswordEnabled() { return unblockPasswordEnabled; }
    public void setUnblockPasswordEnabled(Boolean unblockPasswordEnabled) { this.unblockPasswordEnabled = unblockPasswordEnabled; }

    public Boolean getUnblockPasswordAdmin() { return unblockPasswordAdmin; }
    public void setUnblockPasswordAdmin(Boolean unblockPasswordAdmin) { this.unblockPasswordAdmin = unblockPasswordAdmin; }

    public String getUnblockPassword() { return unblockPassword; }
    public void setUnblockPassword(String unblockPassword) { this.unblockPassword = unblockPassword; }

    public String getUnblockMode() { return unblockMode; }
    public void setUnblockMode(String unblockMode) { this.unblockMode = unblockMode; }

    public Integer getUnblockTimeout() { return unblockTimeout; }
    public void setUnblockTimeout(Integer unblockTimeout) { this.unblockTimeout = unblockTimeout; }

    public Boolean getEnforceSafeSearch() { return enforceSafeSearch; }
    public void setEnforceSafeSearch(Boolean enforceSafeSearch) { this.enforceSafeSearch = enforceSafeSearch; }

    public Boolean getForceKidFriendly() { return forceKidFriendly; }
    public void setForceKidFriendly(Boolean forceKidFriendly) { this.forceKidFriendly = forceKidFriendly; }

    public Boolean getRestrictYoutube() { return restrictYoutube; }
    public void setRestrictYoutube(Boolean restrictYoutube) { this.restrictYoutube = restrictYoutube; }

    public Boolean getBlockQuic() { return blockQuic; }
    public void setBlockQuic(Boolean blockQuic) { this.blockQuic = blockQuic; }

    public Boolean getLogQuic() { return logQuic; }
    public void setLogQuic(Boolean logQuic) { this.logQuic = logQuic; }

    public Boolean getBlockAllIpHosts() { return blockAllIpHosts; }
    public void setBlockAllIpHosts(Boolean blockAllIpHosts) { this.blockAllIpHosts = blockAllIpHosts; }

    public Boolean getPassReferers() { return passReferers; }
    public void setPassReferers(Boolean passReferers) { this.passReferers = passReferers; }

    public Boolean getRestrictGoogleApps() { return restrictGoogleApps; }
    public void setRestrictGoogleApps(Boolean restrictGoogleApps) { this.restrictGoogleApps = restrictGoogleApps; }

    public String getRestrictGoogleAppsDomain() { return restrictGoogleAppsDomain; }
    public void setRestrictGoogleAppsDomain(String restrictGoogleAppsDomain) { this.restrictGoogleAppsDomain = restrictGoogleAppsDomain; }

    public Boolean getBlockECH() { return blockECH; }
    public void setBlockECH(Boolean blockECH) { this.blockECH = blockECH; }

    public Boolean getCustomBlockPageEnabled() { return customBlockPageEnabled; }
    public void setCustomBlockPageEnabled(Boolean customBlockPageEnabled) { this.customBlockPageEnabled = customBlockPageEnabled; }

    public String getCustomBlockPageUrl() { return customBlockPageUrl; }
    public void setCustomBlockPageUrl(String customBlockPageUrl) { this.customBlockPageUrl = customBlockPageUrl; }

    public Boolean getCloseHttpsBlockEnabled() { return closeHttpsBlockEnabled; }
    public void setCloseHttpsBlockEnabled(Boolean closeHttpsBlockEnabled) { this.closeHttpsBlockEnabled = closeHttpsBlockEnabled; }

    public LinkedList<RuleGeneric> getFilterRules() { return filterRules; }
    public void setFilterRules(LinkedList<RuleGeneric> filterRules) { this.filterRules = filterRules; }

    public LinkedList<GenericRule> getPassClients() { return passClients; }
    public void setPassClients(LinkedList<GenericRule> passClients) { this.passClients = passClients; }

    public LinkedList<GenericRule> getPassList() { return passList; }
    public void setPassList(LinkedList<GenericRule> passList) { this.passList = passList; }

    public LinkedList<GenericRule> getBlockList() { return blockList; }
    public void setBlockList(LinkedList<GenericRule> blockList) { this.blockList = blockList; }

    public LinkedList<GenericRule> getCategories() { return categories; }
    public void setCategories(LinkedList<GenericRule> categories) { this.categories = categories; }

    public LinkedList<GenericRule> getSearchTerms() { return searchTerms; }
    public void setSearchTerms(LinkedList<GenericRule> searchTerms) { this.searchTerms = searchTerms; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms this V2 settings object into V1 by mutating the passed-in
     * (deep-cloned) V1 settings object. Preserves V1-only fields like
     * version, blockedMimeTypes, blockedExtensions.
     *
     * @param v1 deep-cloned V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public WebFilterSettings transformGenericToWebFilterSettings(WebFilterSettings v1)
    {
        if (v1 == null) v1 = new WebFilterSettings();

        v1.setEnableHttpsSni(this.enableHttpsSni);
        v1.setEnableHttpsSniCertFallback(this.enableHttpsSniCertFallback);
        v1.setEnableHttpsSniIpFallback(this.enableHttpsSniIpFallback);
        v1.setUnblockPasswordEnabled(this.unblockPasswordEnabled);
        v1.setUnblockPasswordAdmin(this.unblockPasswordAdmin);
        v1.setUnblockPassword(this.unblockPassword);
        v1.setUnblockMode(this.unblockMode);
        v1.setUnblockTimeout(this.unblockTimeout);
        v1.setEnforceSafeSearch(this.enforceSafeSearch);
        v1.setForceKidFriendly(this.forceKidFriendly);
        v1.setRestrictYoutube(this.restrictYoutube);
        v1.setBlockQuic(this.blockQuic);
        v1.setLogQuic(this.logQuic);
        v1.setBlockAllIpHosts(this.blockAllIpHosts);
        v1.setPassReferers(this.passReferers);
        v1.setRestrictGoogleApps(this.restrictGoogleApps);
        v1.setRestrictGoogleAppsDomain(this.restrictGoogleAppsDomain);
        v1.setBlockECH(this.blockECH);
        v1.setCustomBlockPageEnabled(this.customBlockPageEnabled);
        v1.setCustomBlockPageUrl(this.customBlockPageUrl);
        v1.setCloseHttpsBlockEnabled(this.closeHttpsBlockEnabled);

        // GenericRule lists - shared type, assign directly; always set (empty list when null)
        v1.setPassedClients(this.passClients != null ? new LinkedList<>(this.passClients) : new LinkedList<>());
        v1.setPassedUrls(this.passList != null       ? new LinkedList<>(this.passList)    : new LinkedList<>());
        v1.setBlockedUrls(this.blockList != null     ? new LinkedList<>(this.blockList)   : new LinkedList<>());
        v1.setCategories(this.categories != null       ? new LinkedList<>(this.categories)    : new LinkedList<>());
        v1.setSearchTerms(this.searchTerms != null     ? new LinkedList<>(this.searchTerms)   : new LinkedList<>());

        // filterRules - the ONE transformation (with orphan cleanup); returns empty list when null
        v1.setFilterRules(WebFilterRule.transformGenericToWebFilterRules(this.filterRules, v1.getFilterRules()));

        return v1;
    }
}
