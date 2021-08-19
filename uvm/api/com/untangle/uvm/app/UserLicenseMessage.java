/**
 * $Id$
 */

package com.untangle.uvm.app;

import org.apache.log4j.Logger;

/**
 The UserLicense message class
 */
public class UserLicenseMessage 
{
    private final Logger logger = Logger.getLogger(UserLicenseMessage.class);

    /* enum for UserLicense message type */
    public enum UserLicenseMessageType {
        ALERT,
        WARNING,
        INFO,
    }; 

    /* message to display */
    private String message; 

    /* whether message can be closed */
    private boolean closure;

    /* type of message */
    private UserLicenseMessageType type;

    /**
     * Constructor
     *
     * @param message message of UserLicenseMessage
     *
     * @param closure if message can be closable in UI
     *
     * @param type type of UserLicenseMessage from enum
     */
    public UserLicenseMessage(String message, boolean closure, UserLicenseMessageType type) 
    {
        this.message = message;
        this.closure = closure;
        this.type = type;
    }

    /**
     * Returns the message that will be displayed
     *
     * @return the message
     */
    public String getMessage() 
    {
        return this.message;
    }

    /**
     * Set the message 
     *
     * @param newValue the new message
     */
    public void setMessage(String newValue)
    {
        this.message = newValue;
    }

    /**
     * Returns whether the message is able to be closed in the UI or not 
     * If it can be closed, it will be true, false otherwise
     *
     * @return the closure true/false
     */
    public boolean getClosure() 
    {
        return this.closure; 
    }

    /**
     * Set if message can be closed or not 
     *
     * @param newValue if message can closed or not 
     */
    public void setClosure(boolean newValue) 
    {
        this.closure = newValue;
    }

    /**
     * Returns what type of LicenseUserMessage it is, alert, info, warning, etc.
     *
     * @return the type
     */
    public UserLicenseMessageType getType() 
    {
        return this.type;
    }

    /**
     * Set the type of message for the LicenseUserMessage
     *
     * @param newValue the new type
     */
    public void setType(UserLicenseMessageType newValue)
    {
        this.type = newValue;
    }
}