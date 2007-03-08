/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine.addrbook;

import java.util.List;
import java.util.Map;
import javax.naming.AuthenticationException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.DirContext;

import com.untangle.mvvm.addrbook.NoSuchEmailException;
import com.untangle.mvvm.addrbook.RepositorySettings;
import com.untangle.mvvm.addrbook.RepositoryType;
import com.untangle.mvvm.addrbook.UserEntry;
import org.apache.log4j.Logger;


/**
 * Implementation of the Ldap adapter which understands uniqueness
 * of ActiveDirectory.
 */
class ActiveDirectoryLdapAdapter extends LdapAdapter {

    private final Logger m_logger =
        Logger.getLogger(ActiveDirectoryLdapAdapter.class);

    private RepositorySettings m_settings;

    /**
     * Construct a new AD adapter with the given settings
     *
     * @param settings the settings
     */
    public ActiveDirectoryLdapAdapter(RepositorySettings settings) {
        m_settings = settings;
    }

    @Override
    protected final RepositorySettings getSettings() {
        return m_settings;
    }

    @Override
    protected final RepositoryType getRepositoryType() {
        return RepositoryType.MS_ACTIVE_DIRECTORY;
    }

    @Override
    protected String getUserClassType() {
        return "user";
    }

    @Override
    protected String getMailAttributeName() {
        return "mail";
    }

    @Override
    protected String getFullNameAttributeName() {
        return "cn";
    }

    @Override
    protected String getUIDAttributeName() {
        return "sAMAccountName";//Dated, but seems to be what windows uses
    }

    @Override
    public String getSuperuserDN() {
        String sustring = m_settings.getSuperuser();
        if (sustring.toUpperCase().startsWith("CN="))
            return sustring + "," + domainComponents(m_settings.getDomain());
        return "cn=" + sustring + "," + getSearchBase();
    }

    @Override
    public String getSearchBase() {
        String userLoc = "cn=users";
        String ouFilter = m_settings.getOUFilter();
        if (ouFilter != null && !("".equals(ouFilter)))
            userLoc = ouFilter;
            
        return userLoc + "," + domainComponents(m_settings.getDomain());
    }



    @Override
    public boolean authenticate(String uid, String pwd)
        throws ServiceUnavailableException {


        //First, we need the "CN" for the user from uid.  Not sure why
        //microsoft did things this way???
        String cn = getCNFromUID(uid);
        if(cn == null) {
            return false;//throw new AuthenticationException("Unable to get CN from sAMAccountName");
        }

        try {
            String dn = "cn=" + cn + "," + getSearchBase();
            DirContext ctx = createContext(
                                           getSettings().getLDAPHost(),
                                           getSettings().getLDAPPort(),
                                           dn,
                                           pwd);
            boolean ret = ctx != null;
            closeContext(ctx);
            return ret;
        }
        catch(AuthenticationException ex) {
            return false;
        }
        catch(NamingException ex) {
            m_logger.warn("Exception authenticating user \"" + uid + "\"", ex);
            throw new ServiceUnavailableException(ex.toString());
        }
    }

    @Override
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException {

        String cn = getCNFromEmail(email);
        if(cn == null) {
            throw new NoSuchEmailException(email);
        }

        try {

            String dn = "cn=" + cn + "," + getSearchBase();
            DirContext ctx = createContext(
                                           getSettings().getLDAPHost(),
                                           getSettings().getLDAPPort(),
                                           dn,
                                           pwd);
            boolean ret = ctx != null;
            closeContext(ctx);
            return ret;
        }
        catch(AuthenticationException ex) {
            return false;
        }
        catch(NamingException ex) {
            m_logger.warn("Exception authenticating user by email \"" + email + "\"", ex);
            throw new ServiceUnavailableException(ex.toString());
        }
    }

    @Override
    protected String getListAllUsersSearchString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(&");
        sb.append("(objectClass=").append(getUserClassType()).append(")");
        sb.append("(|(userAccountControl=66048)(userAccountControl=512))");
        sb.append(")");
        return sb.toString();
    }


    //TODO Some way to filter-out the system users from AD?!?

    /*
      @Override
      protected String getListAllUsersSearchString() {

      if(System.currentTimeMillis() > 0) {
      return "(&(objectClass=user)(!cn=Builtin))";
      }

      StringBuilder sb = new StringBuilder();
      sb.append("(&");
      sb.append("(").append("objectClass=").append(getSettings().getUserClass()).append(")");
      sb.append("(").append("cn=Builtin").append(")");
      sb.append(")");
      System.out.println("*************" + sb.toString());
      return sb.toString();
      }
    */

    /**
     * Get the CN from email address.  Returns null
     * if none found.
     */
    private String getCNFromEmail(String email)
        throws ServiceUnavailableException {

        try {
            String searchStr = "(&(objectClass=" + getUserClassType() + ")(mail=" + email + "))";
            List<Map<String, String[]>> result = queryAsSuperuser(
                                                                  getSearchBase(),
                                                                  searchStr,
                                                                  createSimpleSearchControls("cn"));
            if(result != null && result.size() > 0) {
                return getFirstEntryOrNull(result.get(0).get("cn"));
            }
            return null;
        }
        catch(NamingException ex) {
            throw new ServiceUnavailableException("Unable to get CN from email");
        }
    }

    /**
     * Get the CN attribute from the uid (which we map to "sAMAccountName").
     * Returns null if none found.
     */
    private String getCNFromUID(String uid)
        throws ServiceUnavailableException {

        try {
            String searchStr = "(&(objectClass=" + getUserClassType() + ")(sAMAccountName=" + uid + "))";
            List<Map<String, String[]>> result = queryAsSuperuser(
                                                                  getSearchBase(),
                                                                  searchStr,
                                                                  createSimpleSearchControls("cn"));
            if(result != null && result.size() > 0) {
                return getFirstEntryOrNull(result.get(0).get("cn"));
            }
            return null;
        }
        catch(NamingException ex) {
            throw new ServiceUnavailableException("Unable to get CN from uid");
        }
    }







    //=============================
    // Test Code
    //=============================


    public static void main(String[] args) throws Exception {

        RepositorySettings settings = new RepositorySettings(
                                                             "Bill Test1",
                                                             "ABC123xyz",
                                                             "windows.metavize.com",
                                                             "mrslave",
                                                             389);

        ActiveDirectoryLdapAdapter adapter = new ActiveDirectoryLdapAdapter(settings);

        System.out.println("==================================");
        System.out.println("Do test");
        adapter.test();
        System.out.println("Test OK");


        System.out.println("===================================");
        System.out.println("Check if user exists by ID");
        String goodUid = "billtest1";
        System.out.println("User \"" + goodUid + "\" exists? " + adapter.containsUser(goodUid));

        System.out.println("===================================");
        System.out.println("Check if user doesn't exists by ID");
        String badUid = "imnothere";
        System.out.println("User \"" + badUid + "\" exists? " + adapter.containsUser(badUid));


        System.out.println("===================================");
        System.out.println("Get full entry for good user");
        UserEntry entry = adapter.getEntry(goodUid);
        System.out.println("Returned: ");
        System.out.println(entry);

        System.out.println("===================================");
        System.out.println("Authenticate (good)");
        System.out.println(adapter.authenticate("cng", "Sneaker11"));

        System.out.println("===================================");
        System.out.println("Authenticate (bad)");
        System.out.println(adapter.authenticate("cng", "xsneaker11"));

        System.out.println("===================================");
        System.out.println("Authenticate by email (good)");
        System.out.println(adapter.authenticateByEmail("cng@windows.metavize.com", "Sneaker11"));

        System.out.println("===================================");
        System.out.println("Authenticate by email (bad)");
        System.out.println(adapter.authenticateByEmail("cng@windows.metavize.com", "Sxneaker11"));

        System.out.println("===================================");
        System.out.println("Authenticate by email (bad email)");
        try {
            System.out.println(adapter.authenticateByEmail("xcng@windows.metavize.com", "Sneaker11"));
        }
        catch(NoSuchEmailException expected) {
            System.out.println("Got exception (as expected)");
        }



        System.out.println("===================================");
        System.out.println("Get full entry for bad user");
        entry = adapter.getEntry(badUid);
        System.out.println("Returned: ");
        System.out.println(entry);


        System.out.println("===================================");
        System.out.println("Get entry by email");
        entry = adapter.getEntryByEmail("billtest1@windows.metavize.com");
        System.out.println("Returned: ");
        System.out.println(entry);

        System.out.println("===================================");
        System.out.println("List All");
        List<UserEntry> list = adapter.listAll();
        for(UserEntry ue : list) {
            System.out.println("----------------");
            System.out.println(ue);
        }

    }
}


