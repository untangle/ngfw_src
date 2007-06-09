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

package com.untangle.uvm.addrbook;

import java.io.Serializable;
import java.util.List;


/**
 * Lightweight class to encapsulate an entry (user)
 * in the Address Book service.
 *
 */
public final class UserEntry implements Serializable {

    public static final String UNCHANGED_PASSWORD = "***UNCHANGED***";

    private String m_firstName;
    private String m_lastName;
    private String m_uid;
    private String m_email;
    private RepositoryType m_storedIn;
    private String m_password;
    private String m_comment;

    public UserEntry() {
        this(null, null, null, null, RepositoryType.NONE);
    }

    public UserEntry(String uid,
                     String firstName,
                     String lastName,
                     String email) {

        m_firstName = firstName;
        m_lastName = lastName;
        m_uid = uid;
        m_email = email;
        m_storedIn = RepositoryType.NONE;
    }

    public UserEntry(String uid,
                     String firstName,
                     String lastName,
                     String email,
                     RepositoryType storedIn) {

        m_firstName = firstName;
        m_lastName = lastName;
        m_uid = uid;
        m_email = email;
        m_storedIn = storedIn;
    }

    /**
     * Get the uniqueID within the scope
     * of the {@link #getStoredIn repository}
     *
     * @return the unique id
     */
    public String getUID() {
        return m_uid;
    }

    public void setUID(String uid) {
        m_uid = uid;
    }

    /**
     * Get the password to used in account creation
     *
     * @return the password
     */
    public String getPassword() {
        return m_password;
    }

    public void setPassword(String password) {
        m_password = password;
    }

    /**
     * Get the comment;
     *
     * @return the comment
     */
    public String getComment() {
        if( m_comment != null )
            return m_comment;
        else
            return "";
    }

    public void setComment(String comment) {
        m_comment = comment;
    }

    /**
     * Get the firstname (i.e. "Emma").  This
     * may be null.
     *
     * @return the first name.
     */
    public String getFirstName() {
        return m_firstName;
    }

    public void setFirstName(String name) {
        m_firstName = name;
    }

    /**
     * Get the surname (i.e. "Wilson").  This
     * may be null.
     *
     * @return the last name.
     */
    public String getLastName() {
        return m_lastName;
    }

    public void setLastName(String name) {
        m_lastName = name;
    }

    /**
     * Get the email address associated with the given user.  This
     * may be null.
     *
     * @return the address
     */
    public String getEmail() {
        return m_email;
    }

    public void setEmail(String email) {
        m_email = email;
    }

    /**
     * Get the type of repository in-which this entry is stored.  Note
     * that this property only applies for instances of UserEntry
     * retreived from the {@link AddressBook AddressBook}.
     *
     * @return the repository
     */
    public RepositoryType getStoredIn() {
        return m_storedIn;
    }

    public void setStoredIn(RepositoryType type) {
        m_storedIn = type;
    }


    /**
     * Equality test based on uid (case sensitive - although I'm not sure
     * that is always true) and RepositoryType.
     */
    public boolean equals(Object obj) {
        UserEntry other = (UserEntry) obj;
        return makeNotNull(other.getUID()).equals(makeNotNull(m_uid)) &&
            makeNotNull(other.getStoredIn()).equals(makeNotNull(m_storedIn));
    }


    /**
     * hashcode for use in hashing
     */
    public int hashCode() {
        return new String(makeNotNull(m_uid).toString() + makeNotNull(m_storedIn).toString()).hashCode();
    }


    /**
     * For debugging
     */
    public String toString() {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder ret = new StringBuilder();

        ret.append("UID:").append(getUID()).append(newLine);
        ret.append("First:").append(getFirstName()).append(newLine);
        ret.append("Last:").append(getLastName()).append(newLine);
        ret.append("Email:").append(getEmail()).append(newLine);
        ret.append("Repository:").append(getStoredIn()).append(newLine);

        return ret.toString();
    }

    private Object makeNotNull(Object obj) {
        return obj==null?"":obj;
    }

}
