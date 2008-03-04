package com.untangle.node.spyware;

import java.io.Serializable;

import com.untangle.node.http.UserWhitelistMode;
import com.untangle.uvm.security.Tid;

/**
 * Base Settings for the Spyware node.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
public class SpywareBaseSettings implements Serializable {
    private Tid tid;
    private UserWhitelistMode userWhitelistMode;
    private boolean activeXEnabled;
    private boolean cookieBlockerEnabled;
    private boolean spywareEnabled;
    private boolean blockAllActiveX;
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
    
    public SpywareBaseSettings(SpywareSettings settings) {
    	this.tid = settings.getTid();
        this.userWhitelistMode = settings.getUserWhitelistMode();
        this.activeXEnabled = settings.getActiveXEnabled();
        this.cookieBlockerEnabled = settings.getCookieBlockerEnabled();
        this.spywareEnabled = settings.getSpywareEnabled();
        this.blockAllActiveX = settings.getBlockAllActiveX();
        this.urlBlacklistEnabled  = settings.getUrlBlacklistEnabled();
        this.activeXDetails = settings.getActiveXDetails();
        this.cookieBlockerDetails = settings.getCookieBlockerDetails();
        this.spywareDetails = settings.getSpywareDetails();
        this.blockAllActiveXDetails = settings.getBlockAllActiveXDetails();
        this.urlBlacklistDetails = settings.getUrlBlacklistDetails();
        this.activeXRulesLength = settings.getActiveXRules().size();
        this.cookieRulesLength = settings.getCookieRules().size();
        this.subnetRulesLength = settings.getSubnetRules().size();
        this.domainWhitelistLength = settings.getDomainWhitelist().size();
	}
        
	public Tid getTid() {
		return tid;
	}
	public void setTid(Tid tid) {
		this.tid = tid;
	}
	public UserWhitelistMode getUserWhitelistMode() {
		return userWhitelistMode;
	}
	public void setUserWhitelistMode(UserWhitelistMode userWhitelistMode) {
		this.userWhitelistMode = userWhitelistMode;
	}
	public boolean isActiveXEnabled() {
		return activeXEnabled;
	}
	public void setActiveXEnabled(boolean activeXEnabled) {
		this.activeXEnabled = activeXEnabled;
	}
	public boolean isCookieBlockerEnabled() {
		return cookieBlockerEnabled;
	}
	public void setCookieBlockerEnabled(boolean cookieBlockerEnabled) {
		this.cookieBlockerEnabled = cookieBlockerEnabled;
	}
	public boolean isSpywareEnabled() {
		return spywareEnabled;
	}
	public void setSpywareEnabled(boolean spywareEnabled) {
		this.spywareEnabled = spywareEnabled;
	}
	public boolean isBlockAllActiveX() {
		return blockAllActiveX;
	}
	public void setBlockAllActiveX(boolean blockAllActiveX) {
		this.blockAllActiveX = blockAllActiveX;
	}
	public boolean isUrlBlacklistEnabled() {
		return urlBlacklistEnabled;
	}
	public void setUrlBlacklistEnabled(boolean urlBlacklistEnabled) {
		this.urlBlacklistEnabled = urlBlacklistEnabled;
	}
	public String getActiveXDetails() {
		return activeXDetails;
	}
	public void setActiveXDetails(String activeXDetails) {
		this.activeXDetails = activeXDetails;
	}
	public String getCookieBlockerDetails() {
		return cookieBlockerDetails;
	}
	public void setCookieBlockerDetails(String cookieBlockerDetails) {
		this.cookieBlockerDetails = cookieBlockerDetails;
	}
	public String getSpywareDetails() {
		return spywareDetails;
	}
	public void setSpywareDetails(String spywareDetails) {
		this.spywareDetails = spywareDetails;
	}
	public String getBlockAllActiveXDetails() {
		return blockAllActiveXDetails;
	}
	public void setBlockAllActiveXDetails(String blockAllActiveXDetails) {
		this.blockAllActiveXDetails = blockAllActiveXDetails;
	}
	public String getUrlBlacklistDetails() {
		return urlBlacklistDetails;
	}
	public void setUrlBlacklistDetails(String urlBlacklistDetails) {
		this.urlBlacklistDetails = urlBlacklistDetails;
	}
	public int getActiveXRulesLength() {
		return activeXRulesLength;
	}
	public void setActiveXRulesLength(int activeXRulesLength) {
		this.activeXRulesLength = activeXRulesLength;
	}
	public int getCookieRulesLength() {
		return cookieRulesLength;
	}
	public void setCookieRulesLength(int cookieRulesLength) {
		this.cookieRulesLength = cookieRulesLength;
	}
	public int getSubnetRulesLength() {
		return subnetRulesLength;
	}
	public void setSubnetRulesLength(int subnetRulesLength) {
		this.subnetRulesLength = subnetRulesLength;
	}
	public int getDomainWhitelistLength() {
		return domainWhitelistLength;
	}
	public void setDomainWhitelistLength(int domainWhitelistLength) {
		this.domainWhitelistLength = domainWhitelistLength;
	}

}
