/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: License.java 27705 2010-10-15 01:44:20Z dmorris $
 */

package com.untangle.uvm.license;

import java.io.Serializable;
import java.util.Date;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class License implements Serializable
{
    private final Logger logger = Logger.getLogger(License.class);

    /**
     * Various Names
     */
    public static final String ADCONNECTOR = "untangle-node-policy";
    public static final String POLICY = "untangle-node-policy";
    public static final String PORTAL = "untangle-node-portal";
    public static final String KAV = "untangle-node-kav";
    public static final String SITEFILTER = "untangle-node-sitefilter";
    public static final String PCREMOTE = "untangle-node-pcremote";
    public static final String BRANDING = "untangle-node-branding";
    public static final String COMMTOUCH = "untangle-node-commtouch";
    public static final String SPLITD = "untangle-node-splitd";
    public static final String FAILD = "untangle-node-faild";
    public static final String BANDWIDTH = "untangle-node-bandwidth";
    public static final String BOXBACKUP = "untangle-node-boxbackup";
    
    /** Identifier for the product this license is for */
    private String name;

    /** The human readable name */
    private String displayName;
    
    /** This is the type of license */
    private String type;

    /** Start date for the license */
    private long start;
    
    /** End date for the license */
    private long end;

    /** Key for the license */
    private String key;

    /** the version of the key signing algorithm used */
    private int keyVersion;

    private Boolean valid;
    
    License( String name, String displayName, String type, long start, long end, String key, int keyVersion, Boolean valid )
    {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.start = start;
        this.end = end;
        this.key = key;
        this.keyVersion = keyVersion;
    }
        
    /**
     * Returns the unique identifier of the product of this license
     */
    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Returns the human readable name of the product of this license
     */
    public String getDisplayName()
    {
        return this.displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    /**
     * Get the type of license.  This would be used for a
     * descriptive name of the license, like 30 Day Trial, Academic,
     * Professional, etc.
     */
    public String getType()
    {
        return this.type;
    }

    public void setType( String type )
    {
        this.type = type;
    }
    
    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    public long getStart()
    {
        return this.start;
    }

    public void setStart( long start )
    {
        this.start = start;
    }
    
    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    public long getEnd()
    {
        return this.end;
    }

    public void setEnd( long end )
    {
        this.end = end;
    }
    
    /**
     * Set the key for this license.
     */
    public String getKey()
    {
        return this.key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }
    
    /**
     * Set the key version for this license.
     */
    public int getKeyVersion()
    {
        return this.keyVersion;
    }

    public void setKeyVersion( int keyVersion )
    {
        this.keyVersion = keyVersion;
    }
        
    /**
     * Returns the status of this license
     * Set by license manager
     */
    public Boolean isValid()
    {
        if (this.valid == null)
            return Boolean.FALSE;

        return this.valid;
    }

    public Boolean getValid()
    {
        return this.valid;
    }

    public void setValid( Boolean valid )
    {
        this.valid = valid;
    }

    /**
     * XXX
     */
    public Boolean isExpired()
    {
        logger.error("IMPLEMENT ME");
        return null;

        //return !this.isValid();
    }
    
    /**
     * XXX
     */
    public String getTimeRemaining()
    {
        logger.error("IMPLEMENT ME");
        return null;
    }

    /**
     * XXX
     */
    public Date getExpirationDate()
    {
        logger.error("IMPLEMENT ME");
        return null;
    }
    
    public String toString()
    {
        return "<" + this.name + "/" + this.type + "/" + ">";
    }
}
