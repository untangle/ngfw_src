/*
 * $Id$
 */
package com.untangle.uvm.addrbook;

import java.util.List;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

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
public interface RemoteAddressBook 
{
    public enum Backend { ACTIVE_DIRECTORY, RADIUS };

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
    
    public boolean authenticate( String uid, String password, Backend backend )
    throws ServiceUnavailableException;

    /**
     * Connectivity tester for AD
     *
     * @return a <code>Status</code> value
     */
    Status getStatus();

    Status getStatusForSettings(AddressBookSettings newSettings);

    Status getStatusForSettings(AddressBookSettings newSettings,
                                String username,
                                String password);

    interface Status
    {
        boolean isADWorking();

        boolean isRadiusWorking();
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

    /**
     * Return true iff user is a member of group.
     * @param user The user to test
     * @param group The group to see if users is a member.
     * @return True if the user is a member of the group.
     */
    public boolean isMemberOf(String user, String group);

    /**
     * Returns a list of groups the user is in
     * XXX this is implemented inefficiently
     * @param user 
     * @return The list of groups
     */
    public List<String> memberOf(String user);
    
    /**
     * Refresh the group cache, normally this is done every x minutes.
     */
    public void refreshGroupCache();

}


