/*
 * $Id: LocalDirectory.java,v 1.00 2011/08/17 14:18:00 dmorris Exp $
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
    
}


