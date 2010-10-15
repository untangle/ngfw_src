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

package com.untangle.uvm.license;

class License
{
    /** Identifier for the product this license is for */
    private final String productIdentifier;

    /** The mackage this license is in, this is not hashed for the key */
    private final String mackage;

    /** This is the type of license */
    private final String type;

    /** Start date for the license */
    private final long start;
    
    /** End date for the license */
    private final long end;

    /** Key for the license */
    private final String key;

    /** the version of the key signing algorithm used */
    private final int keyVersion;

    License( String identifier, String mackage, String type, long start, long end, String key, int keyVersion )
    {
        this.productIdentifier = identifier;
        this.mackage = mackage;
        this.type = type;
        this.start = start;
        this.end = end;
        this.key = key;
        this.keyVersion = keyVersion;
    }
        
    /**
     * Get the product identifier for this license.
     */
    String getProductIdentifier()
    {
        return this.productIdentifier;
    }

    /**
     * Get the type of license.  This would be used for a
     * descriptive name of the license, like 30 Day Trial, Academic,
     * Professional, etc.
     */
    String getType()
    {
        return this.type;
    }

    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    long getStart()
    {
        return this.start;
    }

    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    long getEnd()
    {
        return this.end;
    }
    
    /**
     * Set the key for this license.
     */
    String getKey()
    {
        return this.key;
    }

    String getMackage()
    {
        return this.mackage;
    }

    /**
     * Set the key version for this license.
     */
    int getKeyVersion()
    {
        return this.keyVersion;
    }

    public String toString()
    {
        return "<" + this.productIdentifier + "/" + this.type + "/" + this.mackage + ">";
    }
}
