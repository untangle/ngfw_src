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

import java.util.ArrayList;
import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import com.untangle.uvm.addrbook.AddressBookConfiguration;
import com.untangle.uvm.addrbook.AddressBookSettings;
import com.untangle.uvm.addrbook.NoSuchEmailException;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.addrbook.RepositorySettings;
import com.untangle.uvm.addrbook.RepositoryType;
import com.untangle.uvm.addrbook.UserEntry;
import com.untangle.uvm.license.ProductIdentifier;
import org.apache.log4j.Logger;


/**
 * Concrete implementation of the AddressBook.  Note that this class
 * should only be used by classes in "engine" (the parent package).
 *
 */
class DefaultAddressBookImpl implements RemoteAddressBook {

    private static final ABStatus status = new ABStatus();

    private final Logger m_logger =
        Logger.getLogger(getClass());

    DefaultAddressBookImpl() {
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public AddressBookSettings getAddressBookSettings() {
        m_logger.info("getting invalid settings");
        AddressBookSettings m_settings = new AddressBookSettings();
        m_settings.setAddressBookConfiguration(AddressBookConfiguration.NOT_CONFIGURED);
        m_settings.setADRepositorySettings(new RepositorySettings());
        return m_settings;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void setAddressBookSettings(final AddressBookSettings newSettings) {
        m_logger.info("ignoring save settings");
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean authenticate(String uid, String pwd)
        throws ServiceUnavailableException {
        m_logger.info("ignoring authenticate.");
        return false;
    }

    private static class ABStatus implements RemoteAddressBook.Status {
        public boolean isLocalWorking() { return false; }
        public boolean isADWorking() { return false; }
        public String localDetail() { return "unconfigured"; }
        public String adDetail() { return "unconfigured"; }
    }

    public Status getStatus() {
        m_logger.info("ignoring get status.");
        return this.status;
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException {

        m_logger.info("ignoring authenticate by email.");
        return false;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains email.");
        return RepositoryType.NONE;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains email.");
        return RepositoryType.NONE;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains uid.");
        return RepositoryType.NONE;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains uid.");
        return RepositoryType.NONE;
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains getLocalUserEntries.");
        return new ArrayList<UserEntry>();
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameNotFoundException, NameAlreadyBoundException {
        m_logger.info("ignoring contains setLocalUserEntries.");
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries()
        throws ServiceUnavailableException {
        m_logger.info("ignoring getUserEntries");
        return new ArrayList<UserEntry>();
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getUserEntries");
        return new ArrayList<UserEntry>();
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntry");
        return null;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntry");
        return null;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntryByEmail");
        return null;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntryByEmail");
        return null;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException {
        m_logger.info("ignoring createLocalEntry");
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException {
        m_logger.info("ignoring deleteLocalEntry");
        return false;
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException {
        m_logger.info("ignoring updateLocalEntry");
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException {
        m_logger.info("ignoring updateLocalPassword");
    }

    public String productIdentifier()
    {
        return ProductIdentifier.ADDRESS_BOOK;
    }
}


