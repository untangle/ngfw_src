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

import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import com.untangle.uvm.addrbook.AddressBookSettings;
import com.untangle.uvm.addrbook.NoSuchEmailException;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.addrbook.RepositoryType;
import com.untangle.uvm.addrbook.UserEntry;
import com.untangle.uvm.addrbook.GroupEntry;

/**
 * Concrete implementation of the AddressBook.  Note that this class
 * should only be used by classes in "engine" (the parent package).
 */
class RemoteAddressBookAdaptor implements RemoteAddressBook {

    private final RemoteAddressBook addressBook;

    RemoteAddressBookAdaptor(RemoteAddressBook addressBook) {
        this.addressBook = addressBook;
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public AddressBookSettings getAddressBookSettings() {
        return this.addressBook.getAddressBookSettings();
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void setAddressBookSettings(final AddressBookSettings newSettings) {
        this.addressBook.setAddressBookSettings(newSettings);
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean authenticate(String uid, String pwd)
        throws ServiceUnavailableException {
        return this.addressBook.authenticate(uid,pwd);
    }

    public Status getStatus() {
        return this.addressBook.getStatus();
    }

    public Status getStatusForSettings(AddressBookSettings newSettings) {
        return this.addressBook.getStatusForSettings(newSettings);
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException {
        return this.addressBook.authenticateByEmail(email,pwd);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.containsEmail(address,searchIn);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address)
        throws ServiceUnavailableException {
        return this.addressBook.containsEmail(address);
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid)
        throws ServiceUnavailableException {
        return this.addressBook.containsUid(uid);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.containsUid(uid,searchIn);
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException {
        return this.addressBook.getLocalUserEntries();
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameNotFoundException, NameAlreadyBoundException {
        this.addressBook.setLocalUserEntries(userEntries);
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries()
        throws ServiceUnavailableException {
        return this.addressBook.getUserEntries();
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getUserEntries(searchIn);
    }


    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid)
        throws ServiceUnavailableException {
        return this.addressBook.getEntry(uid);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getEntry(uid,searchIn);
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email)
        throws ServiceUnavailableException {
        return this.addressBook.getEntryByEmail(email);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getEntryByEmail(email,searchIn);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException {
        this.addressBook.createLocalEntry(newEntry,password);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException {
        return this.addressBook.deleteLocalEntry(entryUid);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException {
        this.addressBook.updateLocalEntry(changedEntry);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException {
        this.addressBook.updateLocalPassword(uid,newPassword);
    }

    public String productIdentifier()
    {
        return this.addressBook.productIdentifier();
    }

    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<GroupEntry> getGroupEntries(boolean fetchMembersOf)
        throws ServiceUnavailableException {
        return this.addressBook.getGroupEntries(fetchMembersOf);
    }



    //====================================================
    // See doc on com.untangle.uvm.addrbook.AddressBook
    //====================================================
    public List<GroupEntry> getGroupEntries(RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getGroupEntries(searchIn);
    }
    
    /* (non-Javadoc)
     * @see com.untangle.uvm.addrbook.RemoteAddressBook#getGroupUsers(java.lang.String)
     */
    @Override
    public List<UserEntry> getGroupUsers(String groupName)
    throws ServiceUnavailableException
    {
        return this.addressBook.getGroupUsers(groupName);
    }
    
    @Override
    public boolean isMemberOf( String user, String group )
    {
        return this.addressBook.isMemberOf(user, group);
    }

    @Override
    public void refreshGroupCache() {
        this.addressBook.refreshGroupCache();
    }

}


