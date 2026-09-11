/**
 * $Id$
 */
package com.untangle.app.captive_portal.generic;

import java.io.Serializable;
import java.util.LinkedList;

import com.untangle.app.captive_portal.CaptivePortalSettings;
import com.untangle.app.captive_portal.CaptivePortalSettings.AuthenticationType;
import com.untangle.app.captive_portal.CaptivePortalSettings.CertificateDetection;
import com.untangle.app.captive_portal.CaptivePortalSettings.PageType;
import com.untangle.app.captive_portal.CaptureRule;
import com.untangle.app.captive_portal.PassedAddress;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Generic (V2) settings for the Captive Portal app, consumed by the Vue UI.
 * Keeps V1 field names where structurally identical; transforms only the
 * captureRules list into the shared RuleGeneric shape.
 */
@SuppressWarnings("serial")
public class CaptivePortalSettingsGeneric implements Serializable, JSONString {

    // Auth & session - V1 names retained
    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private boolean concurrentLoginsEnabled = true;
    private boolean useMacAddress = false;
    private boolean sessionCookiesEnabled = false;

    // Timeouts (seconds)
    private int idleTimeout = 0;
    private int userTimeout = 3600;
    private int sessionCookiesTimeout = 86400;

    // Page
    private PageType pageType = PageType.BASIC_MESSAGE;
    private CertificateDetection certificateDetection = CertificateDetection.DISABLE_DETECTION;
    private String redirectUrl = "";

    // Redirect flags
    private boolean alwaysUseSecureCapture = false;
    private boolean redirectUsingHostname = false;
    private boolean disableSecureRedirect = false;

    // Basic Message page
    private String basicMessagePageTitle = "";
    private String basicMessagePageWelcome = "";
    private String basicMessageMessageText = "";
    private boolean basicMessageAgreeBox = false;
    private String basicMessageAgreeText = "";
    private String basicMessageFooter = "";

    // Basic Login page
    private String basicLoginPageTitle = "";
    private String basicLoginPageWelcome = "";
    private String basicLoginUsername = "";
    private String basicLoginPassword = "";
    private String basicLoginMessageText = "";
    private String basicLoginFooter = "";

    // Lists - V1 PassedAddress reused directly
    private LinkedList<PassedAddress> passedClients = new LinkedList<>();
    private LinkedList<PassedAddress> passedServers = new LinkedList<>();

    // The ONLY transformed list - type changes from List<CaptureRule> to LinkedList<RuleGeneric>
    // snake_case follows the V2 convention for transformed rule lists
    private LinkedList<RuleGeneric> capture_rules = new LinkedList<>();

// THIS IS FOR ECLIPSE - @formatter:off

    public AuthenticationType getAuthenticationType() { return authenticationType; }
    public void setAuthenticationType(AuthenticationType authenticationType) { this.authenticationType = authenticationType; }

    public boolean getConcurrentLoginsEnabled() { return concurrentLoginsEnabled; }
    public void setConcurrentLoginsEnabled(boolean concurrentLoginsEnabled) { this.concurrentLoginsEnabled = concurrentLoginsEnabled; }

    public boolean getUseMacAddress() { return useMacAddress; }
    public void setUseMacAddress(boolean useMacAddress) { this.useMacAddress = useMacAddress; }

    public boolean getSessionCookiesEnabled() { return sessionCookiesEnabled; }
    public void setSessionCookiesEnabled(boolean sessionCookiesEnabled) { this.sessionCookiesEnabled = sessionCookiesEnabled; }

    public int getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

    public int getUserTimeout() { return userTimeout; }
    public void setUserTimeout(int userTimeout) { this.userTimeout = userTimeout; }

    public int getSessionCookiesTimeout() { return sessionCookiesTimeout; }
    public void setSessionCookiesTimeout(int sessionCookiesTimeout) { this.sessionCookiesTimeout = sessionCookiesTimeout; }

    public PageType getPageType() { return pageType; }
    public void setPageType(PageType pageType) { this.pageType = pageType; }

    public CertificateDetection getCertificateDetection() { return certificateDetection; }
    public void setCertificateDetection(CertificateDetection certificateDetection) { this.certificateDetection = certificateDetection; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public boolean getAlwaysUseSecureCapture() { return alwaysUseSecureCapture; }
    public void setAlwaysUseSecureCapture(boolean alwaysUseSecureCapture) { this.alwaysUseSecureCapture = alwaysUseSecureCapture; }

    public boolean getRedirectUsingHostname() { return redirectUsingHostname; }
    public void setRedirectUsingHostname(boolean redirectUsingHostname) { this.redirectUsingHostname = redirectUsingHostname; }

    public boolean getDisableSecureRedirect() { return disableSecureRedirect; }
    public void setDisableSecureRedirect(boolean disableSecureRedirect) { this.disableSecureRedirect = disableSecureRedirect; }

    public String getBasicMessagePageTitle() { return basicMessagePageTitle; }
    public void setBasicMessagePageTitle(String basicMessagePageTitle) { this.basicMessagePageTitle = basicMessagePageTitle; }

    public String getBasicMessagePageWelcome() { return basicMessagePageWelcome; }
    public void setBasicMessagePageWelcome(String basicMessagePageWelcome) { this.basicMessagePageWelcome = basicMessagePageWelcome; }

    public String getBasicMessageMessageText() { return basicMessageMessageText; }
    public void setBasicMessageMessageText(String basicMessageMessageText) { this.basicMessageMessageText = basicMessageMessageText; }

    public boolean getBasicMessageAgreeBox() { return basicMessageAgreeBox; }
    public void setBasicMessageAgreeBox(boolean basicMessageAgreeBox) { this.basicMessageAgreeBox = basicMessageAgreeBox; }

    public String getBasicMessageAgreeText() { return basicMessageAgreeText; }
    public void setBasicMessageAgreeText(String basicMessageAgreeText) { this.basicMessageAgreeText = basicMessageAgreeText; }

    public String getBasicMessageFooter() { return basicMessageFooter; }
    public void setBasicMessageFooter(String basicMessageFooter) { this.basicMessageFooter = basicMessageFooter; }

    public String getBasicLoginPageTitle() { return basicLoginPageTitle; }
    public void setBasicLoginPageTitle(String basicLoginPageTitle) { this.basicLoginPageTitle = basicLoginPageTitle; }

    public String getBasicLoginPageWelcome() { return basicLoginPageWelcome; }
    public void setBasicLoginPageWelcome(String basicLoginPageWelcome) { this.basicLoginPageWelcome = basicLoginPageWelcome; }

    public String getBasicLoginUsername() { return basicLoginUsername; }
    public void setBasicLoginUsername(String basicLoginUsername) { this.basicLoginUsername = basicLoginUsername; }

    public String getBasicLoginPassword() { return basicLoginPassword; }
    public void setBasicLoginPassword(String basicLoginPassword) { this.basicLoginPassword = basicLoginPassword; }

    public String getBasicLoginMessageText() { return basicLoginMessageText; }
    public void setBasicLoginMessageText(String basicLoginMessageText) { this.basicLoginMessageText = basicLoginMessageText; }

    public String getBasicLoginFooter() { return basicLoginFooter; }
    public void setBasicLoginFooter(String basicLoginFooter) { this.basicLoginFooter = basicLoginFooter; }

    public LinkedList<PassedAddress> getPassedClients() { return passedClients; }
    public void setPassedClients(LinkedList<PassedAddress> passedClients) { this.passedClients = passedClients; }

    public LinkedList<PassedAddress> getPassedServers() { return passedServers; }
    public void setPassedServers(LinkedList<PassedAddress> passedServers) { this.passedServers = passedServers; }

    public LinkedList<RuleGeneric> getCapture_rules() { return capture_rules; }
    public void setCapture_rules(LinkedList<RuleGeneric> capture_rules) { this.capture_rules = capture_rules; }


    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms this V2 settings object into V1 by mutating the passed-in
     * (deep-cloned) V1 settings object. Preserves V1-only fields like
     * secretKey, binaryKey, customFileName, checkServerCertificate.
     *
     * @param v1 deep-cloned V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public CaptivePortalSettings transformGenericToCaptivePortalSettings(CaptivePortalSettings v1) {
        if (v1 == null) v1 = new CaptivePortalSettings();

        v1.setAuthenticationType(this.authenticationType);
        v1.setConcurrentLoginsEnabled(this.concurrentLoginsEnabled);
        v1.setUseMacAddress(this.useMacAddress);
        v1.setSessionCookiesEnabled(this.sessionCookiesEnabled);
        v1.setIdleTimeout(this.idleTimeout);
        v1.setUserTimeout(this.userTimeout);
        v1.setSessionCookiesTimeout(this.sessionCookiesTimeout);
        v1.setPageType(this.pageType);
        v1.setCertificateDetection(this.certificateDetection);
        v1.setRedirectUrl(this.redirectUrl);
        v1.setAlwaysUseSecureCapture(this.alwaysUseSecureCapture);
        v1.setRedirectUsingHostname(this.redirectUsingHostname);
        v1.setDisableSecureRedirect(this.disableSecureRedirect);

        v1.setBasicMessagePageTitle(this.basicMessagePageTitle);
        v1.setBasicMessagePageWelcome(this.basicMessagePageWelcome);
        v1.setBasicMessageMessageText(this.basicMessageMessageText);
        v1.setBasicMessageAgreeBox(this.basicMessageAgreeBox);
        v1.setBasicMessageAgreeText(this.basicMessageAgreeText);
        v1.setBasicMessageFooter(this.basicMessageFooter);

        v1.setBasicLoginPageTitle(this.basicLoginPageTitle);
        v1.setBasicLoginPageWelcome(this.basicLoginPageWelcome);
        v1.setBasicLoginUsername(this.basicLoginUsername);
        v1.setBasicLoginPassword(this.basicLoginPassword);
        v1.setBasicLoginMessageText(this.basicLoginMessageText);
        v1.setBasicLoginFooter(this.basicLoginFooter);

        // PassedAddress lists - direct V1 reuse, no transformation
        v1.setPassedClients(this.passedClients);
        v1.setPassedServers(this.passedServers);

        // capture_rules - the ONE transformation (with orphan cleanup)
        if (this.capture_rules != null)
            v1.setCaptureRules(CaptureRule.transformGenericToCaptureRules(this.capture_rules, v1.getCaptureRules()));

        return v1;
    }
}
