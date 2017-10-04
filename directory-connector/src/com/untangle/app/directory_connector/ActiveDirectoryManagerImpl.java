/*
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.lang.String;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import javax.naming.ServiceUnavailableException;
import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ActiveDirectoryManagerImpl provides the API implementation of all Active Directory related functionality
 */
public class ActiveDirectoryManagerImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This is just a copy of the current settings being used
     */
    private ActiveDirectorySettings currentSettings;

    /**
     * The app that owns this manager
     */
    private DirectoryConnectorApp app;

    /**
     * List of LDAP adapters
     */
    private LinkedList<ActiveDirectoryLdapAdapter> adAdapters = new LinkedList<ActiveDirectoryLdapAdapter>();

    /**
     * Initialize AD manager
     *
     * @param settings  Active directory settings.
     * @param app       Directory Connector Application
     */
    public ActiveDirectoryManagerImpl( ActiveDirectorySettings settings, DirectoryConnectorApp app )
    {
        this.app = app;

        setSettings( settings );
    }

    /**
     * Configure AD settings.
     *
     * Includes building the server adapters.
     *
     * @param settings  Active Directory settings.
     */
    public void setSettings( ActiveDirectorySettings settings )
    {
        this.currentSettings = settings;

        this.adAdapters.clear();
        for(ActiveDirectoryServer adsSettings : settings.getServers()){
            this.adAdapters.add( new ActiveDirectoryLdapAdapter( adsSettings ) );
        }
    }

    /**
     * Get user entries from all servers.
     *
     * @return List of users
     */
    public List<UserEntry> getUserEntries(String domain)
        throws ServiceUnavailableException
    {
        if (!app.isLicenseValid()){
            return new LinkedList<UserEntry>();
        }

        List<UserEntry> userList = new LinkedList<UserEntry>();

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }
            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                userList.addAll(adAdapter.listAll());
            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Users.",x);
                throw new ServiceUnavailableException(x.getMessage());
            }
        }

        Collections.sort(userList);

        return userList;
    }

    // get domains from settings
    public List<String> getDomains(){
        LinkedList<String> domains = new LinkedList<String>();
        if (!app.isLicenseValid()){
            return domains;
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }
            domains.push(adAdapter.getSettings().getDomain());
        }
        return domains;
    }

    /**
     * Get user entries from all servers
     *
     * @param groupName Group to search.
     * @return List of users
     */
    public List<UserEntry> getGroupUsers( String domain, String groupName )
        throws ServiceUnavailableException
    {
        if (!app.isLicenseValid()){
            return new LinkedList<UserEntry>();
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }
            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                return adAdapter.listGroupUsers(groupName);
            }  catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Groups.",x);
                throw new ServiceUnavailableException(x.getMessage());
            }
        }

        return new LinkedList<UserEntry>();
    }

    /**
     * Get group entries from all servers
     *
     * @param fetchMemberOf  ???
     * @return List of groups
     */
    public List<GroupEntry> getGroupEntries( String domain, boolean fetchMemberOf )
    {
        List<GroupEntry> groupList = new LinkedList<GroupEntry>();

        if (!app.isLicenseValid()){
            return new LinkedList<GroupEntry>();
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }
            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                groupList.addAll(adAdapter.listAllGroups(fetchMemberOf));
            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory group.", x);
            }
        }

        Collections.sort(groupList);
        return groupList;
    }

    /**
     * Get AD server status with the specified server settings.
     *
     * @param testServerSettings Server settings to test.
     * @return String containing status of connection.
     */
    public String getStatusForSettings( ActiveDirectoryServer testServerSettings )
    {
        String status;

        if (testServerSettings == null ){
            return "Invalid settings (null)";
        }

        try {
            String ouFiltersString = "";
            for(String ouFilter : testServerSettings.getOUFilters()){
                ouFiltersString += (ouFiltersString.length() > 0 ? "," : "") + ouFilter;
            }
            logger.info("Testing Active Directory settings for (" +
                        "server='" + testServerSettings.getLDAPHost() + "', " +
                        "secure='" + testServerSettings.getLDAPSecure() + "', " +
                        "port='" + testServerSettings.getLDAPPort() + "', " +
                        "superuser='" + testServerSettings.getSuperuser() + "', " +
                        "domain='" + testServerSettings.getDomain() + "', " +
                        "ou_filters='" + ouFiltersString + "')");

            ActiveDirectoryLdapAdapter temp_adAdapter = new ActiveDirectoryLdapAdapter(testServerSettings);
            List<UserEntry> adRet = temp_adAdapter.listAll();
            return "Active Directory authentication success!";
        } catch (Exception e) {
            logger.warn("Active Directory Test Failure", e);
            String statusStr = "Active Directory authentication failed: <br/><br/>"+ e.getMessage();
            return statusStr.replaceAll("\\p{Cntrl}", "");  //remove non-printable chars
        }
    }

    /**
     * Perform authentication against all servers with the specified username and password.
     *
     * @param username Username to authenticate.
     * @param pwd Password for the user.
     * @return true if authenticated against a server, false if not.
     */
    public boolean authenticate( String username, String pwd )
    {
        String domain = null;
        if(username.contains("\\")){
            String[] domainUsername = username.split("\\\\");
            domain = domainUsername[0];
            username = domainUsername[1];
        }
        if(username.contains("@")){
            String[] domainUsername = username.split("@");
            username = domainUsername[0];
            domain = domainUsername[1];
        }
        logger.warn("authenticate: username=" + username + ", domain=" + domain + ", password=" + pwd);

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }
            if(domain != null ){
                if(adAdapter.getSettings().getDomain().equals(domain) == false &&
                   adAdapter.getSettings().getDomain().startsWith(domain+".") == false ){
                    continue;
                }
            }

            logger.warn("authenticate:" + adAdapter.getSettings().getDomain() );
            try {
                if(adAdapter.authenticate(username, pwd)) {
                    return true;
                }
            } catch (ServiceUnavailableException x) {
                logger.warn("Active Directory authenticate failed: ", x);
                return false;
            }
        }

        return false;
    }

    /**
     * Convert list of strings to a single string for the purpose of...what?
     *
     * @param input Input list.
     * @return String of list joined by commas.
     */
    private String listToString(List<String> input) 
    {
        StringBuffer sb = new StringBuffer();
        int index = 0;
        for (String s: input) {
            if ( index > 0) {
                sb.append(",");
            }
            sb.append(s);
            index++;
        }
        return sb.toString();
    }

    /**
     * Get user entries in JSON array format, suitable for UI display.
     *
     * @param domain
     *      Domain to query.  If null, all domains.
     * @return JSONArray of users with each entry containing fields uid, domain.
     */
    public JSONArray getUsers( String domain)
        throws ServiceUnavailableException
    {
        JSONArray result = new JSONArray();
        if (!app.isLicenseValid()){
            return result;
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }

            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                for(UserEntry user : adAdapter.listAll()){
                    try{
                        JSONObject jsonUser = new JSONObject();
                        jsonUser.put("uid", user.getUid());
                        jsonUser.put("domain", adAdapter.getSettings().getDomain());
                        result.put(jsonUser);
                    } catch (Exception x){
                        logger.warn("Unable to query Active Directory Users.",x);
                    }
                }

            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Users.",x);
                throw new ServiceUnavailableException(x.getMessage());
            }
        }
        return result;
    }

    /**
     * Get user-group map entries in JSON array format, suitable for UI display.
     *
     * @param domain
     *      Domain to query.  If null, all domains.
     * @return JSONArray of users with each entry containing fields uid, groups (comma separated list), domain.
     */
    public JSONArray getUserGroupMap(String domain)
        throws ServiceUnavailableException
    {
        JSONArray result = new JSONArray();

        if (!app.isLicenseValid()){
            return result;
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(adAdapter.getSettings().getEnabled() == false){
                continue;
            }
            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                for(UserEntry user : adAdapter.listAll()){
                    try{
                        JSONObject jsonUser = new JSONObject();
                        jsonUser.put("uid", user.getUid());
                        jsonUser.put("domain", adAdapter.getSettings().getDomain());
                        jsonUser.put("groups", listToString(app.memberOfGroup(user.getUid())));
                        result.put(jsonUser);
                    } catch (Exception x){
                        logger.warn("Unable to query Active Directory Users.",x);
                    }
                }

            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Users.",x);
                throw new ServiceUnavailableException(x.getMessage());
            }
        }

        return result;
    }

}
