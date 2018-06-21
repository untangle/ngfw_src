/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;

/**
 * The Local Directory API provides functions for managing and authenticating "local users."
 * This is useful for some apps with lists of users (like Captive Portal)
 */
public interface LocalDirectory
{
    /**
     * Authenticate a user
     *
     * @returns true if valid uid/password, false otherwise
     */
    public boolean authenticate( String username, String password );

    /**
     * Return a list of users
     *
     * @returns the current list (never null)
     */
    public LinkedList<LocalDirectoryUser> getUsers();

    /**
     * Save a new list of users
     */
    public void setUsers( LinkedList<LocalDirectoryUser> users );

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

}


