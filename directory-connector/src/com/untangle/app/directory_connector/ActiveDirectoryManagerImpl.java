/*
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.lang.String;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import javax.naming.ServiceUnavailableException;
import org.apache.log4j.Logger;

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
     * The node that owns this manager
     */
    private DirectoryConnectorApp node;
    
    /**
     * The LDAP adapter
     */
    private ActiveDirectoryLdapAdapter adAdapter;
    
    public ActiveDirectoryManagerImpl( ActiveDirectorySettings settings, DirectoryConnectorApp node )
    {
        this.node = node;
        
        setSettings( settings );
    }

    public void setSettings( ActiveDirectorySettings settings )
    {
        this.currentSettings = settings;
        this.adAdapter = new ActiveDirectoryLdapAdapter( settings );
    }

    public List<UserEntry> getActiveDirectoryUserEntries()
        throws ServiceUnavailableException
    {
        if (!node.isLicenseValid())
            return new LinkedList<UserEntry>();
        if (!currentSettings.getEnabled())
            return new LinkedList<UserEntry>();
        
        ActiveDirectoryLdapAdapter adAdapter = this.adAdapter;
       
        List<UserEntry> userList = new LinkedList<UserEntry>();

        if(adAdapter != null) {
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

    public List<UserEntry> getActiveDirectoryGroupUsers( String groupName ) 
        throws ServiceUnavailableException
    {
        ActiveDirectoryLdapAdapter adAdapter = this.adAdapter;

        if (!node.isLicenseValid())
            return new LinkedList<UserEntry>();
        if (!currentSettings.getEnabled())
            return new LinkedList<UserEntry>();
        
        if(adAdapter != null) {
            try {
                return adAdapter.listGroupUsers(groupName);
            }  catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory Groups.",x);
                throw new ServiceUnavailableException(x.getMessage());
            }
        }

        return new LinkedList<UserEntry>();
    }
    
    public List<GroupEntry> getActiveDirectoryGroupEntries( boolean fetchMemberOf )
    {
        ActiveDirectoryLdapAdapter adAdapter = this.adAdapter;
        List<GroupEntry> groupList = new LinkedList<GroupEntry>();

        if (!node.isLicenseValid())
            return new LinkedList<GroupEntry>();
        if (!currentSettings.getEnabled())
            return new LinkedList<GroupEntry>();
        
        if(adAdapter != null) {
            try {
                groupList.addAll(adAdapter.listAllGroups(fetchMemberOf));
            } catch (ServiceUnavailableException x) {
                logger.warn("Unable to query Active Directory group.", x);
            }
        }

        Collections.sort(groupList);
        return groupList;
    }

    public String getActiveDirectoryStatusForSettings( DirectoryConnectorSettings newSettings )
    {
        String status;

        if (newSettings == null || newSettings.getActiveDirectorySettings() == null) {
            return "Invalid settings (null)";
        }

        ActiveDirectorySettings testSettings = newSettings.getActiveDirectorySettings();

        try {
            logger.info("Testing Active Directory settings for (" +
                        "server='" + testSettings.getLDAPHost() + "', " +
                        "secure='" + testSettings.getLDAPSecure() + "', " +
                        "port='" + testSettings.getLDAPPort() + "', " + 
                        "superuser='" + testSettings.getSuperuser() + "', " + 
                        "domain='" + testSettings.getDomain() + "', " + 
                        "ou_filter='" + testSettings.getOUFilter() + "')");

            ActiveDirectoryLdapAdapter temp_adAdapter = new ActiveDirectoryLdapAdapter(testSettings);
            List<UserEntry> adRet = temp_adAdapter.listAll();
            return "Active Directory authentication success!";
        } catch (Exception e) {
            logger.warn("Active Directory Test Failure", e);
            String statusStr = "Active Directory authentication failed: <br/><br/>"+ e.getMessage();
            return statusStr.replaceAll("\\p{Cntrl}", "");  //remove non-printable chars
        }
    }

    public boolean authenticate( String username, String pwd )
    {
        if(this.adAdapter == null)
            return false;
        if (!currentSettings.getEnabled())
            return false;
        
        try {
            if(adAdapter.authenticate(username, pwd)) {
                return true;
            }
        } catch (ServiceUnavailableException x) {
            logger.warn("Active Directory authenticate failed: ", x);
            return false;
        }
            
        return false;                
    }
  
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
  
    public Map<String,String> getUserGroupMap() 
    {
        Map<String,String> result = new HashMap<String,String>();
        try{
            List<UserEntry> users = getActiveDirectoryUserEntries();
            for (UserEntry u: users) {
                result.put(u.getUid(), listToString(node.memberOf(u.getUid())));
            }
        }catch( ServiceUnavailableException e ){
            logger.warn(e.getMessage());
        }
        return result;
    }

}
