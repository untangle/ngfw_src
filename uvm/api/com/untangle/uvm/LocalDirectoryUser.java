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
    private String passwordHash;

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
     * Get the passwordHash to used in account creation
     *
     * @return the passwordHash
     */
    public String getPasswordHash()
    {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash)
    {
        this.passwordHash = makeNotNull(passwordHash);
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
        this.passwordHash = password; /* FIXME hash */
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
