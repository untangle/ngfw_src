/**
 * $Id$
 */
package com.untangle.uvm.setup.jabsorb;


import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.transaction.TransactionRolledbackException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.hc.core5.net.URIBuilder;

import org.json.JSONObject;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.WizardSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.AdminUserSettings;
/** SetupContextImpl */
public class SetupContextImpl implements UtJsonRpcServlet.SetupContext
{
    private final Logger logger = LogManager.getLogger( this.getClass());

    private UvmContext context;

    /* Shamelessly lifted from AdminManagerImpl */
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";

    /**
     * SetupContextImpl create a SetupContextImpl
     * @param context
     */
    private SetupContextImpl( UvmContext context )
    {
        this.context = context;
    }

    /**
     * setLanguage - sets the language (called from setup wizard)
     * @param language
     * @param source
     */
    public void setLanguage( String language, String source )
    {
        LanguageManager lm = this.context.languageManager();
        LanguageSettings ls = lm.getLanguageSettings();
        ls.setLanguage( language );
        ls.setSource( source );
        lm.setLanguageSettings( ls );
    }

    /**
     * setAdminPassword - set the admin password (called from setup wizard)
     * @param password
     * @param email
     * @param installType
     * @throws TransactionRolledbackException
     */
    public void setAdminPassword( String password, String email, String installType ) throws TransactionRolledbackException
    {
        AdminSettings adminSettings = this.context.adminManager().getSettings();
        SystemSettings systemSettings = this.context.systemManager().getSettings();
        AdminUserSettings admin = null;

        /**
         * Find the "admin" user
         */
        for ( AdminUserSettings user : adminSettings.getUsers()) {
            if ( INITIAL_USER_LOGIN.equals( user.getUsername())) {
                admin = user;
                break;
            }
        }

        /**
         * If not found, create it, otherwise just set the existing admin user's password
         */
        if ( admin == null ) {
            admin = new AdminUserSettings( INITIAL_USER_LOGIN, password, INITIAL_USER_NAME, email );
            adminSettings.addUser( admin );
        } else {
            admin.setEmailAddress( email );
            admin.setPassword( password );
        }

        systemSettings.setInstallType( installType );

        this.context.adminManager().setSettings( adminSettings );
        this.context.systemManager().setSettings( systemSettings, false );
    }

    /**
     * getWizardSettings - gets the current Wizard Settings
     * @return WizardSettings
     */
    public WizardSettings getWizardSettings()
    {
        return this.context.getWizardSettings();
    }

    /**
     * Determine whether to enforce remote-only configuration.
     *
     * @return boolean of whether to force remote where true=force remote, false=don't force remote
     * String of following values:
     */
    public boolean getRemote()
    {
        return context.isRemoteSetup();
    }

    /**
     * Determine if remote is reachable
     * @return Boolean of true of cmd is reachable, otherwise false.
     */
    public Boolean getRemoteReachable()
    {
        boolean reachable = false;
        URIBuilder uriBuilder = null;
        try{
            uriBuilder = new URIBuilder(this.context.getCmdUrl());
        }catch(Exception e){
            logger.warn("getRemoteReachable: Unable to create URIBuilder from cmdUrl", e);
            return reachable;
        }

        String host = uriBuilder.getHost();
        int port = uriBuilder.getPort();
        if(port == -1){
            port = uriBuilder.getScheme().equals("https") ? 443 : 80;
        }
        try (Socket socket = new Socket()) {
            reachable = true;
            socket.connect(new InetSocketAddress(host, port), 7000);
        } catch (Exception e) {
            reachable = false;
            logger.warn("Failed to connect to cmd: [{}:{}]", host, port);
        }

        return reachable;
    }

    /**
     * setTimeZone - sets the timezone (called from setup wizard)
     * @param timeZone
     * @throws TransactionRolledbackException
     */
    public void setTimeZone( TimeZone timeZone ) throws TransactionRolledbackException
    {
        this.context.systemManager().setTimeZone( timeZone );
    }

    /**
     * getTranslations - get the translation map
     * @return the map
     */
    public Map<String, String> getTranslations()
    {
        return this.context.languageManager().getTranslations("untangle");
    }

    /**
     * This call returns one big JSONObject with references to all the
     * important information This is used to avoid lots of separate
     * synchornous calls via the Setup Wizards UI. Reducing all these
     * seperate calls to initialize the UI reduces startup time
     * @return <doc>
     */
    public JSONObject getSetupWizardStartupInfo()
    {
        JSONObject json = new JSONObject();

        try {
            json.put("skinName", this.context.skinManager().getSettings().getSkinName());
            json.put("timezoneID", this.context.systemManager().getTimeZone().getID());
            json.put("timezones", this.context.systemManager().getTimeZones());
            json.put("oemName", this.context.oemManager().getOemName());
            json.put("oemShortName", this.context.oemManager().getOemShortName());
            json.put("oemProductName", this.context.oemManager().getOemProductName());
            json.put("licenseAgreementUrl", this.context.oemManager().getLicenseAgreementUrl());
            json.put("isCCHidden", this.context.isCCHidden());
            json.put("fullVersionAndRevision", this.context.adminManager().getFullVersionAndRevision());
            json.put("adminEmail", this.context.adminManager().getAdminEmail());
            json.put("language", this.context.languageManager().getLanguageSettings().getLanguage());
            json.put("translations", this.context.languageManager().getTranslations("untangle"));

            List<InterfaceSettings> interfaces = this.context.networkManager().getNetworkSettings().getInterfaces();
            boolean isWirelessInterface = interfaces.stream().anyMatch(intf -> intf.getIsWirelessInterface());
            
            json.put("isWirelessInterface" , isWirelessInterface);
            json.put("wizardSettings", this.context.getWizardSettings());
            json.put("remote", getRemote());
            json.put("remoteUrl", this.context.getCmdUrl());
            json.put("serverUID", this.context.getServerUID());
            json.put("licenseTestUrl", this.context.uriManager().getUriWithPath("https://edge.arista.com/favicon.ico"));

        } catch (Exception e) {
            logger.error("Error generating WebUI startup object", e);
        }
        return json;
    }

    /**
     * makeSetupContext constructor
     * @return SetupContext
     */
    public static UtJsonRpcServlet.SetupContext makeSetupContext()
    {
        UvmContext uvm = UvmContextFactory.context();
        return new SetupContextImpl( uvm );
    }
}
