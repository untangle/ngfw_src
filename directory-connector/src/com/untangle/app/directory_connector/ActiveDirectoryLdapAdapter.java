/**
 * $Id: ActiveDirectoryLdapAdapter.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;

import com.untangle.app.directory_connector.GroupEntry;
import com.untangle.app.directory_connector.ActiveDirectoryServer;
import com.untangle.app.directory_connector.UserEntry;

/**
 * Implementation of the Ldap adapter which understands uniqueness of
 * ActiveDirectory.
 */
class ActiveDirectoryLdapAdapter extends LdapAdapter
{
    private final Logger logger = Logger.getLogger(ActiveDirectoryLdapAdapter.class);

    private ActiveDirectoryServer settings;

    private String userAccountControl;
    
    private static final String[] USER_CLASS_TYPE = { "user" };
    private static final String[] GROUP_CLASS_TYPE = { "group" };
    private static final String AZURE_USERS_OU = "OU=AADDC Users";

    /**
     * Construct a new AD adapter with the given settings
     * 
     * @param settings
     *      AD server settings
     */
    public ActiveDirectoryLdapAdapter(ActiveDirectoryServer settings)
    {
        this.settings = settings;
    }

    /**
     * Get AD server settings.
     *
     * @return ActiveDirectoryServer settings.
     */
    @Override
    protected final ActiveDirectoryServer getSettings()
    {
        return settings;
    }

    /**
     * Get user class type.
     *
     * @return String of user class type.
     */
    @Override
    protected String[] getUserClassType()
    {
        return USER_CLASS_TYPE;
    }

    /**
     * Get group class type.
     *
     * @return String of group class type.
     */
    @Override
    protected String[] getGroupClassType()
    {
        return GROUP_CLASS_TYPE;
    }

    /**
     * Get mail attribute name.
     *
     * @return fixed string "mail".
     */
    @Override
    protected String getMailAttributeName()
    {
        return "mail";
    }
    
    /**
     * Get primary group id attribute.
     *
     * @return fixed string "primaryGroupID".
     */
    @Override
    protected String getPrimaryGroupIDAttribute()
    {
        return "primaryGroupID";
    }

    /**
     * Get full name attribute name.
     *
     * @return fixed string "cn".
     */
    @Override
    protected String getFullNameAttributeName()
    {
        return "cn";
    }

    /**
     * Get UID attribute name.
     *
     * @return fixed string "sAMAccountName".
     */
    @Override
    protected String getUIDAttributeName()
    {
        return "sAMAccountName";// Dated, but seems to be what windows uses
    }

    /**
     * Get AD SuperUser DN
     *
     * @return String in format of user@domain.
     */
    @Override
    public String getSuperuserDN()
    {
        String sustring = settings.getSuperuser();
        if (sustring.toUpperCase().startsWith("CN="))
            return sustring + "," + domainComponents(settings.getDomain());
        if (sustring.contains("@") || sustring.contains("\\") )
            return sustring;
        return sustring + "@" + settings.getDomain();
    }

    /**
     * Get search bases
     *
     * @return List of strings with ou filters.
     */
    @Override
    public List<String> getSearchBases()
    {
        ArrayList<String> bases = new ArrayList<>();

        List<String> ouFilters = settings.getOUFilters();

        for( String ouFilter: ouFilters ){
            if (ouFilter != null){
                bases.add(ouFilter + (!"".equals(ouFilter) ? ",": "") + domainComponents(settings.getDomain()));
            }
        }

        if( bases.isEmpty()){
            if(settings.getAzure()){
                bases.add(AZURE_USERS_OU + "," + domainComponents(settings.getDomain()));
            }
            bases.add(domainComponents(settings.getDomain()));
        }
        return bases;
    }

    /**
     * Detmine if GID is filtered (below 1 million)
     *
     * @param gid
     *      Group ID to test.
     * @return true if below limit, false otherise.
     */
    @Override
    protected boolean filterGID(int gid)
    {
        return gid < 1000000;
    }

    /**
     * Authenticate user.
     *
     * @param uid
     *      UID to authenticate.
     * @param pwd
     *      Password
     * @return true if authenciated, false otherwise.
     * @throws ServiceUnavailableException
     */
    @Override
    public boolean authenticate(String uid, String pwd)
            throws ServiceUnavailableException
    {
        if (uid == null || uid.equals("") || pwd == null || pwd.equals("")) {
            // No blank uids or passwords.
            return false;
        }

        // First, we need the "CN" for the user from uid.
        // For 4.2.2 -- now we know that we have to use the exact DN
        // we can't start the search again at the search base (we must specify
        // the subdirectory exactly, which is easy since we looked it up). jdi
        String dn = getDNFromUID(uid);
        if (dn == null) {
            return false;// throw new
            // AuthenticationException("Unable to get DN from sAMAccountName");
        }

        try {
            if (logger.isDebugEnabled())
                logger.debug("Trying AD authentication of uid " + uid + " (" + dn + ")");

            DirContext ctx = createContext(getSettings().getLDAPHost(), getSettings().getLDAPPort(), getSettings().getLDAPSecure(), dn, pwd);
            boolean ret = ctx != null;
            closeContext(ctx);
            return ret;
        } catch (AuthenticationException ex) {
            return false;
        } catch (NamingException ex) {
            logger.warn("Exception authenticating user \"" + uid + "\"", ex);
            throw new ServiceUnavailableException(ex.toString());
        }
    }

    /**
     * Get all of the groups that are available for this adapter.
     *
     * @param fetchMembersOf 
     *      Set to true to indicate that the entries should include the list of groups that the group is a member of.
     * @return List of all groups
     * @throws ServiceUnavailableException if unable to access server.
     */
    @Override
    public List<GroupEntry> listAllGroups( boolean fetchMembersOf ) 
        throws ServiceUnavailableException
    {
        try {
            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 getListAllGroupsSearchString(),
                                 getGroupEntrySearchControls(fetchMembersOf));

            List<GroupEntry> ret = new ArrayList<>();

            if(list == null || list.isEmpty()) {
                return ret;
            }

            for(Map<String, String[]> map : list) {
                GroupEntry entry = toGroupEntry(map);
                if(entry != null && !ret.contains(entry)) {
                    ret.add(entry);
                }
            }
            Collections.sort(ret);
            return ret;
        }
        catch(NamingException ex) {
            logger.warn("Exception listing entries", ex);
            throw new ServiceUnavailableException(ex.toString());
        }
    }
    
    /**
     * Return string for group search.
     *
     * @param group
     *      Group ID to test.
     * @param fetchMembersOf
     *      true to get users, false otherwise.
     * @return GroupEntry containing group information.
     * @throws ServiceUnavailableException if unable to access AD server.
     */
    public GroupEntry getGroupEntry( String group, boolean fetchMembersOf ) throws ServiceUnavailableException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(&");
        sb.append(joinCriteria("|","objectClass=", getGroupClassType()));
        sb.append("(").append("CN=").append(group).append(")");
        sb.append(")");
        return getGroupEntryWithSearchString(sb.toString(), fetchMembersOf);
    }
        
    /**
     * Get all of the groups that a user belongs to.
     * @param
     *      user The username to query.
     * @return A List of all of the groups that a user belongs to.
     * @throws ServiceUnavailableException if cannot access server.
     */
    public List<GroupEntry> listUserGroups( String user ) throws ServiceUnavailableException
    {
        /* First retrieve the user object */
        UserEntry userEntry = getEntry(user);
        
        if ( userEntry == null ) {
            logger.debug( "Unable to query an user entry for the user: '" + user + "'" );
            return new ArrayList<>();
        }
        
        /* Next retrieve all of the groups that the user belongs to, this doesn't recurse */
        try {
            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 getListGroupsSearchString(userEntry),
                                 getGroupEntrySearchControls(false));

            List<GroupEntry> ret = new ArrayList<>();

            if(list == null || list.isEmpty()) {
                return ret;
            }

            for(Map<String, String[]> map : list) {
                GroupEntry entry = toGroupEntry(map);
                if(entry != null) {
                    ret.add(entry);
                }
            }
            return ret;
        }
        catch(NamingException ex) {
            logger.warn("Exception listing entries", ex);
            throw new ServiceUnavailableException(ex.toString());
        }
    }
    
    /**
     * Get all of the users that belong to a group.
     * @param group
     *      Name of the group to query.
     * @return A list of all of the users that belong to a group.
     * @throws ServiceUnavailableException if unable to access server.
     */
    @Override
    public List<UserEntry> listGroupUsers( String group ) throws ServiceUnavailableException
    {
        GroupEntry groupEntry = getGroupEntry(group,false);
        if ( groupEntry == null ) {
            logger.debug( "The group '" + group + "' doesn't exist.");
            return new ArrayList<>();
        }

        try {
            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 getListUsersSearchString(groupEntry, getUserClassType()),
                                 getUserEntrySearchControls());

            List<UserEntry> ret = new ArrayList<>();

            if(list == null || list.isEmpty()) {
                return ret;
            }

            for(Map<String, String[]> map : list) {
                UserEntry entry = toUserEntry(map);
                if(entry != null) {
                    ret.add(entry);
                }
            }
            return ret;
        }
        catch(NamingException ex) {
            logger.warn("Exception listing entries", ex);
            throw new ServiceUnavailableException(ex.toString());
        }        
    }
    
    /**
     * Build LDAP query to get all users.
     *
     * @return LDAP query to get all users.
     */
    @Override
    protected String getListAllUsersSearchString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("(&");
        sb.append(orStrings("objectClass=", getUserClassType()));
        sb.append(getUserAccountControl());
        sb.append(")");

        if ( logger.isDebugEnabled()) {
            logger.debug("the string is" + sb.toString());
        }
        return sb.toString();
    }

    /**
     * Build LDAP query to get all groups.
     *
     * @return LDAP query to get all groups.
     */
    protected String getListAllGroupsSearchString()
    {
        // TODO Some way to filter-out the system groups from AD?!?
        StringBuilder sb = new StringBuilder();
       
        sb.append("(&");
        sb.append(joinCriteria("|", "objectClass=", getGroupClassType()));
        sb.append(")");

        if ( logger.isDebugEnabled()) {
            logger.debug("the string is" + sb.toString());
        }
        return sb.toString();
    }

    /**
     * Get LDAP query to get users for the specified group.
     *
     * @param group
     *      Group to search.
     * @param objectClasses
     *      List of object classes to query.
     * @return LDAP query string.
     */
    private String getListUsersSearchString(GroupEntry group, String [] objectClasses)
    {
        StringBuilder sb = new StringBuilder();
 
        sb.append("(&");
        sb.append(joinCriteria("|","objectClass=", objectClasses));
        sb.append(getUserAccountControl());
        sb.append("(|");
        sb.append("(").append("memberOf=").append(group.getDN()).append(")");
        if ( group.getPrimaryGroupToken() != null ){
            sb.append("(").append("primaryGroupId=").append(group.getPrimaryGroupToken()).append(")");           
        }
        sb.append(")");
        sb.append(")");

        if ( logger.isDebugEnabled()) {
            logger.debug("the string is" + sb.toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Build query for account control.
     *
     * @return LDAP query.
     */
    private String getUserAccountControl()
    {
        // We want to select the following:
        // 512 (NORMAL_ACCOUNT)
        // with all the following permutations
        // +1, +8, +32, +64, +65536
        // and all combinations thereof (so, 512+1, 512+8, 512+1+8, 512+32,
        // 512+32+1, 512+32+1+8 (and so on)
        // userAccountControl codes translate as follows:
        // 66048 = 0x10200 = DONT_EXPIRE_PASSWORD (65536-0x10000) +
        // NORMAL_ACCOUNT (512-0x200)
        // 512 = 0x200 = NORMAL_ACCOUNT
        // 544 = 0x220 = NORMAL_ACCOUNT (512-0x200) + PASSWD_NOTREQD (32-0x0020)
        // 576 = 0x240 = NORMAL_ACCOUNT (512-0x200) + PASSWD_CANT_CHANGE
        // (64-0x0040)
        // 66080 = 0x10220 = DONT_EXPIRE_PASSWORD (65536-0x10000) +
        // NORMAL_ACCOUNT (512-0x200) + PASSWD_NOTREQD (32-0x0020)
        // 66112 = 0x10240 = DONT_EXPIRE_PASSWORD (65536-0x10000) +
        // NORMAL_ACCOUNT (512-0x200) + PASSWD_CANT_CHANGE (64-0x0040)
        // Need to add three more values for each + 1 and + 8 and + 9!
        // For more info, look here: http://support.microsoft.com/kb/305144
        // string should look like this with an entry for each value
        // (|userAccountControl=512)(|(UserAccountControl=513)|(UserAccountControl=514)(UserAccountControl=544))

        if ( this.userAccountControl == null ) {
            int[] values = { 1, 8, 32, 64, 65536 };

            Set<String> valueSet  = new HashSet<>();
            for ( int c = 0 ; c < Math.pow( 2, (double) values.length + 1 ) - 1 ; c++ ) {
                int v = 512;
                for ( int d = 0, bit = 1 ; d <values.length; d++, bit <<= 1 ) {
                    if (( c & bit ) == bit ) { 
                        v |= values[d];
                    }
                }

                valueSet.add(Integer.toString(v));
            }

            logger.debug( "Values has " + valueSet.size()+ " items");
            this.userAccountControl = joinCriteria("|", "userAccountControl=", valueSet.toArray(new String[0]));
        }

        return this.userAccountControl;    
    }
    
    /**
     * Get group entry for search.
     *
     * @param searchStr
     *      Search string.
     * @param fetchMembersOf
     *      true to fetch members of, false otherwise.
     * @return Return null if not found.  If more than one found (!), first is returned.
     * @throws ServiceUnavailableException if server unavailable
     */
    private GroupEntry getGroupEntryWithSearchString(String searchStr, boolean fetchMembersOf)
        throws ServiceUnavailableException
    {
        try {

            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 searchStr,
                                 getGroupEntrySearchControls(fetchMembersOf));


            if(list == null || list.isEmpty()) {
                return null;
            }

            return toGroupEntry(list.get(0));
        }
        catch(NamingException ex) {
            throw new ServiceUnavailableException(ex.toString());
        }
    }

    /**
     * Get LDAP query for groups for the specified user.
     *
     * @param userEntry
     *      User record to search.
     * @return LDAP query.
     */
    private String getListGroupsSearchString(UserEntry userEntry)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("(&");
        sb.append(joinCriteria("|", "objectClass=", getGroupClassType()));
        sb.append("(|");
        sb.append("(member=");
        sb.append(userEntry.getDN());
        sb.append(")");
        
        /*
        String primaryGroupID = userEntry.getPrimaryGroupID();
        if ( primaryGroupID != null ) {
            sb.append("(");
            sb.append("primaryGroupToken=");
            sb.append(primaryGroupID);
            sb.append(")");
        }*/
        
        sb.append(")");
        sb.append(")");

        logger.debug("the string is" + sb.toString());
        return sb.toString();
    }

    /*
     * @Override protected String getListAllUsersSearchString() {
     * 
     * if(System.currentTimeMillis() > 0) { return
     * "(&(objectClass=user)(!cn=Builtin))"; }
     * 
     * StringBuilder sb = new StringBuilder(); sb.append("(&");
     * sb.append("(").append
     * ("objectClass=").append(getSettings().getUserClass()).append(")");
     * sb.append("(").append("cn=Builtin").append(")"); sb.append(")");
     * System.out.println("*************" + sb.toString()); return
     * sb.toString(); }
     */

    /**
     * Get the Distinguished Name from the uid (which we map to
     * "sAMAccountName"). Returns null if none found.
     *
     * @param uid
     *      Username to search.
     * @return LDAP result for query, null if not found.
     * @throws ServiceUnavailableException if server not reachable.
     */
    private String getDNFromUID(String uid) throws ServiceUnavailableException
    {
        try {
            String searchStr = "(&"
                    + orStrings("objectClass=", getUserClassType())
                    + "(" + (settings.getAzure() ? "userPrincipalName" : "sAMAccountName") + "=" + uid + "))";
            SearchResult result = queryFirstAsSuperuser(getSearchBases(),
                    searchStr);
            if (result != null)
                return result.getNameInNamespace();
            return null;
        } catch (NamingException ex) {
            ex.printStackTrace();
            throw new ServiceUnavailableException("Unable to get DN from uid");
        }
    }

    /**
     * Perform LDAP query as superuser.
     *
     * Unfortunately the existing query mechanism is focused on attributes, but
     * we need the first SearchResult itself, so that we can extract its DN.
     *
     * @param searchBases
     *      List of searchbases
     * @param searchFilter
     *      Optional filter.
     * @return Result of query.
     * @throws NamingException if 
     * @throws ServiceUnavailableException if server not available.
     */
    private SearchResult queryFirstAsSuperuser(List<String> searchBases, String searchFilter)
        throws NamingException, ServiceUnavailableException
    {
        SearchResult result = null;
        DirContext ctx = null;
        try {
            ctx = checkoutSuperuserContext();
        } catch (Exception e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
        if( ctx == null ) {
            throw new ServiceUnavailableException("Unable to obtain context");
        }

        for (int trynum = 0; trynum < 2; trynum++) {
            try {
                // We specify CN for search controls so that it returns
                // something but not everything.
                SearchControls ctls = createSimpleSearchControls("cn");
                ctls.setCountLimit(0);
                for( String searchBase : searchBases ){
                    NamingEnumeration<SearchResult> answer = ctx.search(searchBase, searchFilter, ctls);
                    if (answer.hasMoreElements()){
                        result = answer.next();
                    }
                }
                returnSuperuserContext(ctx, false);
                return result;
            } catch (NamingException ex) {
                returnSuperuserContext(ctx, true);
                if (trynum > 0)
                    throw convertToServiceUnavailableException(ex);
            }
        }
        // Not reached, but the compiler doesn't unroll to be able to tell.
        return result;
    }

}
