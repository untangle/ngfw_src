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

    /* whether message can be closed. true is can be closed, false if not */
    private boolean hasClosure;

    /* type of message */
    private UserLicenseMessageType type;

    /* if should show as banner or just an alert */
    private boolean showAsBanner; 

    /**
     * Blank constructor
     */
    public UserLicenseMessage()
    {
        this.message = "";
        this.hasClosure = true;
        this.type = UserLicenseMessageType.INFO;
        this.showAsBanner = true;
    }

    /**
     * Constructor
     *
     * @param message message of UserLicenseMessage
     *
     * @param hasClosure if message can be closable in UI
     *
     * @param type type of UserLicenseMessage from enum
     *
     * @param showAsBanner if message will show as banner in the UI
     */
    public UserLicenseMessage(String message, boolean hasClosure, UserLicenseMessageType type, boolean showAsBanner) 
    {
        this.message = message;
        this.hasClosure = hasClosure;
        this.type = type;
        this.showAsBanner = showAsBanner;
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
    public boolean getHasClosure() 
    {
        return this.hasClosure; 
    }

    /**
     * Set if message can be closed or not 
     *
     * @param newValue if message can closed or not 
     */
    public void setHasClosure(boolean newValue) 
    {
        this.hasClosure = newValue;
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

    /**
     * Get if should show message in banner
     *
     * @return if message should show as a banner
     */
    public boolean getShowAsBanner() {
        return this.showAsBanner;
    }

    /**
     * Set if message should show as banner
     *
     * @param newValue the boolean for if message will be shown as banner
     */
    public void setShowAsBanner(boolean newValue) {
        this.showAsBanner = newValue;
    }

    /** 
     * Converts User License Message to string
     *
     * @return string representation 
     */
    @Override
    public String toString() {  
        return "<" + this.type + "/" + this.message + "/" + this.hasClosure + ">";
    }
}
