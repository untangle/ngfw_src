/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

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
    private LinkedList<ActiveDirectoryLdapAdapter> adAdapters = new LinkedList<>();

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
     * @param domain
     *  Domain to pull users from.
     * @return List of users
     * @throws ServiceUnavailableException
     *  If unable to contact server
     */
    public List<UserEntry> getUserEntries(String domain)
        throws ServiceUnavailableException
    {
        if (!app.isLicenseValid()){
            return new LinkedList<>();
        }

        List<UserEntry> userList = new LinkedList<>();

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(!adAdapter.getSettings().getEnabled()){
                continue;
            }
            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                userList.addAll(adAdapter.listAll());
            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Users.",x);
            }
        }

        Collections.sort(userList);

        return userList;
    }

    /**
     * Get domains from settings
     *
     * @return
     *  List of string of domain names.
     */
    public List<String> getDomains(){
        LinkedList<String> domains = new LinkedList<>();
        if (!app.isLicenseValid()){
            return domains;
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(!adAdapter.getSettings().getEnabled()){
                continue;
            }
            domains.push(adAdapter.getSettings().getDomain());
        }
        return domains;
    }

    /**
     * Get user entries from all servers
     *
     * @param domain
     *  Name of domain to pull.
     * @param groupName 
     *  Group to search.
     * @return
     *  List of user entries
     * @throws ServiceUnavailableException
     *  If unable to contact server.
     */
    public List<UserEntry> getGroupUsers( String domain, String groupName )
        throws ServiceUnavailableException
    {
        if (!app.isLicenseValid()){
            return new LinkedList<>();
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(!adAdapter.getSettings().getEnabled()){
                continue;
            }
            if(domain != null && !adAdapter.getSettings().getDomain().equals(domain)){
                continue;
            }

            try {
                return adAdapter.listGroupUsers(groupName);
            }  catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Groups.",x);
            }
        }

        return new LinkedList<>();
    }

    /**
     * Get group entries from all servers
     *
     * @param domain
     *  Domain name to search.
     * @param fetchMemberOf
     *  If true, pull user entires too.
     * @return List of groups
     */
    public List<GroupEntry> getGroupEntries( String domain, boolean fetchMemberOf )
    {
        List<GroupEntry> groupList = new LinkedList<>();

        if (!app.isLicenseValid()){
            return new LinkedList<>();
        }

        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(!adAdapter.getSettings().getEnabled()){
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
        String authUsername = null;
        String authDomain = null;
        if(username.contains("\\")){
            String[] domainUsername = username.split("\\\\");
            authDomain = domainUsername[0];
            authUsername = domainUsername[1];
        }
        if(username.contains("@")){
            String[] domainUsername = username.split("@");
            authUsername = domainUsername[0];
            authDomain = domainUsername[1];
        }

        boolean azure = false;
        for(ActiveDirectoryLdapAdapter adAdapter : this.adAdapters){
            if(adAdapter == null){
                continue;
            }
            if(!adAdapter.getSettings().getEnabled()){
                continue;
            }
            azure = adAdapter.getSettings().getAzure();
            if(!azure){
                /**
                 * Azure usernames can be in different domains, so don't do specific matching.
                 */
                if(authDomain != null ){
                    if(!adAdapter.getSettings().getDomain().equals(authDomain) &&
                    !adAdapter.getSettings().getDomain().startsWith(authDomain+".") ){
                        continue;
                    }
                }
            }

            try {
                if(adAdapter.authenticate(azure ? username : authUsername, pwd)) {
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
     * @throws ServiceUnavailableException
     *  If server is unreachable
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
            if(!adAdapter.getSettings().getEnabled()){
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
     * @throws ServiceUnavailableException
     *  If unable to access server.
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
            if(!adAdapter.getSettings().getEnabled()){
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
                        jsonUser.put("groups", listToString(app.memberOfGroup(user.getUid(), domain)));
                        result.put(jsonUser);
                    } catch (Exception x){
                        logger.warn("Unable to query Active Directory Users.",x);
                    }
                }

            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Users.",x);
            }
        }

        return result;
    }

}
