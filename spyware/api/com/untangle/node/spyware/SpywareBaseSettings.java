/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/api/com/untangle/node/spyware/SpywareSettings.java $
 */
package com.untangle.node.spyware;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

/**
 * Base Settings for the Spyware node.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
@Embeddable
@SuppressWarnings("serial")
public class SpywareBaseSettings implements Serializable
{
    public static final String UNBLOCK_MODE_NONE   = "None";
    public static final String UNBLOCK_MODE_HOST   = "Host";
    public static final String UNBLOCK_MODE_GLOBAL = "Global";

    private String unblockMode = UNBLOCK_MODE_NONE;
    private boolean activeXEnabled = true;
    private boolean cookieBlockerEnabled = true;
    private boolean spywareEnabled = true;
    private boolean blockAllActiveX = false;
    private boolean urlBlacklistEnabled = true;
    private String activeXDetails ="no description";
    private String cookieBlockerDetails = "no description";
    private String spywareDetails = "no description";
    private String blockAllActiveXDetails = "no description";
    private String urlBlacklistDetails = "no description";

    private int activeXRulesLength;
    private int cookieRulesLength;
    private int subnetRulesLength;
    private int domainWhitelistLength;

    /* This is the date when the system last checked for updates, not
     * the date of the virus signatures. */
    private Date lastUpdate;

    /* This is the version string for the signatures, it may or may
     * not include a date */
    private String signatureVersion;

    public SpywareBaseSettings() { }

    @Enumerated(EnumType.STRING)
    @Column(name="user_whitelist_mode", nullable=false)
    public String getUnblockMode()
    {
        return unblockMode;
    }

    public void setUnblockMode(String unblockMode)
    {
        this.unblockMode = unblockMode;
    }

    /**
     * ActiveX scanner enabled.
     *
     * @return true if active.
     */
    @Column(name="activex_enabled", nullable=false)
    public boolean getActiveXEnabled()
    {
        return activeXEnabled;
    }

    public void setActiveXEnabled(boolean activeXEnabled)
    {
        this.activeXEnabled = activeXEnabled;
    }

    /**
     * Cookie blocker enabled.
     *
     * @return true if cookie is enabled.
     */
    @Column(name="cookie_enabled", nullable=false)
    public boolean getCookieBlockerEnabled()
    {
        return cookieBlockerEnabled;
    }

    public void setCookieBlockerEnabled(boolean cookieBlockerEnabled)
    {
        this.cookieBlockerEnabled = cookieBlockerEnabled;
    }

    /**
     * Spyware enabled.
     *
     * @return true if spyware checking enabled.
     */
    @Column(name="spyware_enabled", nullable=false)
    public boolean getSpywareEnabled()
    {
        return spywareEnabled;
    }

    public void setSpywareEnabled(boolean spywareEnabled)
    {
        this.spywareEnabled = spywareEnabled;
    }

    /**
     * All ActiveX blocked.
     *
     * @return true if all ActiveX should be blocked.
     */
    @Column(name="block_all_activex", nullable=false)
    public boolean getBlockAllActiveX()
    {
        return blockAllActiveX;
    }

    public void setBlockAllActiveX(boolean blockAllActiveX)
    {
        this.blockAllActiveX = blockAllActiveX;
    }

    /**
     * Enables the URL blacklist.
     *
     * @return true if blacklist enabled, false otherwise.
     */
    @Column(name="url_blacklist_enabled", nullable=false)
    public boolean getUrlBlacklistEnabled()
    {
        return urlBlacklistEnabled;
    }

    public void setUrlBlacklistEnabled(boolean urlBlacklistEnabled)
    {
        this.urlBlacklistEnabled = urlBlacklistEnabled;
    }

    @Column(name="activex_details")
    public String getActiveXDetails()
    {
        return activeXDetails;
    }

    public void setActiveXDetails(String activeXDetails)
    {
        this.activeXDetails = activeXDetails;
    }

    @Column(name="cookie_details")
    public String getCookieBlockerDetails()
    {
        return cookieBlockerDetails;
    }

    public void setCookieBlockerDetails(String cookieBlockerDetails)
    {
        this.cookieBlockerDetails = cookieBlockerDetails;
    }

    @Column(name="spyware_details")
    public String getSpywareDetails()
    {
        return spywareDetails;
    }

    public void setSpywareDetails(String spywareDetails)
    {
        this.spywareDetails = spywareDetails;
    }

    @Column(name="block_all_activex_details")
    public String getBlockAllActiveXDetails()
    {
        return blockAllActiveXDetails;
    }

    public void setBlockAllActiveXDetails(String blockAllActiveXDetails)
    {
        this.blockAllActiveXDetails = blockAllActiveXDetails;
    }

    @Column(name="url_blacklist_details")
    public String getUrlBlacklistDetails()
    {
        return urlBlacklistDetails;
    }

    public void setUrlBlacklistDetails(String urlBlacklistDetails)
    {
        this.urlBlacklistDetails = urlBlacklistDetails;
    }

    @Transient
    public int getActiveXRulesLength()
    {
        return this.activeXRulesLength;
    }

    public void setActiveXRulesLength(int activeXRulesLength)
    {
        this.activeXRulesLength = activeXRulesLength;
    }

    @Transient
    public int getCookieRulesLength()
    {
        return this.cookieRulesLength;
    }

    public void setCookieRulesLength(int cookieRulesLength)
    {
        this.cookieRulesLength = cookieRulesLength;
    }

    @Transient
    public int getSubnetRulesLength()
    {
        return this.subnetRulesLength;
    }

    public void setSubnetRulesLength(int subnetRulesLength)
    {
        this.subnetRulesLength = subnetRulesLength;
    }

    @Transient
    public int getDomainWhitelistLength()
    {
        return this.domainWhitelistLength;
    }

    public void setDomainWhitelistLength(int domainWhitelistLength)
    {
        this.domainWhitelistLength = domainWhitelistLength;
    }

    @Transient
    public Date getLastUpdate()
    {
        return this.lastUpdate;
    }

    public void setLastUpdate(Date newValue)
    {
        this.lastUpdate = newValue;
    }

    @Transient
    public String getSignatureVersion()
    {
        return this.signatureVersion;
    }

    public void setSignatureVersion(String newValue)
    {
        this.signatureVersion = newValue;
    }
}
