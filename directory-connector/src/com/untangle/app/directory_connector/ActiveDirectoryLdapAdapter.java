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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.app.directory_connector.GroupEntry;
import com.untangle.app.directory_connector.ActiveDirectorySettings;
import com.untangle.app.directory_connector.UserEntry;
import com.untangle.uvm.servlet.ServletUtils;

/**
 * Implementation of the Ldap adapter which understands uniqueness of
 * ActiveDirectory.
 */
class ActiveDirectoryLdapAdapter extends LdapAdapter
{
    private final Logger logger = Logger.getLogger(ActiveDirectoryLdapAdapter.class);

    private ActiveDirectorySettings settings;

    private String userAccountControl;
    
    private static final String[] USER_CLASS_TYPE = { "user" };
    private static final String[] GROUP_CLASS_TYPE = { "group" };

    /**
     * Construct a new AD adapter with the given settings
     * 
     * @param settings
     *            the settings
     */
    public ActiveDirectoryLdapAdapter(ActiveDirectorySettings settings)
    {
        this.settings = settings;
    }

    @Override
    protected final ActiveDirectorySettings getSettings()
    {
        return settings;
    }

    @Override
    protected String[] getUserClassType()
    {
        return USER_CLASS_TYPE;
    }

    @Override
    protected String[] getGroupClassType()
    {
        return GROUP_CLASS_TYPE;
    }

    @Override
    protected String getMailAttributeName()
    {
        return "mail";
    }
    
    @Override
    protected String getPrimaryGroupIDAttribute()
    {
        return "primaryGroupID";
    }

    @Override
    protected String getFullNameAttributeName()
    {
        return "cn";
    }

    @Override
    protected String getUIDAttributeName()
    {
        return "sAMAccountName";// Dated, but seems to be what windows uses
    }

    @Override
    public String getSuperuserDN()
    {
        String sustring = settings.getSuperuser();
        if (sustring.toUpperCase().startsWith("CN="))
            return sustring + "," + domainComponents(settings.getDomain());
        if (sustring.contains("@") == true || sustring.contains("\\") == true)
            return sustring;
        return sustring + "@" + settings.getDomain();
    }

    @Override
    public List<String> getSearchBases()
    {
        ArrayList<String> bases = new ArrayList<String>();

        List<String> ouFilters = settings.getOUFilters();

        for( String ouFilter: ouFilters ){
            logger.warn("getSearchBases: ouFilter=" + ouFilter );
            if (ouFilter != null && !("".equals(ouFilter))){
                bases.add(ouFilter + "," + domainComponents(settings.getDomain()));
            }
        }

        if( bases.isEmpty()){
            bases.add(domainComponents(settings.getDomain()));
        }
        return bases;
    }

    @Override
    protected boolean filterGID(int gid)
    {
        return gid < 1000000;
    }

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
     * @param fetchMembersOf Set to true to indicate that the entries should include the list of groups that the group is a member of.
     * @return
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

            List<GroupEntry> ret = new ArrayList<GroupEntry>();

            if(list == null || list.size() == 0) {
                return ret;
            }

            for(Map<String, String[]> map : list) {
                GroupEntry entry = toGroupEntry(map);
                if(entry != null) {
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
     * @param user The username to query.
     * @return A List of all of the groups that a user belongs to.
     */
    public List<GroupEntry> listUserGroups( String user ) throws ServiceUnavailableException
    {
        /* First retrieve the user object */
        UserEntry userEntry = getEntry(user);
        
        if ( userEntry == null ) {
            logger.debug( "Unable to query an user entry for the user: '" + user + "'" );
            return new ArrayList<GroupEntry>();
        }
        
        /* Next retrieve all of the groups that the user belongs to, this doesn't recurse */
        try {
            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 getListGroupsSearchString(userEntry),
                                 getGroupEntrySearchControls(false));

            List<GroupEntry> ret = new ArrayList<GroupEntry>();

            if(list == null || list.size() == 0) {
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
     * @param group Name of the group to query.
     * @return A list of all of the users that belong to a group.
     */
    @Override
    public List<UserEntry> listGroupUsers( String group ) throws ServiceUnavailableException
    {
        GroupEntry groupEntry = getGroupEntry(group,false);
        if ( groupEntry == null ) {
            logger.debug( "The group '" + group + "' doesn't exist.");
            return new ArrayList<UserEntry>();
        }
        
        try {
            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 getListUsersSearchString(groupEntry, getUserClassType()),
                                 getUserEntrySearchControls());

            List<UserEntry> ret = new ArrayList<UserEntry>();

            if(list == null || list.size() == 0) {
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
            int values[] = { 1, 8, 32, 64, 65536 };

            Set<String> valueSet  = new HashSet<String>();
            for ( int c = 0 ; c < Math.pow( 2, values.length + 1 ) - 1 ; c++ ) {
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
     * Returns null if not found.  If more than one found (!),
     * first is returned.
     */
    private GroupEntry getGroupEntryWithSearchString(String searchStr, boolean fetchMembersOf)
        throws ServiceUnavailableException
    {
        try {

            List<Map<String, String[]>> list =
                queryAsSuperuser(getSearchBases(),
                                 searchStr,
                                 getGroupEntrySearchControls(fetchMembersOf));


            if(list == null || list.size() == 0) {
                return null;
            }

            return toGroupEntry(list.get(0));
        }
        catch(NamingException ex) {
            throw new ServiceUnavailableException(ex.toString());
        }
    }

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
     * Get the DN from email address. Returns null if none found.
     */
    private String getDNFromEmail(String email)
            throws ServiceUnavailableException
    {

        try {
            String searchStr = "(&"
                    + orStrings("objectClass=", getUserClassType()) + "(mail="
                    + email + "))";
            SearchResult result = queryFirstAsSuperuser(getSearchBases(),
                    searchStr);
            if (result != null)
                return result.getNameInNamespace();
            return null;
        } catch (NamingException ex) {
            throw new ServiceUnavailableException("Unable to get DN from email");
        }
    }

    /**
     * Get the Distinguished Name from the uid (which we map to
     * "sAMAccountName"). Returns null if none found.
     */
    private String getDNFromUID(String uid) throws ServiceUnavailableException
    {
        try {
            String searchStr = "(&"
                    + orStrings("objectClass=", getUserClassType())
                    + "(sAMAccountName=" + uid + "))";
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

    // Unfortunately the existing query mechanism is focused on attributes, but
    // we need the first SearchResult itself, so that we can extract its DN.
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

    private static class LDAPTester
    {
        private ActiveDirectorySettings testerSettings;
        private ActiveDirectoryLdapAdapter adapter;
        private final JSONSerializer serializer = new JSONSerializer();

        private LDAPTester() throws Exception
        {
            ServletUtils.getInstance().registerSerializers(serializer);
        }

        private ActiveDirectorySettings parseSettings(String fileName)
                throws IOException, UnmarshallException
        {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();

            try {
                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } finally {
                reader.close();
            }
            this.testerSettings = (ActiveDirectorySettings) serializer.fromJSON(sb.toString());
            return this.testerSettings;
        }

        private void buildAdapter()
        {
            this.adapter = new ActiveDirectoryLdapAdapter(this.testerSettings);
        }

        @SuppressWarnings({"unchecked","rawtypes"})
        private Object runCommand(String methodName, String params)
                throws Exception
        {
            Object[] args = new Object[0];

            if (params != null) {
                params = String.format( "{ 'javaClass' : 'java.util.ArrayList', list : [%s]}", params);
                args = ((List<Object>)this.serializer.fromJSON(params)).toArray();
            }

            Class[] argsClass = new Class[args.length];

            for (int c = 0; c < args.length; c++) {
                Class clz = args[c].getClass();
                if ( clz == Boolean.class ) {
                    clz = boolean.class;
                }
                argsClass[c] = clz;
            }

            Method m = this.adapter.getClass().getMethod(methodName, argsClass);
            return m.invoke(this.adapter, args);
        }
        
        @SuppressWarnings("unchecked")
        private void dumpObject( Object o ) throws Exception
        {
            if ( o instanceof Iterable<?> ) {
                Iterable<Object> iterable = (Iterable<Object>)o;
                for ( Object item : iterable ) {
                    System.out.println(this.serializer.toJSON(item));
                }
            } else {
                System.out.println(this.serializer.toJSON(o));
            }
        }
    }
}
