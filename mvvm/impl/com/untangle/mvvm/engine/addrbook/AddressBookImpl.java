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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.addrbook.*;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;


/**
 * Concrete implementation of the AddressBook.  Note that this class
 * should only be used by classes in "engine" (the parent package).
 *
 */
public class AddressBookImpl implements AddressBook {

    private static AddressBookImpl s_instance;

    private final Logger m_logger =
        Logger.getLogger(AddressBookImpl.class);
    private AddressBookSettings m_settings;

    private ActiveDirectoryLdapAdapter m_adAdapter;
    private LocalLdapAdapter m_localAdapter;


    public AddressBookImpl() {

        TransactionWork<AddressBookSettings> work = new TransactionWork<AddressBookSettings>() {

            private AddressBookSettings settings;

            public boolean doWork(org.hibernate.Session s) {
                Query q = s.createQuery("from AddressBookSettings");
                settings = (AddressBookSettings)q.uniqueResult();

                if(settings == null) {
                    m_logger.info("creating new AddressBookSettings");
                    settings = new AddressBookSettings();
                    settings.setAddressBookConfiguration(AddressBookConfiguration.LOCAL_ONLY);
                    settings.setADRepositorySettings(new RepositorySettings("Administrator",
                                                                            "mypassword",
                                                                            "mydomain",
                                                                            "ad_server",
                                                                            389
                                                                            ));
                    s.save(settings);
                }
                return true;
            }

            public AddressBookSettings getResult() { return settings; }
        };

        MvvmContextFactory.context().runTransaction(work);

        m_settings =  work.getResult();

        //We create the local adapter regardless
        m_localAdapter = new LocalLdapAdapter();

        //Create the AD adapter conditionaly
        if(m_settings.getAddressBookConfiguration() == AddressBookConfiguration.AD_AND_LOCAL) {
            m_adAdapter = new ActiveDirectoryLdapAdapter(m_settings.getADRepositorySettings());
        }

    }

    /**
     * Method to obtain the singleton AppServerManager
     */
    public static synchronized AddressBookImpl getInstance() {
        if(s_instance == null) {
            s_instance = new AddressBookImpl();
        }
        return s_instance;
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public AddressBookSettings getAddressBookSettings() {
        return m_settings;
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void setAddressBookSettings(final AddressBookSettings newSettings) {

        //Change our state to suit the new settings
        if(
           (newSettings.getAddressBookConfiguration() == AddressBookConfiguration.NOT_CONFIGURED) ||
           (newSettings.getAddressBookConfiguration() == AddressBookConfiguration.LOCAL_ONLY)) {

            ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;
            if(adAdapter != null) {
                adAdapter.close();
            }
            m_adAdapter = null;
        }
        else {//AD_AND_LOCAL
            if(newSettings.getADRepositorySettings() == null) {
                //TODO more validation
                throw new IllegalArgumentException("Must provide settings for ActiveDirectory");
            }
            ActiveDirectoryLdapAdapter oldADAdapter = m_adAdapter;
            ActiveDirectoryLdapAdapter newADAdapter =
                new ActiveDirectoryLdapAdapter(newSettings.getADRepositorySettings());
            m_adAdapter = newADAdapter;
            if(oldADAdapter != null) {
                oldADAdapter.close();
            }
        }

        m_settings = newSettings;

        //Hibernate stuff
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(m_settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);

    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean authenticate(String uid, String pwd)
        throws ServiceUnavailableException {
        if (uid == null || uid.equals("") || pwd == null || pwd.equals(""))
            // No blank uids or passwords.
            return false;

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            if(adAdapter.authenticate(uid, pwd)) {
                return true;
            }
            else {
                //Need to differentiate between "false" meaning there
                //is no such account and "false" meaning "wrong password"
                if(containsUid(uid, RepositoryType.MS_ACTIVE_DIRECTORY) ==
                   RepositoryType.MS_ACTIVE_DIRECTORY) {
                    return false;
                }
            }
        }
        return isNotConfigured()?
            false:m_localAdapter.authenticate(uid, pwd);
    }

    private class ABStatus implements AddressBook.Status {
        boolean isLocalWorking;
        boolean isADWorking;
        String localDetail;
        String adDetail;
        public boolean isLocalWorking() { return isLocalWorking; }
        public boolean isADWorking() { return isADWorking; }
        public String localDetail() { return localDetail; }
        public String adDetail() { return adDetail; }
    }
        
    public Status getStatus() {
        ABStatus s = new ABStatus();
        AddressBookConfiguration conf = m_settings.getAddressBookConfiguration();
        if (conf == AddressBookConfiguration.NOT_CONFIGURED) {
            s.isLocalWorking = false;
            s.isADWorking = false;
            s.localDetail = "unconfigured";
            s.adDetail = "unconfigured";
        } else {
            if (conf == AddressBookConfiguration.LOCAL_ONLY) {
                s.isADWorking = false;
                s.adDetail = "unconfigured";
            } else {
                try {
                    List<UserEntry> adRet = m_adAdapter.listAll();
                    s.isADWorking = true;
                    s.adDetail = "working, " + adRet.size() + " users found";
                } catch (Exception x) {
                    s.isADWorking = false;
                    s.adDetail = x.getMessage();
                }
            }

            try {
                List<UserEntry> localRet = m_localAdapter.listAll();
                s.isLocalWorking = true;
                s.localDetail = "working, " + localRet.size() + " users found";
            } catch (Exception x) {
                s.isLocalWorking = false;
                s.localDetail = x.getMessage();
            }
        }
        return s;
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException {

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            if(adAdapter.authenticateByEmail(email, pwd)) {
                return true;
            }
            else {
                //Need to differentiate between "false" meaning there
                //is no such account and "false" meaning "wrong password"
                if(containsEmail(email, RepositoryType.MS_ACTIVE_DIRECTORY) ==
                   RepositoryType.MS_ACTIVE_DIRECTORY) {
                    return false;
                }
            }
        }
        return isNotConfigured()?
            false:m_localAdapter.authenticateByEmail(email, pwd);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException {

        switch(searchIn) {
            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;
            if(adAdapter != null) {
                return adAdapter.getEntryByEmail(address) == null?
                    RepositoryType.NONE:RepositoryType.MS_ACTIVE_DIRECTORY;
            }
            return RepositoryType.NONE;

            //---------------------------------------
        case LOCAL_DIRECTORY:
            return isNotConfigured()?
                RepositoryType.NONE:
                m_localAdapter.getEntryByEmail(address) == null?
                RepositoryType.NONE:RepositoryType.LOCAL_DIRECTORY;

            //---------------------------------------
        case NONE:
        default:
            return RepositoryType.NONE;
        }
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address)
        throws ServiceUnavailableException {

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            if(adAdapter.getEntryByEmail(address) != null) {
                return RepositoryType.MS_ACTIVE_DIRECTORY;
            }
        }
        return isNotConfigured()?
            RepositoryType.NONE:
            m_localAdapter.getEntryByEmail(address) == null?
            RepositoryType.NONE:RepositoryType.LOCAL_DIRECTORY;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid)
        throws ServiceUnavailableException {

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            if(adAdapter.containsUser(uid)) {
                return RepositoryType.MS_ACTIVE_DIRECTORY;
            }
        }
        return isNotConfigured()?
            RepositoryType.NONE:
            m_localAdapter.containsUser(uid)?
            RepositoryType.LOCAL_DIRECTORY:RepositoryType.NONE;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {

        switch(searchIn) {
            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;
            if(adAdapter != null) {
                return adAdapter.containsUser(uid)?
                    RepositoryType.MS_ACTIVE_DIRECTORY:RepositoryType.NONE;
            }
            return RepositoryType.NONE;

            //---------------------------------------
        case LOCAL_DIRECTORY:
            return isNotConfigured()?
                RepositoryType.NONE:
                m_localAdapter.containsUser(uid)?
                RepositoryType.LOCAL_DIRECTORY:RepositoryType.NONE;

            //---------------------------------------
        case NONE:
        default:
            return RepositoryType.NONE;
        }
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException {
        if(isNotConfigured()) {
            return new ArrayList<UserEntry>();
        }
        else
            return m_localAdapter.listAll();
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameNotFoundException, NameAlreadyBoundException {
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
        for( UserEntry userEntry : currentEntries.keySet() )
            deleteLocalEntry(userEntry.getUID());
        for( UserEntry userEntry : addList ){
            String password = userEntry.getPassword();
            userEntry.setPassword(null);
            createLocalEntry(userEntry,password);
        }
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries()
        throws ServiceUnavailableException {

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            List<UserEntry> adRet = adAdapter.listAll();
            List<UserEntry> localRet = m_localAdapter.listAll();
            adRet.addAll(localRet);
            return adRet;
        }
        return isNotConfigured()?
            new ArrayList<UserEntry>():
            m_localAdapter.listAll();
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException {

        switch(searchIn) {
            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;
            if(adAdapter != null) {
                return adAdapter.listAll();
            }
            break;
            //---------------------------------------
        case LOCAL_DIRECTORY:
            if(!isNotConfigured()) {//Double negative. Bad english
                return m_localAdapter.listAll();
            }

            //---------------------------------------
        case NONE:
        default:
            break;
        }
        return new ArrayList<UserEntry>();
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid)
        throws ServiceUnavailableException {

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            UserEntry ret = adAdapter.getEntry(uid);
            if(ret != null) {
                return ret;
            }
        }
        return isNotConfigured()?
            null:
        m_localAdapter.getEntry(uid);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {

        switch(searchIn) {
            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;
            if(adAdapter != null) {
                return adAdapter.getEntry(uid);
            }
            break;
            //---------------------------------------
        case LOCAL_DIRECTORY:
            if(!isNotConfigured()) {
                return m_localAdapter.getEntry(uid);
            }

            //---------------------------------------
        case NONE:
        default:
            break;
        }
        return null;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email)
        throws ServiceUnavailableException {

        //Stash reference method-local, in case config
        //changes and this "goes null".
        ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;

        if(adAdapter != null) {
            UserEntry ret = adAdapter.getEntryByEmail(email);
            if(ret != null) {
                return ret;
            }
        }
        return isNotConfigured()?
            null:
        m_localAdapter.getEntryByEmail(email);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException {

        switch(searchIn) {
            //---------------------------------------
        case MS_ACTIVE_DIRECTORY:
            ActiveDirectoryLdapAdapter adAdapter = m_adAdapter;
            if(adAdapter != null) {
                return adAdapter.getEntryByEmail(email);
            }
            break;
            //---------------------------------------
        case LOCAL_DIRECTORY:
            if(!isNotConfigured()) {
                return m_localAdapter.getEntryByEmail(email);
            }

            //---------------------------------------
        case NONE:
        default:
            break;
        }
        return null;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException {
        m_localAdapter.createUserEntry(newEntry, password);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException {
        return m_localAdapter.deleteUserEntry(entryUid);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException {
        m_localAdapter.modifyUserEntry(changedEntry, null);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException {
        m_localAdapter.changePassword(uid, newPassword);
    }



    private boolean isNotConfigured() {
        return m_settings.getAddressBookConfiguration() ==
            AddressBookConfiguration.NOT_CONFIGURED;
    }

}


