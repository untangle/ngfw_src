/**
 * $Id: FacebookAuthenticator.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */
package com.untangle.app.directory_connector;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.io.FileWriter;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.License;
import com.untangle.uvm.util.I18nUtil;

public class DirectoryConnectorApp extends AppBase implements com.untangle.uvm.app.DirectoryConnector
{
    private static final String FILE_DISCLAIMER = "# This file is created and maintained by the Untangle Directory Connector\n# service. If you modify this file manually, your changes may be overridden.\n\n";
    private static final String USERAPI_WEBAPP_OLD = "adpb";
    private static final String USERAPI_WEBAPP = "userapi";
    private static final String OAUTH_WEBAPP = "oauth";
    private static final String TAB = "\t";
    private static final String RET = "\n";

    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    /**
     * Directory Connector settings (mostly for RADIUS and AD)
     */
    private DirectoryConnectorSettings settings = null;

    /**
     * The group manager
     */
    private GroupManager groupManager = null;

    /**
     * The Active Directory Manager
     */
    private ActiveDirectoryManagerImpl activeDirectoryManager = null;

    /**
     * The RADIUS Manager
     */
    private RadiusManagerImpl radiusManager = null;

    /**
     * The Google Manager
     */
    private GoogleManagerImpl googleManager = null;

    /**
     * The Facebook Manager
     */
    private FacebookManagerImpl facebookManager = null;

    /**
     * is xvfb running? (used by selenium-based authenticators)
     */
    private static boolean xvfbLaunched = false;
    
    public DirectoryConnectorApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);
    }

    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        /* Start the servlet */
        UvmContextFactory.context().tomcatManager().loadServlet("/" + OAUTH_WEBAPP, OAUTH_WEBAPP);
        UvmContextFactory.context().tomcatManager().loadServlet("/" + USERAPI_WEBAPP, USERAPI_WEBAPP);
        UvmContextFactory.context().tomcatManager().loadServlet("/" + USERAPI_WEBAPP_OLD, USERAPI_WEBAPP); //load the old URL for backwards compat
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().tomcatManager().unloadServlet("/" + OAUTH_WEBAPP);
        UvmContextFactory.context().tomcatManager().unloadServlet("/" + USERAPI_WEBAPP);
        UvmContextFactory.context().tomcatManager().unloadServlet("/" + USERAPI_WEBAPP_OLD);

        stopXvfb();
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        DirectoryConnectorSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/directory-connector/" + "settings_" + nodeID + ".js";

        try {
            readSettings = settingsManager.load(DirectoryConnectorSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        } else {
            logger.info("Loading Settings...");

            /* 12.1 - new facebook settings */
            if (readSettings.getFacebookSettings() == null) {
                readSettings.setFacebookSettings( new FacebookSettings() );
                setSettings( readSettings );
            }
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    public DirectoryConnectorSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final DirectoryConnectorSettings newSettings)
    {
        if (newSettings.getActiveDirectorySettings() == null) {
            throw new IllegalArgumentException("Must provide settings for ActiveDirectory");
        }
        if (newSettings.getRadiusSettings() == null) {
            throw new IllegalArgumentException("Must provide settings for RADIUS");
        }
        if (newSettings.getGoogleSettings() == null) {
            throw new IllegalArgumentException("Must provide settings for Google");
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "directory-connector/" + "settings_" + nodeID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try { logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2)); } catch (Exception e) {}

        this.reconfigure();
    }

    public ActiveDirectoryManagerImpl getActiveDirectoryManager()
    {
        return this.activeDirectoryManager;
    }

    public RadiusManagerImpl getRadiusManager()
    {
        return this.radiusManager;
    }

    public GoogleManagerImpl getGoogleManager()
    {
        return this.googleManager;
    }

    public FacebookManagerImpl getFacebookManager()
    {
        return this.facebookManager;
    }
    
    public List<UserEntry> getUserEntries()
    {
        LinkedList<UserEntry> users = new LinkedList<UserEntry>();

        /* add all AD users */
        try{
            List<UserEntry> adUsers = activeDirectoryManager.getActiveDirectoryUserEntries();
            users.addAll(adUsers);
        }
        catch( Exception e ){
            logger.warn(e.getMessage());
        }


        /* add all "standard" users */
        users.addFirst(new UserEntry("[unauthenticated]", "", "", ""));
        users.addFirst(new UserEntry("[authenticated]", "", "", ""));
        users.addFirst(new UserEntry("[any]", "", "", ""));

        return users;
    }

    public List<GroupEntry> getGroupEntries()
    {
        LinkedList<GroupEntry> groups = new LinkedList<GroupEntry>();

        /* add all AD groups */
        List<GroupEntry> adGroups = activeDirectoryManager.getActiveDirectoryGroupEntries(true);
        groups.addAll(adGroups);

        return groups;
    }

    public boolean authenticate(String username, String pwd)
    {
        if (activeDirectoryAuthenticate(username, pwd)) return true;
        if (radiusAuthenticate(username, pwd)) return true;
        if (googleAuthenticate(username, pwd)) return true;
        if (facebookAuthenticate(username, pwd)) return true;

        return false;
    }

    public boolean activeDirectoryAuthenticate(String username, String pwd)
    {
        if (!isLicenseValid()) 
            return false;
        if (username == null || username.equals("") || pwd == null || pwd.equals("")) 
            return false;
        if (this.activeDirectoryManager == null) 
            return false;

        return this.activeDirectoryManager.authenticate(username, pwd);
    }

    public boolean radiusAuthenticate(String username, String pwd)
    {
        if (!isLicenseValid())
            return false;
        if (username == null || username.equals("") || pwd == null || pwd.equals("")) 
            return false;
        if (this.radiusManager == null)
            return false;

        return this.radiusManager.authenticate(username, pwd);
    }

    public boolean googleAuthenticate(String username, String pwd)
    {
        if (!isLicenseValid())
            return false;
        if (username == null || username.equals("") || pwd == null || pwd.equals("")) 
            return false;
        if (this.googleManager == null)
            return false;

        return this.googleManager.authenticate(username, pwd);
    }

    public boolean facebookAuthenticate(String username, String pwd)
    {
        if (!isLicenseValid())
            return false;
        if (username == null || username.equals("") || pwd == null || pwd.equals("")) 
            return false;
        if (this.facebookManager == null)
            return false;

        return this.facebookManager.authenticate(username, pwd);
    }
    
    public boolean anyAuthenticate(String username, String pwd)
    {
        if (!isLicenseValid())
            return false;
        if (username == null || username.equals("") || pwd == null || pwd.equals(""))
            return false;
        try {
            if ( UvmContextFactory.context().localDirectory().authenticate(username, pwd) )
                return true;
        } catch (Exception e) {}
        try {
            if (activeDirectoryAuthenticate( username, pwd ))
                return true;                              
        } catch (Exception e) {}
        try {
            if (radiusAuthenticate( username, pwd ))
                return true;                              
        } catch (Exception e) {}
        try {
            if (googleAuthenticate( username, pwd ))
                return true;                              
        } catch (Exception e) {}
        try {
            if (facebookAuthenticate( username, pwd ))
                return true;                              
        } catch (Exception e) {}
        return false;
    }
    
    public boolean isMemberOf(String user, String group)
    {
        if (!isLicenseValid()) {
            return false;
        }

        return this.groupManager.isMemberOf(user, group);
    }

    public List<String> memberOf(String user)
    {
        if (!isLicenseValid()) {
            return new LinkedList<String>();
        }

        return this.groupManager.memberOf(user);
    }

    public void refreshGroupCache()
    {
        this.groupManager.refreshGroupCache();
    }

    public boolean isGoogleDriveConnected()
    {
        return getGoogleManager().isGoogleDriveConnected();
    }

    public void startXvfbIfNecessary()
    {
        if ( ! xvfbLaunched ) {
            UvmContextFactory.context().execManager().exec("killall Xvfb");
            UvmContextFactory.context().execManager().execOutput("nohup Xvfb :1 -screen 5 1024x768x8 >/dev/null 2>&1 &");
            xvfbLaunched = true;
        }

        // sleep one second to give it time to start
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    public void stopXvfb()
    {
        // Kill any running Xvfb used by GoogleAuthenticator and FacebookAuthenticator
        UvmContextFactory.context().execManager().exec("killall Xvfb");

        xvfbLaunched = false;        
    }
    
    protected boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.DIRECTORY_CONNECTOR))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.DIRECTORY_CONNECTOR_OLDNAME))
            return true;
        return false;
    }

    @Override
    public void initializeSettings()
    {
        this.settings = new DirectoryConnectorSettings();
        this.settings.setActiveDirectorySettings(new ActiveDirectorySettings("Administrator", "mypassword", "mydomain.int", "ad_server.mydomain.int", 636, true ));
        this.settings.setRadiusSettings(new RadiusSettings(false, "1.2.3.4", 1812, 1813, "mysharedsecret", "PAP"));
        this.settings.setGoogleSettings(new GoogleSettings());
        this.settings.setFacebookSettings(new FacebookSettings());
        setSettings(this.settings);
    }

    private synchronized void reconfigure()
    {
        /**
         * Initialize the Active Directory manager (or update settings on
         * current)
         */
        if (activeDirectoryManager == null) {
            this.activeDirectoryManager = new ActiveDirectoryManagerImpl(settings.getActiveDirectorySettings(), this);
        } else
            this.activeDirectoryManager.setSettings(settings.getActiveDirectorySettings());

        /**
         * Initialize the Radius manager (or update settings on current)
         */
        if (radiusManager == null)
            this.radiusManager = new RadiusManagerImpl(settings.getRadiusSettings(), this);
        else
            this.radiusManager.setSettings(settings.getRadiusSettings());

        /**
         * Initialize the Google manager (or update settings on current)
         */
        if (googleManager == null)
            this.googleManager = new GoogleManagerImpl(settings.getGoogleSettings(), this);
        else
            this.googleManager.setSettings(settings.getGoogleSettings());

        /**
         * Initialize the Google manager (or update settings on current)
         */
        if (facebookManager == null)
            this.facebookManager = new FacebookManagerImpl(settings.getFacebookSettings(), this);
        else
            this.facebookManager.setSettings(settings.getFacebookSettings());
        
        /**
         * Initialize the Group manager (if necessary) and Refresh
         */
        if (groupManager == null) {
            this.groupManager = new GroupManager(this);
            this.groupManager.start();
        }
        this.refreshGroupCache();
        this.updateRadiusClient(settings.getRadiusSettings());
    }

    private void updateRadiusClient(RadiusSettings radiusSettings)
    {
        /**
         * The IPsec node uses the radiusclient1 package for L2TP RADIUS support so
         * we update those files and we create strongswan.radius for Xauth support.
         * We also create /etc/ppp/peers/radius-auth-proto which tells xl2tpd/pppd
         * which authentication method (PAP, CHAP, MS-CHAP v1, MS-CHAP v2) to require.
         */

        try {
            FileWriter server = new FileWriter("/etc/radiusclient/servers", false);
            FileWriter client = new FileWriter("/etc/radiusclient/radiusclient.conf", false);
            FileWriter xauth = new FileWriter("/etc/strongswan.radius", false);
            FileWriter peers = new FileWriter("/etc/ppp/peers/radius-auth-proto", false);

            server.write(FILE_DISCLAIMER);
            client.write(FILE_DISCLAIMER);
            xauth.write(FILE_DISCLAIMER);
            peers.write(FILE_DISCLAIMER);

            if (radiusSettings.isEnabled() == true) {
                server.write(radiusSettings.getServer() + TAB + TAB + radiusSettings.getSharedSecret() + RET);

                client.write("auth_order       " + "radius" + RET);
                client.write("login_tries      " + "4" + RET);
                client.write("login_timeout    " + "60" + RET);
                client.write("nologin          " + "/etc/nologin" + RET);
                client.write("issue            " + "/etc/radiusclient/issue" + RET);
                client.write("servers          " + "/etc/radiusclient/servers" + RET);
                client.write("dictionary       " + "/etc/radiusclient/dictionary.untangle" + RET);
                client.write("login_radius     " + "/usr/sbin/login.radius" + RET);
                client.write("seqfile          " + "/var/run/radius.seq" + RET);
                client.write("mapfile          " + "/etc/radiusclient/port-id-map" + RET);
                client.write("default_realm    " + RET);
                client.write("radius_timeout   " + "10" + RET);
                client.write("radius_retries   " + "3" + RET);
                client.write("login_local      " + "/bin/login" + RET);
                client.write("authserver       " + radiusSettings.getServer() + ":" + String.valueOf(radiusSettings.getAuthPort()) + RET);
                client.write("acctserver       " + radiusSettings.getServer() + ":" + String.valueOf(radiusSettings.getAcctPort()) + RET);
                
                xauth.write("eap-radius {" + RET);
                xauth.write(TAB + "servers {" + RET);
                xauth.write(TAB + TAB + "untangle {" + RET);
                xauth.write(TAB + TAB + TAB + "address = " + radiusSettings.getServer() + RET);
                xauth.write(TAB + TAB + TAB + "secret = " + radiusSettings.getSharedSecret() + RET);
                xauth.write(TAB + TAB + TAB + "port = " + String.valueOf(radiusSettings.getAuthPort()) + RET);
                xauth.write(TAB + TAB  + "}" + RET);
                xauth.write(TAB + "}" + RET);
                xauth.write("}" + RET);
            
                switch(radiusSettings.getAuthenticationMethod())
                {
                case "PAP":
                    peers.write("require-pap" + RET);
                    break;
                case "CHAP":
                    peers.write("require-chap" + RET);
                    break;
                case "MSCHAPV1":
                    peers.write("require-mschap" + RET);
                    break;
                case "MSCHAPV2":
                    peers.write("require-mschap-v2" + RET);
                    break;
                default:
                    // If we don't recognize just enable debug which might work since it will
                    // accept any authentication protocol, and if not at least we have a log.
                    peers.write("debug");
                    break;
                }
            }

            server.close();
            client.close();
            xauth.close();
            peers.close();
        }

        catch (Exception exn) {
            logger.warn("Exception writing /etc/radiusclient configuration files" + exn);
        }
    }
}
