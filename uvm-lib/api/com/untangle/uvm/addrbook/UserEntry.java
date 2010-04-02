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

import java.io.Serializable;


/**
 * Lightweight class to encapsulate an entry (user)
 * in the Address Book service.
 *
 */
public final class UserEntry implements Serializable, Comparable {

    public static final String UNCHANGED_PASSWORD = "***UNCHANGED***";

    private String m_firstName;
    private String m_lastName;
    private String m_uid;
    
    private String m_primaryGroupID;
    private String m_dn;
    private String m_email;
    private RepositoryType m_storedIn;
    private String m_password;
    private String m_comment;

    public UserEntry() {
        this(null, null, null, null, null, null, RepositoryType.NONE);
    }

    public UserEntry(String uid,
                     String firstName,
                     String lastName,
                     String email) {

        this(uid,firstName,lastName,email,null,null, RepositoryType.NONE);
    }

    public UserEntry(String uid,
                     String firstName,
                     String lastName,
                     String email,
                     String primaryGroupID,
                     String dn,
                     RepositoryType storedIn) {

        m_firstName = firstName;
        m_lastName = lastName;
        setUID(uid);
        m_email = email;
        m_storedIn = storedIn;
        m_dn = dn;
        m_primaryGroupID = primaryGroupID;
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
        if ( uid == null ) {
            uid = "";
        }
        
        m_uid = uid.toLowerCase();
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
    
    public String getPrimaryGroupID() {
        return this.m_primaryGroupID;
    }
    
    public void setPrimaryGroupID( String newValue )
    {
        this.m_primaryGroupID = newValue;
    }
    
    public String getDN() {
        return this.m_dn;
    }
    
    public void setDN( String newValue )
    {
        this.m_dn = newValue;
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
    
    public int compareTo(Object e) {
        return m_uid.compareToIgnoreCase(((UserEntry)e).getUID());
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
