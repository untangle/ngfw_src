/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.Serializable;

/**
 * Lightweight class to encapsulate an entry (user) for the Local Directory
 * 
 */
@SuppressWarnings("serial")
public final class LocalDirectoryUser implements Serializable, Comparable<LocalDirectoryUser>
{
    private String username;

    private String firstName;
    private String lastName;

    private String password;
    private String email;
    private String passwordShaHash;
    private String passwordMd5Hash;
    private String passwordBase64Hash;

    private long expirationTime;

    /**
     * Constructor
     */
    public LocalDirectoryUser()
    {
        this(null, null, null, null);
    }

    /**
     * Constructor
     * 
     * @param username
     *        The username
     * @param firstName
     *        The first name
     * @param lastName
     *        The last name
     * @param email
     *        The email address
     */
    public LocalDirectoryUser(String username, String firstName, String lastName, String email)
    {
        this.firstName = makeNotNull(firstName);
        this.lastName = makeNotNull(lastName);
        setUsername(username);
        this.email = makeNotNull(email);
    }

    // public LocalDirectoryUser(String username, String firstName, String lastName, String email, String password)
    // {
    //     this.firstName = makeNotNull(firstName);
    //     this.lastName = makeNotNull(lastName);
    //     setUsername(username);
    //     this.email = makeNotNull(email);
    //     setPassword(password);
    // }

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
     * Set the username Local Directory is case insensitive, so this will always
     * call toLowerCase before saving
     * 
     * @param username
     *        The username
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

    /**
     * Set the password ShaHash
     * 
     * @param passwordShaHash
     *        The hash
     */
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

    /**
     * Set the password MD5 hash
     * 
     * @param passwordMd5Hash
     *        The hash
     */
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

    /**
     * Set the password base64 hash
     * 
     * @param passwordBase64Hash
     *        The hash
     */
    public void setPasswordBase64Hash(String passwordBase64Hash)
    {
        this.passwordBase64Hash = makeNotNull(passwordBase64Hash);
    }

    /**
     * Get the password (in the clear) Note: This is often blanked out before
     * saving the user, but this field can be used when passing the object
     * around before saving
     * 
     * DEPRECATED This is still here because old custom captive portal scripts
     * set the password in the user before calling addUser
     * 
     * @return the password or null
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * Set the password
     * 
     * @param password
     *        The password
     */
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

    /**
     * Set the first name
     * 
     * @param firstName
     *        The first name
     */
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

    /**
     * Set the surname
     * 
     * @param lastName
     *        The last name
     */
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

    /**
     * Set the email address
     * 
     * @param email
     *        The email address
     */
    public void setEmail(String email)
    {
        this.email = makeNotNull(email);
    }

    /**
     * Gets the account expiration time
     * 
     * @return expiration time in milliseconds expressing the difference between
     *         the current time and midnight, January 1, 1970 UTC.
     */
    public long getExpirationTime()
    {
        return expirationTime;
    }

    /**
     * Sets the account expiration time
     * 
     * @param expirationTime
     */
    public void setExpirationTime(long expirationTime)
    {
        this.expirationTime = expirationTime;
    }

    /**
     * Equality test based on username (case insensitive)
     * 
     * @param obj
     *        The object for comparison
     * @return True if equal, otherwise false
     */
    public boolean equals(Object obj)
    {
        LocalDirectoryUser other = (LocalDirectoryUser) obj;
        return makeNotNull(other.getUsername()).equals(makeNotNull(this.username));
    }

    /**
     * Compare two users (checks for equal usernames but ignores case)
     * 
     * @param e
     *        The object for comparison
     * @return The comparison result
     */
    public int compareTo(LocalDirectoryUser e)
    {
        return this.username.compareToIgnoreCase(e.getUsername());
    }

    /**
     * hashcode for use in hashing
     * 
     * @return The hash code
     */
    public int hashCode()
    {
        return makeNotNull(this.username).hashCode();
    }

    /**
     * Convert the object to a string for debugging
     * 
     * @return The string representation
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

    /**
     * Make sure a string is not null
     * 
     * @param obj
     *        The string
     * @return The string or an empty string if null
     */
    private String makeNotNull(String obj)
    {
        return obj == null ? "" : obj;
    }
}
