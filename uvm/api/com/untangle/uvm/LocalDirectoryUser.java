/*
 * $Id: LocalDirectoryUser.java,v 1.00 2011/08/10 15:27:40 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;


/**
 * Lightweight class to encapsulate an entry (user)
 * for the Local Directory
 *
 */
@SuppressWarnings("serial")
public final class LocalDirectoryUser implements Serializable, Comparable<LocalDirectoryUser>
{
    public static final String UNCHANGED_PASSWORD = "***UNCHANGED***";

    private String username;

    private String firstName;
    private String lastName;
    
    private String email;
    private String password;
    private String passwordShaHash;
    private String passwordMd5Hash;
    private String passwordBase64Hash;

    private long expirationTime;

    public LocalDirectoryUser()
    {
        this(null, null, null, null);
    }

    public LocalDirectoryUser(String username, String firstName, String lastName, String email)
    {
        this(username, firstName, lastName, email, null);
    }

    public LocalDirectoryUser(String username, String firstName, String lastName, String email, String password)
    {
        this.firstName = makeNotNull(firstName);
        this.lastName = makeNotNull(lastName);
        setUsername(username);
        this.email = makeNotNull(email);
        setPassword(password);
    }
    
    /**
     * Get the username
     *
     * @return the unique id
     */
    public String getUsername()
    {
        return this.username;
    }

    /**
     * Set the username
     * Local Directory is case insensitive, so this will always call toLowerCase before saving
     */
    public void setUsername(String username)
    {
        this.username = makeNotNull(username).toLowerCase();
    }


    /**
     * Get the passwordShaHash to used in account creation
     *
     * @return the passwordShaHash
     */
    public String getPasswordShaHash()
    {
        return this.passwordShaHash;
    }

    public void setPasswordShaHash(String passwordShaHash)
    {
        this.passwordShaHash = makeNotNull(passwordShaHash);
    }

    /**
     * Get the passwordMd5Hash to used in account creation
     *
     * @return the passwordMd5Hash
     */
    public String getPasswordMd5Hash()
    {
        return this.passwordMd5Hash;
    }

    public void setPasswordMd5Hash(String passwordMd5Hash)
    {
        this.passwordMd5Hash = makeNotNull(passwordMd5Hash);
    }

    /**
     * Get the passwordBase64Hash to used in account creation
     *
     * @return the passwordBase64Hash
     */
    public String getPasswordBase64Hash()
    {
        return this.passwordBase64Hash;
    }

    public void setPasswordBase64Hash(String passwordBase64Hash)
    {
        this.passwordBase64Hash = makeNotNull(passwordBase64Hash);
    }
    
    /**
     * Get the password (in the clear)
     * Note: This is often blanked out before saving the user,
     * but this field can be used when passing the object around before saving
     *
     * @return the password or null
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
     * Get the firstname (i.e. "Emma"). 
     *
     * @return the first name.
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = makeNotNull(firstName);
    }

    /**
     * Get the surname (i.e. "Wilson").
     *
     * @return the last name.
     */
    public String getLastName()
    {
        return this.lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = makeNotNull(lastName);
    }

    /**
     * Get the email address associated with the given user. 
     *
     * @return the address
     */
    public String getEmail()
    {
        return this.email;
    }

    public void setEmail(String email)
    {
        this.email = makeNotNull(email);
    }

    /**
     * This clears the cleartext password (useful before saving)
     * but maintains the hashes
     */
    public void removeCleartextPassword()
    {
        this.password = null;
    }

    /**
     * Gets the account expiration time
     *
     * @return expiration time in milliseconds expressing the difference between the current time and midnight, January 1, 1970 UTC.
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets the account expiration time
     *
     * @param expirationTime
     */
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Equality test based on username (case insensitive)
     */
    public boolean equals(Object obj)
    {
        LocalDirectoryUser other = (LocalDirectoryUser) obj;
        return makeNotNull(other.getUsername()).equals(makeNotNull(this.username));
    }
    
    /**
     * Compare two users (checks for equal usernames but ignores case)
     */
    public int compareTo(LocalDirectoryUser e)
    {
        return this.username.compareToIgnoreCase(e.getUsername());
    }

    /**
     * hashcode for use in hashing
     */
    public int hashCode()
    {
        return makeNotNull(this.username).hashCode();
    }
    
    /**
     * For debugging
     */
    public String toString()
    {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder ret = new StringBuilder();

        ret.append("Username:").append(getUsername()).append(newLine);
        ret.append("First:").append(getFirstName()).append(newLine);
        ret.append("Last:").append(getLastName()).append(newLine);
        ret.append("Email:").append(getEmail()).append(newLine);

        return ret.toString();
    }

    private String makeNotNull(String obj)
    {
        return obj==null?"":obj;
    }

}
