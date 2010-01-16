/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.addrbook;

import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import com.untangle.uvm.license.LicensedProduct;

/**
 * Interface for an "AddressBook", which acts as both
 * an authentication service and a directory.
 *
 * The AddressBook can be configured to use a local
 * repository, as well as ActiveDirectory.  For query methods,
 * the repository can optionaly be specified.
 *
 * Note that the initial state of the AddressBook is
 * {@link com.untangle.uvm.addrbook.AddressBookConfiguration#NOT_CONFIGURED NOT_CONFIGURED}.
 */
public interface RemoteAddressBook extends LicensedProduct
{
    /**
     * Get the AddressBookSettings of this address book.
     */
    AddressBookSettings getAddressBookSettings();

    /**
     * Set the AddressBookSettings of this address book.
     *
     * @exception IllegalArgumentException to cover obvious cases of
     * junk (null settings, settings for "AD" without embedded
     * RepositorySettings object, etc).  If the settings are
     * semanticaly valid but "don't work", this exception is
     * <b>not</b> thrown.
     */
    void setAddressBookSettings(AddressBookSettings conf)
        throws IllegalArgumentException;

    /**
     * Authenticate the user using the provided password.  This method
     * uses all configured repositories.
     *
     * <b>Warning.  It is a big security no-no to tell bad-guys if an
     * account exists or not (it helps them guess stuff).  Therefore,
     * special care should be used in translating the
     * "NameNotFoundException" into a "wrong ID" message in any UI</b>
     *
     * @param uid the userid
     * @param pwd the password
     * @return true if such a userid exists and the password matched.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    boolean authenticate(String uid, /*char[]*/String pwd)
        throws ServiceUnavailableException;


    /**
     * Connectivity tester for AD
     *
     * @return a <code>Status</code> value
     */
    Status getStatus();

    Status getStatusForSettings(AddressBookSettings newSettings);

    interface Status
    {
        boolean isLocalWorking();

        boolean isADWorking();

        String localDetail();

        String adDetail();
    }

    /**
     * Authenticate the user associated with the email address using
     * the provided password.  This method uses all configured
     * repositories.
     *
     * <b>Warning.  It is a big security no-no to tell bad-guys if an
     * account exists or not (it helps them guess stuff).  Therefore,
     * special care should be used in translating the
     * "NoSuchEmailException" into a "wrong email" message in any
     * UI</b>
     *
     * @param email the email address
     * @param pwd the password
     * @return true if a user was found with that email address, and
     * the password matches their credentials.  False if the user was
     * found yet the credentials did not match.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    boolean authenticateByEmail(String email, /*char[]*/String pwd)
        throws ServiceUnavailableException, NoSuchEmailException;

    /**
     * Search the specified repository for the given email address.
     *
     * @param searchIn the repository to search for the given address
     * @return <code>searchIn</code>, or
     * com.untangle.uvm.addrbook.RepositoryType#NONE NONE} if not
     * found.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input.  Note that this is
     * <b>not</b> thrown if the specified repository is not
     * configured.
     */
    RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException;

    /**
     * Search all configured repositories for the an entry with a
     * matching email address
     *
     * @param address the email address
     * @return The repository containing the given address, or
     *         {@link com.untangle.uvm.addrbook.RepositoryType#NONE NONE}
     *         if not found.
     * @exception ServiceUnavailableException the back-end directory
     *            is in a bad state.  There are no corrective actions
     *            the caller can take based on the given input
     */
    RepositoryType containsEmail(String address)
        throws ServiceUnavailableException;

    /**
     * Search all configured repositories for the given userid.
     *
     * @param uid the userid
     * @return The repository containing the given uid, or {@link
     * com.untangle.uvm.addrbook.RepositoryType#NONE NONE} if not
     * found.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    RepositoryType containsUid(String uid)
        throws ServiceUnavailableException;

    /**
     * Search the specified repository for the given userid.
     *
     * @param searchIn the repository to search for the given uid
     * @return <code>searchIn</code>, or
     * com.untangle.uvm.addrbook.RepositoryType#NONE NONE} if not
     * found.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input.  Note that this is
     * <b>not</b> thrown if the specified repository is not
     * configured.
     */
    RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException;

    /**
     * Get all USerEntries from all configured repositories.  Note
     * that a UserEntry is unique in its uid/repository key so there
     * is a chance to see the same "uid" twice in the returned list
     *
     * @return the list of all entries (may be of zero length, but not
     * null).
     *
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    List<UserEntry> getUserEntries()
        throws ServiceUnavailableException;

    /**
     * Set all UserEntries that exist in the local repository.  A
     * convenience method for UI.
     *
     * @param userEntries the list of all local users.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameAlreadyBoundException,
               NameNotFoundException;

    /**
     * Get all UserEntries from the local repository.  A convenience
     * method for UI.
     *
     * @return the list of all local entries (may be of zero length,
     * but not null).
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException;

    /**
     * Get all UserEntries from the given repository.
     *
     * @param searchIn the repository type to search
     * @return the list of all entries (may ne of zero length, but not
     * null).
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input.  Note that this is
     * <b>not</b> thrown if the specified repository is not
     * configured.
     */
    List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException;

    /**
     * Get a UserEntry by userid address from all configured
     * repositories
     *
     * @param uid the user id
     * @return the entry, <b>or null if not found</b>
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    UserEntry getEntry(String uid)
        throws ServiceUnavailableException;

    /**
     * Get a UserEntry by id from the given repository.
     *
     * @param uid the user id
     * @param searchIn the type of repository to search-in.
     * @return the entry, <b>or null if not found</b>
     *
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input.  Note that this is
     * <b>not</b> thrown if the specified repository is not
     * configured.
     */
    UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException;

    /**
     * Get a UserEntry by email address.  This searches all configured
     * repositories.
     *
     * @param email the email address
     * @return the entry, <b>or null if not found</b>
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    UserEntry getEntryByEmail(String email) throws ServiceUnavailableException;

    /**
     * Get a UserEntry by email address in the given repository.
     *
     * @param email the email address
     * @param searchIn the type of repository to search-in.
     * @return the entry, <b>or null if not found</b>
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input.  Note that this is
     * <b>not</b> thrown if the specified repository is not
     * configured.
     */
    UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException;

    /**
     * Create a new local user.  Note that the {@link
     * #com.untangle.uvm.addrbook.UserEntry#getStoredIn getStoredIn}
     * property is ignored.
     *
     * @param newEntry the new entry
     * @param password the password for the new user
     * @exception ServiceUnavailableException the back-end directory
     *            is in a bad state.  There are no corrective actions
     *            the caller can take based on the given input
     * @exception NameAlreadyBoundException if a user with that uniqueID
     *            already exists
     */
    void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException;

    /**
     * Delete an entry from the local directory.
     *
     * @param entryUid the userid of the entry
     * @return true if deleted, false if the id was not found
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException;

    /**
     * Update an existing local entry (using the {@link
     * com.untangle.uvm.addrbook.UserEntry#getUID uid}) of the
     * passed-in entry.  Note that the {@link
     * com.untangle.uvm.addrbook.UserEntry#getStoredIn storedIn}
     * property is ignored, and implicitly {@link
     * com.untangle.uvm.addrbook.RepositoryType#LOCAL_DIRECTORY
     * LOCAL_DIRECTORY} as well as uid.
     *
     * @param changedEntry the entry to change all attributes except
     * uid and repository type.
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     * @exception NameNotFoundException there is no such userid registered
     */
    void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException;

    /**
     * Change a local user's password.
     *
     * A convienence method, which does not require a re-query of
     * the email address to call {@link #updateEntry updateEntry}
     *
     * @param uid the userid
     * @param newPassword the new password
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     * @exception NameNotFoundException there is no such userid registered
     */
    void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException;

    /**
     * Get all GroupEntries from all configured repositories.  Note
     * that a GroupEntry is unique in its uid/repository key so there
     * is a chance to see the same "uid" twice in the returned list
     * 
     * @param fetchMemberOf Set to true to load the list of groups that each group is in.
     *
     * @return the list of all entries (may be of zero length, but not
     * null).
     *
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input
     */
    List<GroupEntry> getGroupEntries(boolean fetchMemberOf)
        throws ServiceUnavailableException;

    /**
     * Get all GroupEntries from the given repository.
     *
     * @param searchIn the repository type to search
     * @return the list of all entries (may ne of zero length, but not
     * null).
     * @exception ServiceUnavailableException the back-end directory
     * is in a bad state.  There are no corrective actions the caller
     * can take based on the given input.  Note that this is
     * <b>not</b> thrown if the specified repository is not
     * configured.
     */
    List<GroupEntry> getGroupEntries(RepositoryType searchIn)
        throws ServiceUnavailableException;

    /**
     * Get all of the users that are in a group.
     * @param groupName The name of the group to fetch.
     * @return
     */
    public List<UserEntry> getGroupUsers(String groupName) throws ServiceUnavailableException;

    
    public boolean isMemberOf(String user, String group);
}


