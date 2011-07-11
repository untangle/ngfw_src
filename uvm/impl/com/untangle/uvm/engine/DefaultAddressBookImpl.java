/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.addrbook.AddressBookConfiguration;
import com.untangle.uvm.addrbook.AddressBookSettings;
import com.untangle.uvm.addrbook.GroupEntry;
import com.untangle.uvm.addrbook.NoSuchEmailException;
import com.untangle.uvm.addrbook.RadiusServerSettings;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.addrbook.RepositorySettings;
import com.untangle.uvm.addrbook.RepositoryType;
import com.untangle.uvm.addrbook.UserEntry;
import com.untangle.uvm.addrbook.RadiusServerSettings.AuthenticationMethod;
import com.untangle.uvm.license.License;
import com.untangle.uvm.util.TransactionWork;


/**
 * Concrete implementation of the AddressBook.  Note that this class
 * should only be used by classes in "engine" (the parent package).
 *
 */
class DefaultAddressBookImpl implements RemoteAddressBook
{

    private static final ABStatus STATUS_NOT_WORKING = new ABStatus( false, "unconfigured" );
    
    private AddressBookSettings settings;
    private LocalLdapAdapter localAdapter;

    private final Logger logger = Logger.getLogger(getClass());

    public DefaultAddressBookImpl()
    {
        TransactionWork<AddressBookSettings> work = new TransactionWork<AddressBookSettings>() {
            private AddressBookSettings settings;
            
            public boolean doWork(org.hibernate.Session s) {
                Query q = s.createQuery("from AddressBookSettings");
                settings = (AddressBookSettings)q.uniqueResult();

                if(settings == null) {
                    logger.info("creating new AddressBookSettings");
                    settings = new AddressBookSettings();
                    settings.setAddressBookConfiguration(AddressBookConfiguration.LOCAL_ONLY);
                    settings.setADRepositorySettings(new RepositorySettings("Administrator",
                                                                            "mypassword",
                                                                            "mydomain.int",
                                                                            "ad_server.mydomain.int",
                                                                            389));
                    settings.setRadiusServerSettings(new RadiusServerSettings(false,
                            "1.2.3.4",
                            1812,
                            "mysharedsecret",
                            AuthenticationMethod.PAP));
                    s.save(settings);
                } else if ( settings.getRadiusServerSettings() == null ) {                   
                    settings.setRadiusServerSettings(new RadiusServerSettings(false,
                            "1.2.3.4",
                            1812,
                            "mysharedsecret",
                            AuthenticationMethod.PAP));
                    
                    settings = (AddressBookSettings)s.merge( settings );
                }
                return true;
            }

            public AddressBookSettings getResult() { return settings; }
        };
        
        LocalUvmContextFactory.context().runTransaction(work);

        settings =  work.getResult();

        //We create the local adapter regardless
        localAdapter = new LocalLdapAdapter();
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public AddressBookSettings getAddressBookSettings()
    {
        return settings;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void setAddressBookSettings(final AddressBookSettings newSettings)
    {
        settings = newSettings;

        //Hibernate stuff
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean authenticate(String uid, String pwd) throws ServiceUnavailableException
    {
        logger.info("authenticating against local directory.");

        return isNotConfigured() ? false : localAdapter.authenticate(uid, pwd);
    }
    
    public boolean authenticate( String uid, String password, Backend backend )
    throws ServiceUnavailableException
    {
        switch ( backend ) {
        case ACTIVE_DIRECTORY:
                return false;
                
        case LOCAL_DIRECTORY:
            return isNotConfigured() ? false : localAdapter.authenticate(uid, password);
            
        case RADIUS:
            return false;
        }
        
        return false;
    }


    @SuppressWarnings("serial")
    public static class ABStatus implements RemoteAddressBook.Status, Serializable
    {
        private final boolean isLocalWorking;

        private ABStatus(boolean isLocalWorking, String localDetail)
        {
            this.isLocalWorking = isLocalWorking;
        }

        public boolean isLocalWorking() { return this.isLocalWorking; }
        public boolean isADWorking() { return false; }
        public boolean isRadiusWorking() { return false; }
    }

    public Status getStatus()
    {
        if (settings.getAddressBookConfiguration() == AddressBookConfiguration.NOT_CONFIGURED) {
            return STATUS_NOT_WORKING;
        }
        
        try {
            List<UserEntry> localRet = localAdapter.listAll();
            return new ABStatus(true, "working, " + localRet.size() + " users found");
        } catch (Exception x) {
            return new ABStatus(false, x.getMessage());
        }
    }

    public Status getStatusForSettings(AddressBookSettings newSettings)
    {
        return this.getStatus();
    }

    public Status getStatusForSettings(AddressBookSettings newSettings, String username, String password)
    {
        return this.getStatus();
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException
    {

        logger.info("authenticate against the local repository.");
        return isNotConfigured() ? false : localAdapter.authenticateByEmail(email, pwd);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================

    @SuppressWarnings("fallthrough")
    public RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException
    {
        logger.info("containsEmail in default address book.");

        switch(searchIn) {
            //---------------------------------------
        case LOCAL_DIRECTORY:
            if (!isNotConfigured())
                return localAdapter.getEntryByEmail(address) == null ?
                    RepositoryType.NONE : RepositoryType.LOCAL_DIRECTORY;

        case MS_ACTIVE_DIRECTORY:
            // fallthrough
            //---------------------------------------
        case NONE:
        default:
            break;
        }

        return RepositoryType.NONE;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address)
        throws ServiceUnavailableException
    {
        logger.info("containsEmail <" + address + ">.");

        return isNotConfigured()?
                RepositoryType.NONE:
                localAdapter.getEntryByEmail(address) == null?
                RepositoryType.NONE:RepositoryType.LOCAL_DIRECTORY;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException
    {
        logger.info("containsUid <" + uid + ">.");

        switch(searchIn) {
            //---------------------------------------
        case LOCAL_DIRECTORY:
            return isNotConfigured()?
                RepositoryType.NONE:
                localAdapter.containsUser(uid)?
                RepositoryType.LOCAL_DIRECTORY:RepositoryType.NONE;

        case MS_ACTIVE_DIRECTORY:
            // fallthrough
            //---------------------------------------
        case NONE:
        default:
            return RepositoryType.NONE;
        }
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid)
        throws ServiceUnavailableException
    {
        logger.info("containsUid <" + uid + ">.");

        return isNotConfigured()?
            RepositoryType.NONE:
            localAdapter.containsUser(uid)?
            RepositoryType.LOCAL_DIRECTORY:RepositoryType.NONE;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException
    {
        logger.info("getLocalUserEntries local.");

        if(isNotConfigured()) {
            return new ArrayList<UserEntry>();
        } else {
            return localAdapter.listAll();
        }
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameNotFoundException, NameAlreadyBoundException
    {
        // compute the add/delete/keep lists
        HashMap<UserEntry,UserEntry> currentEntries = new HashMap<UserEntry,UserEntry>();
        for( UserEntry userEntry : getLocalUserEntries() )
            currentEntries.put(userEntry,userEntry);
        List<UserEntry> keepList = new ArrayList<UserEntry>();
        List<UserEntry> addList = new ArrayList<UserEntry>();
        for( UserEntry userEntry : userEntries ){
            UserEntry foundEntry = currentEntries.remove(userEntry);
            if( foundEntry != null )
                keepList.add(userEntry);
            else
                addList.add(userEntry);
        }
        // perform the add/removes
        for( UserEntry userEntry : keepList ){
            updateLocalEntry(userEntry);
            if (!UserEntry.UNCHANGED_PASSWORD.equals(userEntry.getPassword()))
                updateLocalPassword(userEntry.getUID(), userEntry.getPassword());
        }
        for( UserEntry userEntry : currentEntries.keySet())
            deleteLocalEntry(userEntry.getUID());
        for( UserEntry userEntry : addList ){
            String password = userEntry.getPassword();
            userEntry.setPassword(null);
            createLocalEntry(userEntry,password);
        }
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries()
        throws ServiceUnavailableException
    {
        logger.info("getUserEntries");
        
        return isNotConfigured() ? new ArrayList<UserEntry>() : localAdapter.listAll();
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException
    {
        logger.info("getUserEntries <" + searchIn + ">");

        switch(searchIn) {
            //---------------------------------------
        case LOCAL_DIRECTORY:
            return isNotConfigured()?
                new ArrayList<UserEntry>():
                localAdapter.listAll();

            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            // fallthrough
            //---------------------------------------
        case NONE:
        default:
            break;
        }

        return new ArrayList<UserEntry>();
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid)
        throws ServiceUnavailableException
    {
        logger.info("getEntry <" + uid + ">");

        return isNotConfigured()?
            null : localAdapter.getEntry(uid);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    @SuppressWarnings("fallthrough")
    public UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException
    {
        logger.info("getEntry <" + uid + ">");

        switch(searchIn) {
            //---------------------------------------
        case LOCAL_DIRECTORY:
            if(!isNotConfigured()) {
                return localAdapter.getEntry(uid);
            }
            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            // fallthrough
            //---------------------------------------
        case NONE:
        default:
            break;
        }
        return null;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email)
        throws ServiceUnavailableException
    {
        logger.info("getEntryByEmail <" + email + ">" );

        if(!isNotConfigured()) {
            return localAdapter.getEntryByEmail(email);
        }
        return null;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException
    {
        logger.info("getEntryByEmail <" + email + "," + searchIn + ">" );

        switch(searchIn) {
            //---------------------------------------
        case LOCAL_DIRECTORY:
            if(!isNotConfigured()) {
                return localAdapter.getEntryByEmail(email);
            }
            break;

            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            //---------------------------------------
        case NONE:
        default:
            break;
        }
        return null;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException
    {
        localAdapter.createUserEntry(newEntry, password);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException
    {
        return localAdapter.deleteUserEntry(entryUid);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException
    {
        localAdapter.modifyUserEntry(changedEntry, null);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException
    {
        localAdapter.changePassword(uid, newPassword);
    }

    private boolean isNotConfigured()
    {
        if ( settings == null ) return false;
        
        return settings.getAddressBookConfiguration() == AddressBookConfiguration.NOT_CONFIGURED;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<GroupEntry> getGroupEntries(boolean fetchMemberOf)
        throws ServiceUnavailableException
    {
        logger.debug("getGroupEntries");
        
        return isNotConfigured() ? new ArrayList<GroupEntry>(): localAdapter.listAllGroups(fetchMemberOf);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<GroupEntry> getGroupEntries(RepositoryType searchIn)
        throws ServiceUnavailableException
    {
        logger.debug("getGroupEntries <" + searchIn + ">");

        switch(searchIn) {
            //---------------------------------------
        case LOCAL_DIRECTORY:
            return isNotConfigured()?
                new ArrayList<GroupEntry>():
                localAdapter.listAllGroups(false);

            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            // fallthrough
            //---------------------------------------
        case NONE:
        default:
            break;
        }

        return new ArrayList<GroupEntry>();
    }
    
    /* (non-Javadoc)
     * @see com.untangle.uvm.addrbook.RemoteAddressBook#getGroupUsers(java.lang.String)
     */
    @Override
    public List<UserEntry> getGroupUsers(String groupName)
    throws ServiceUnavailableException
    {
        return localAdapter.listGroupUsers(groupName);
    }
    
    @Override
    public boolean isMemberOf( String user, String group )
    {
        return false;
    }

    @Override
    public List<String> memberOf(String user)
    {
        return new LinkedList<String>();
    }
    
    @Override
    public void refreshGroupCache() {
    }

}
