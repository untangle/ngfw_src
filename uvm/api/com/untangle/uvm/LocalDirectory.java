/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/uvm/addrbook/RemoteAddressBook.java $
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
    public boolean authenticate(String uid, String pwd);

    /**
     * Return a list of users
     *
     * @returns the current list (never null)
     */
    public LinkedList<LocalDirectoryUser> getUsers();

    /**
     * Save a new list of users
     *
     */
    public void setUsers(LinkedList<LocalDirectoryUser> users);
    
}


