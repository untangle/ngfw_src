/*
 * $Id$
 */
package com.untangle.uvm.addrbook;

import java.io.Serializable;

/**
 * Lightweight class to encapsulate an entry (user)
 * in the Address Book service.
 *
 */
@SuppressWarnings("serial")
public final class UserEntry implements Serializable, Comparable<UserEntry>
{

    public static final String UNCHANGED_PASSWORD = "***UNCHANGED***";

    private String firstName;
    private String lastName;
    private String uid;
    
    private String primaryGroupID;
    private String dn;
    private String email;
    private RepositoryType storedIn;
    private String password;
    private String comment;

    public UserEntry()
    {
        this(null, null, null, null, null, null, RepositoryType.NONE);
    }

    public UserEntry(String uid, String firstName, String lastName, String email)
    {
        this(uid,firstName,lastName,email,null,null, RepositoryType.NONE);
    }

    public UserEntry(String uid, String firstName, String lastName, String email,
                     String primaryGroupID, String dn, RepositoryType storedIn)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        setUID(uid);
        this.email = email;
        this.storedIn = storedIn;
        this.dn = dn;
        this.primaryGroupID = primaryGroupID;
    }

    /**
     * Get the uniqueID within the scope
     * of the {@link #getStoredIn repository}
     *
     * @return the unique id
     */
    public String getUID()
    {
        return this.uid;
    }

    public void setUID(String uid)
    {
        if ( uid == null ) {
            uid = "";
        }
        
        this.uid = uid.toLowerCase();
    }

    /**
     * Get the password to used in account creation
     *
     * @return the password
     */
    public String getPassword()
    {
        return this.password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Get the comment;
     *
     * @return the comment
     */
    public String getComment()
    {
        if( this.comment != null )
            return this.comment;
        else
            return "";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Get the firstname (i.e. "Emma").  This
     * may be null.
     *
     * @return the first name.
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    public void setFirstName(String name)
    {
        this.firstName = name;
    }

    /**
     * Get the surname (i.e. "Wilson").  This
     * may be null.
     *
     * @return the last name.
     */
    public String getLastName()
    {
        return this.lastName;
    }

    public void setLastName(String name)
    {
        this.lastName = name;
    }

    /**
     * Get the email address associated with the given user.  This
     * may be null.
     *
     * @return the address
     */
    public String getEmail()
    {
        return this.email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * Get the type of repository in-which this entry is stored.  Note
     * that this property only applies for instances of UserEntry
     * retreived from the {@link AddressBook AddressBook}.
     *
     * @return the repository
     */
    public RepositoryType getStoredIn()
    {
        return this.storedIn;
    }

    public void setStoredIn(RepositoryType type)
    {
        this.storedIn = type;
    }
    
    public String getPrimaryGroupID()
    {
        return this.primaryGroupID;
    }
    
    public void setPrimaryGroupID( String newValue )
    {
        this.primaryGroupID = newValue;
    }
    
    public String getDN()
    {
        return this.dn;
    }
    
    public void setDN( String newValue )
    {
        this.dn = newValue;
    }

    /**
     * Equality test based on uid (case sensitive - although I'm not sure
     * that is always true) and RepositoryType.
     */
    public boolean equals(Object obj)
    {
        UserEntry other = (UserEntry) obj;
        return makeNotNull(other.getUID()).equals(makeNotNull(this.uid)) &&
            makeNotNull(other.getStoredIn()).equals(makeNotNull(this.storedIn));
    }
    
    public int compareTo(UserEntry e)
    {
        return this.uid.compareToIgnoreCase(e.getUID());
    }

    /**
     * hashcode for use in hashing
     */
    public int hashCode()
    {
        return new String(makeNotNull(this.uid).toString() + makeNotNull(this.storedIn).toString()).hashCode();
    }
    
    /**
     * For debugging
     */
    public String toString()
    {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder ret = new StringBuilder();

        ret.append("UID:").append(getUID()).append(newLine);
        ret.append("First:").append(getFirstName()).append(newLine);
        ret.append("Last:").append(getLastName()).append(newLine);
        ret.append("Email:").append(getEmail()).append(newLine);
        ret.append("Repository:").append(getStoredIn()).append(newLine);

        return ret.toString();
    }

    private Object makeNotNull(Object obj)
    {
        return obj==null?"":obj;
    }

}
