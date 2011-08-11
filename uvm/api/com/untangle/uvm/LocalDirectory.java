/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/uvm/addrbook/RemoteAddressBook.java $
 */
package com.untangle.uvm;

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
    boolean authenticate(String uid, String pwd);

    /**
     * Add a new user
     *
     * @returns true if added, false otherwise
     */
    boolean addUser(LocalDirectoryUser newUser);
}


