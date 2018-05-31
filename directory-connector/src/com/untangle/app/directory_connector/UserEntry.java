/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.io.Serializable;

/**
 * Lightweight class to encapsulate an entry (user)
 * in the Address Book service.
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
    private String password;
    private String comment;

    /**
     * Constructor for empty user
     */
    public UserEntry()
    {
        this(null, null, null, null, null, null);
    }

    /**
     * Constructor for some user information.
     *
     * @param uid
     *      User account UID.
     * @param firstName
     *      User's first name.
     * @param lastName
     *      User's last name.
     * @param email
     *      User's email address.
     */
    public UserEntry(String uid, String firstName, String lastName, String email)
    {
        this(uid,firstName,lastName,email,null,null);
    }

    /**
     * Constructor for more user information.
     *
     * @param uid
     *      User account user id.
     * @param firstName
     *      User's first name.
     * @param lastName
     *      User's last name.
     * @param email
     *      User's email address.
     * @param primaryGroupID
     *      Primary group ud.
     * @param dn
     *      User's distinguished name.
     */
    public UserEntry(String uid, String firstName, String lastName, String email, String primaryGroupID, String dn)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        setUid(uid);
        this.email = email;
        this.dn = dn;
        this.primaryGroupID = primaryGroupID;
    }

    /**
     * Get the uniqueID within the scope
     * of the {@link #getStoredIn repository}
     *
     * @return the unique id
     */
    public String getUid()
    {
        return this.uid;
    }
    /**
     * Set user's id.
     *
     * @param uid
     *      User's account user id
     */
    public void setUid(String uid)
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
    /**
     * Set user password.
     *
     * @param password
     *      User password
     */
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
    /**
     * Set the comment.
     *
     * @param comment
     *      Comment for this user.
     */
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
    /**
     * Set user's first name.
     *
     * @param name
     *      User's first name.
     */
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
    /**
     * Set user's surname.
     *
     * @param name
     *      User's surname.
     */
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
    /**
     * Set user's emaul address.
     *
     * @param email
     *      User's email address.
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * Get user's primary group id.
     *
     * @return User's primary group id.
     */
    public String getPrimaryGroupID()
    {
        return this.primaryGroupID;
    }
    /**
     * Set user's primary group id.
     *
     * @param groupID
     *      User's new group id.
     */
    public void setPrimaryGroupID( String groupID )
    {
        this.primaryGroupID = groupID;
    }
    
    /**
     * Get user's distinguished name
     *
     * @return Distinguished name
     */
    public String getDN()
    {
        return this.dn;
    }
    /**
     * Set user's distinguished name
     *
     * @param dn
     *      Distinguished name
     */
    public void setDN( String dn )
    {
        this.dn = dn;
    }

    /**
     * Equality test based on uid (case sensitive - although I'm not sure
     * that is always true) and RepositoryType.
     *
     * @param obj
     *  Object to compare against this.
     * @return true if equal, false otherwise
     */
    public boolean equals(Object obj)
    {
        UserEntry other = (UserEntry) obj;
        return makeNotNull(other != null ? other.getUid() : other).equals(makeNotNull(this.uid)) ? true : false;
    }
    
    /**
     * Compare uid
     *
     * @param e
     *      Compare with other user's uid.
     * @return integer value
     */
    public int compareTo(UserEntry e)
    {
        return this.uid.compareToIgnoreCase(e.getUid());
    }

    /**
     * hashcode for use in hashing
     * @return hash of uid.
     */
    public int hashCode()
    {
        return new String(makeNotNull(this.uid).toString()).hashCode();
    }
    
    /**
     * For debugging
     *
     * @return string value of object.
     */
    public String toString()
    {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder ret = new StringBuilder();

        ret.append("Uid:").append(getUid()).append(newLine);
        ret.append("First:").append(getFirstName()).append(newLine);
        ret.append("Last:").append(getLastName()).append(newLine);
        ret.append("Email:").append(getEmail()).append(newLine);

        return ret.toString();
    }

    /**
     * Return object as non-null value.
     *
     * @param obj
     *      Object to test.
     * @return object
     */
    private Object makeNotNull(Object obj)
    {
        return obj==null?"":obj;
    }

}
