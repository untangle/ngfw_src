/**
 * $Id: DirectoryConnectorApp.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
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
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.GroupMatcher;
import com.untangle.uvm.app.DomainMatcher;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * Directory connector application
 */
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
     * Directory Connector app constructor
     *
     * @param appSettings
     *      Application settings
     * @param appProperties
     *      Application properties
     */
    public DirectoryConnectorApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);
    }

    /**
     * Load servlets for:
     * * Oauth
     * * API
     * * Old API
     *
     * @param isPermanentTransition
     *      true if permanant transition
     */
    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        /* Start the servlet */
        UvmContextFactory.context().tomcatManager().loadServlet("/" + OAUTH_WEBAPP, OAUTH_WEBAPP);
        UvmContextFactory.context().tomcatManager().loadServlet("/" + USERAPI_WEBAPP, USERAPI_WEBAPP);
        UvmContextFactory.context().tomcatManager().loadServlet("/" + USERAPI_WEBAPP_OLD, USERAPI_WEBAPP); //load the old URL for backwards compat
    }

    /**
     * Shutdown servlets for:
     * * Oauth
     * * API
     * * Old API
     *
     * @param isPermanentTransition
     *      true if permanant transition
     */
    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().tomcatManager().unloadServlet("/" + OAUTH_WEBAPP);
        UvmContextFactory.context().tomcatManager().unloadServlet("/" + USERAPI_WEBAPP);
        UvmContextFactory.context().tomcatManager().unloadServlet("/" + USERAPI_WEBAPP_OLD);
    }

    /**
     * Load settings and perform setting conversions.
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        DirectoryConnectorSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/directory-connector/" + "settings_" + appID + ".js";

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

            /* 13.1 conversion */
            if ( readSettings.getVersion() < 2 ) {
                convertV1toV2Settings( readSettings );
                this.setSettings( readSettings );
            }

            /* 13.1 - convert ouFilter to array */
            String ouFilter = readSettings.getActiveDirectorySettings().getOUFilter();
            if(!ouFilter.equals("") ){
                LinkedList<String> ouFilters = new LinkedList<>();
                ouFilters.add(ouFilter);
                readSettings.getActiveDirectorySettings().setOUFilters(ouFilters);
                readSettings.getActiveDirectorySettings().setOUFilter("");
                setSettings( readSettings );
            }

            if ( readSettings.getVersion() < 3 ) {
                convertV2toV3Settings( readSettings );
                this.setSettings( readSettings );
            }

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    /**
     * Return connectors.
     *
     * @return
     *      PipelineConnector[]  Array of connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Get directory connector settings.
     *
     * @return DirectoryConnectorSettings
     *
     */
    public DirectoryConnectorSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set directory connector settings.
     *
     * @param newSettings
     *      New settings to configure.
     */
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
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "directory-connector/" + "settings_" + appID + ".js", newSettings );
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

    /**
     * Get Active Directory manager.
     *
     * @return
     *      Active Directory manager
     */
    public ActiveDirectoryManagerImpl getActiveDirectoryManager()
    {
        return this.activeDirectoryManager;
    }

    /**
     * Get RADIUS manager.
     *
     * @return
     *      RADIUS manager
     */
    public RadiusManagerImpl getRadiusManager()
    {
        return this.radiusManager;
    }

    /**
     * Get Google manager.
     *
     * @return
     *      Google manager
     */
    public GoogleManagerImpl getGoogleManager()
    {
        return this.googleManager;
    }

    /**
     * Get all users from all AD servers for rule condtions
     *
     * @return
     *      List of users.
     */
    public List<UserEntry> getRuleConditonalUserEntries()
    {
        LinkedList<UserEntry> users = new LinkedList<>();

        /* add all AD users */
        boolean found;
        try{
            List<UserEntry> adUsers = activeDirectoryManager.getUserEntries(null);
            for(UserEntry adUser: adUsers){
                found = false;
                for(UserEntry user: users ){
                    if(user.getUid().equals(adUser.getUid())){
                        found = true;
                        break;
                    }
                }

                if(found == false){
                    users.add(adUser);
                }
            }
        }
        catch( Exception e ){
            logger.warn(e.getMessage());
        }

        return users;
    }

    /**
     * Get all groups from all AD for rule conditions
     *
     * @return
     *      List of groups.
     */
    public List<GroupEntry> getRuleConditionalGroupEntries()
    {
        LinkedList<GroupEntry> groups = new LinkedList<>();

        /* add all AD groups */
        boolean found;
        List<GroupEntry> adGroups = activeDirectoryManager.getGroupEntries(null, true);
        for(GroupEntry adGroup: adGroups){
            found = false;
            for( GroupEntry group : groups){
                if(group.getCN().equals(adGroup.getCN())){
                    found = true;
                    break;
                }
            }
            if( found == false ){
                groups.add(adGroup);
            }
        }

        return groups;
    }

    /**
     * Get all domains from all AD for rule conditions
     *
     * @return
     *      List of groups.
     */
    public List<String> getRuleConditionalDomainEntries()
    {
        return activeDirectoryManager.getDomains();
    }

    /**
     * Authenticate against all servers.
     *
     * @param username
     *      Username to authenticate.
     * @param pwd
     *      Username password.
     * @return
     *      true if user authenticated.
     */
    public boolean authenticate(String username, String pwd)
    {
        if (activeDirectoryAuthenticate(username, pwd)) return true;
        if (radiusAuthenticate(username, pwd)) return true;

        return false;
    }

    /**
     * Authenticate only against active Directory servers.
     *
     * @param username
     *      Username to authenticate.
     * @param pwd
     *      Username password.
     * @return
     *      true if user authenticated.
     */
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

    /**
     * Authenticate only against Radius server.
     *
     * @param username
     *      Username to authenticate.
     * @param pwd
     *      Username password.
     * @return
     *      true if user authenticated.
     */
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

    /**
     * Authenticate against any server including local.
     *
     * @param username
     *      Username to authenticate.
     * @param pwd
     *      Username password.
     * @return
     *      true if user authenticated.
     */
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
        return false;
    }

    /**
     * Determine if user is part of a domain using string.
     *
     * @param user
     *  Username string to lookup.
     * @param domain
     *  Domain string to lookup
     * @return
     *      true if user is in specified domain, false otherwise
     */
    public boolean isMemberOfDomain(String user, String domain){
        if (!isLicenseValid()) {
            return false;
        }
        return this.groupManager.isMemberOfDomain(user, domain);
    }

    /**
     * Determine if user is part of a domain using a matcher
     *
     * @param user
     *  Username string to lookup.
     * @param domainMatcher
     *  Domain marcher to compare
     * @return
     *      true if user is in specified domain, false otherwise
     */
    public boolean isMemberOfDomain(String user, DomainMatcher domainMatcher){
        if (!isLicenseValid()) {
            return false;
        }
        return this.groupManager.isMemberOfDomain(user, domainMatcher);
    }

    /**
     * Determine if user is part of any domain.
     *
     * @param user
     *      Username to check.
     * @return
     *      String list of domains this username belongs in.
     */
    public List<String> memberOfDomain(String user)
    {
        if (!isLicenseValid()) {
            return new LinkedList<>();
        }

        return this.groupManager.memberOfDomain(user);
    }

    /**
     * Determine if user is part of a group.
     *
     * @param user
     *      Username to check.
     * @param group
     *      Name of group to check.
     * @return
     *      true if user is member, false otherwise.
     */
    public boolean isMemberOfGroup(String user, String group)
    {
        if (!isLicenseValid()) {
            return false;
        }

        return this.groupManager.isMemberOfGroup(user, group);
    }

    /**
     * Determine if user is part of a group.
     *
     * @param user
     *      Username to check.
     * @param groupMatcher
     *      GroupMatcher to check.
     * @return
     *      true if user is member, false otherwise.
     */
    public boolean isMemberOfGroup(String user, GroupMatcher groupMatcher)
    {
        if (!isLicenseValid()) {
            return false;
        }

        return this.groupManager.isMemberOfGroup(user, groupMatcher);
    }

    /**
     * Determine if user is part of any group.
     *
     * @param user
     *      Username to check.
     * @return
     *      String list of groups  this username belongs in.
     */
    public List<String> memberOfGroup(String user)
    {
        if (!isLicenseValid()) {
            return new LinkedList<>();
        }

        return this.groupManager.memberOfGroup(user);
    }

    /**
     * Return user group list for a domain.
     *
     * @param user
     *      Username to check.
     * @param domain
     *      Domain to check.
     * @return
     *      String list of groups  this username belongs in.
     */
    public List<String> memberOfGroup(String user, String domain)
    {
        if (!isLicenseValid()) {
            return new LinkedList<>();
        }

        return this.groupManager.memberOfGroup(user, domain);
    }

    /**
     * Refresh group cache by querying servers.
     */
    public void refreshGroupCache()
    {
        this.groupManager.refreshGroupCache();
    }

    /**
     * Determine if Google drive is configured.
     *
     * @return
     *      true if Google Drive is configured, false otherwise.
     */
    public boolean isGoogleDriveConnected()
    {
        return getGoogleManager().isGoogleDriveConnected();
    }

    /**
     * Initalize Directory Connector settings.
     */
    @Override
    public void initializeSettings()
    {
        this.settings = new DirectoryConnectorSettings();
        this.settings.setVersion(2);
        this.settings.setActiveDirectorySettings(new ActiveDirectorySettings("Administrator", "mypassword", "mydomain.int", "ad_server.mydomain.int", 636, true ));
        this.settings.setRadiusSettings(new RadiusSettings(false, "1.2.3.4", 1812, 1813, "mysharedsecret", "PAP"));
        this.settings.setGoogleSettings(new GoogleSettings());
        setSettings(this.settings);
    }

    /**
     * Convert v1 to v2 settings.
     *
     * @param settings
     *      Settings to convert.
     */
    private void convertV1toV2Settings( DirectoryConnectorSettings settings )
    {
        if (settings.getVersion() != 1) {
            logger.warn("Invalid version to convert: " + settings.getVersion());
            return;
        }
        settings.setVersion(2);
        /**
         * Allowing manual specification is the old behavior
         * On upgrade, keep the old behavior
         */
        settings.setApiManualAddressAllowed( true );
    }

    /**
     * Convert v2 to v3 settings.
     *
     * Namely, Active Directory server from a singleton to first entry in the new list.
     *
     * @param settings
     *      Settings to convert.
     */
    private void convertV2toV3Settings( DirectoryConnectorSettings settings )
    {
        if (settings.getVersion() != 2) {
            logger.warn("Invalid version to convert: " + settings.getVersion());
            return;
        }
        settings.setVersion(3);

        ActiveDirectorySettings adSettings = settings.getActiveDirectorySettings();
        if(adSettings.getSuperuser() != null ){
            ActiveDirectoryServer adServer = new ActiveDirectoryServer(
                adSettings.getSuperuser(),
                adSettings.getSuperuserPass(),
                adSettings.getDomain(),
                adSettings.getLDAPHost(),
                adSettings.getLDAPPort(),
                adSettings.getLDAPSecure(),
                adSettings.getOUFilters(),
                false
            );
            adServer.setEnabled( adSettings.getEnabled() );
            adSettings.setSuperuser(null);
            adSettings.setSuperuserPass(null);
            adSettings.setDomain(null);
            adSettings.setLDAPHost(null);
            adSettings.setLDAPPort(-1);
            adSettings.setOUFilters(null);

            LinkedList<ActiveDirectoryServer> adServers = new LinkedList<>();
            adServers.push(adServer);
            adSettings.setServers( adServers );
        }
    }

    /**
     * Reconfigure Directory Connector and all connections.
     */
    private synchronized void reconfigure()
    {
        /**
         * Initialize the Active Directory manager (or update settings on
         * current)
         */
        if (activeDirectoryManager == null) {
            this.activeDirectoryManager = new ActiveDirectoryManagerImpl(settings.getActiveDirectorySettings(), this);
        } else{
            this.activeDirectoryManager.setSettings(settings.getActiveDirectorySettings());
        }

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
         * Initialize the Group manager (if necessary) and Refresh
         */
        if (groupManager == null) {
            this.groupManager = new GroupManager(this);
            this.groupManager.start();
        }else{
            this.refreshGroupCache();
        }
        this.updateRadiusClient(settings.getRadiusSettings());
    }

    /**
     * Update Radius client with new settings.
     *
     * @param radiusSettings
     *      New Radius settings to configure client.
     */
    private void updateRadiusClient(RadiusSettings radiusSettings)
    {
        /**
         * The IPsec app uses the radiusclient1 package for L2TP RADIUS support so
         * we update those files and we create strongswan.radius for Xauth support.
         * We also create /etc/ppp/peers/radius-auth-proto which tells xl2tpd/pppd
         * which authentication method (PAP, CHAP, MS-CHAP v1, MS-CHAP v2) to require.
         */

        FileWriter server = null;
        FileWriter client = null;
        FileWriter xauth = null;
        FileWriter peers = null;
        try {
            server = new FileWriter("/etc/radiusclient/servers", false);
            client = new FileWriter("/etc/radiusclient/radiusclient.conf", false);
            xauth = new FileWriter("/etc/strongswan.radius", false);
            peers = new FileWriter("/etc/ppp/peers/radius-auth-proto", false);

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

        }catch (Exception exn) {
            logger.warn("Exception writing /etc/radiusclient configuration files" + exn);
        }finally{
            if(server != null){
                try{
                    server.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
            if(client != null){
                try{
                    client.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
            if(xauth != null){
                try{
                    xauth.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
            if(peers != null){
                try{
                    peers.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
        }
    }
}
