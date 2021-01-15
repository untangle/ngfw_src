/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Map;
/**
 * The Local Directory API provides functions for managing and authenticating
 * "local users." This is useful for some apps with lists of users (like Captive
 * Portal)
 */
public interface LocalDirectory
{
    /**
     * Authenticate a user
     *
     * @returns true if valid uid/password, false otherwise
     */
    public boolean authenticate(String username, String password);

   /**
     * Authenticate a user who is using 2FA
     *
     * @returns true if valid uid/password/OTP code, false otherwise
     */
    public boolean authenticate(String username, String password, long code);

    /**
     * Return a list of users
     *
     * @returns the current list (never null)
     */
    public LinkedList<LocalDirectoryUser> getUsers();

    /**
     * Save a new list of users
     */
    public void setUsers(LinkedList<LocalDirectoryUser> users);

    /**
     * Adds a new user
     */
    public void addUser(LocalDirectoryUser user);

    /**
     * Checks if the given user exists
     *
     * @returns true if the user exists
     */
    public boolean userExists(LocalDirectoryUser user);

    /**
     * Checks if the given user has expired
     *
     * @param user
     * @return true if the user expired
     */
    public boolean userExpired(LocalDirectoryUser user);

    /**
     * Deletes the expired users from the user list
     */
    public void cleanupExpiredUsers();

    /**
     * Gets the freeRadius log file
     */
    public String getRadiusLogFile();

    /**
     * Gets the Active Directory account status
     */
    public String getRadiusProxyStatus();

    /**
     * Get Radius users
     * @return Map of IP to username
     */
    public Map<String, String> getRadiusUsers();

    /**
     * Adds computer account to the Active Directory domain controller
     */
    public ExecManagerResult addRadiusComputerAccount();

    /**
     * Tests Active Directory authentication with passed credentials
     */
    public String testRadiusProxyLogin(String userName, String userPass, String userDomain);

    /**
     * Sets if radius computer account has been added successfully
     */
    public void setRadiusProxyComputerAccountExists(boolean radiusProxyComputerAccountExists);

    /**
     * Gets if radius computer account has been added successfully
     */
    public boolean getRadiusProxyComputerAccountExists();
    
}
