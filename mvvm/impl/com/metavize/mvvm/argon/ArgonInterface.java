/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IntfConverter.java 7128 2006-09-06 17:32:14Z rbscott $
 */

package com.metavize.mvvm.argon;


/** ArgonInterface:
 * Contains information about a physical interfaces. */
final class ArgonInterface
{
    /* Physical name of the interface */
    private final String name;

    /* Netcap index for the interface */
    private final byte netcap;

    /* Argon index for the interface */
    private final byte argon;

    /* The string value */
    private final String string;
    

    /**
     * In order to avoid the pain of typecasting everywhere, netcap and argon are 
     * should be bytes, but are typecast inside of the constructor 
     */
    ArgonInterface( String name, int netcap, int argon )
    {
        this.name = name;
        this.netcap = (byte)netcap;
        this.argon = (byte)argon;
        this.string =  "'" + this.name + "' " + this.argon + "/" + this.netcap;
    }

    /** Get the linux/physical name of the interface (eg. eth0) */
    public String getName()
    {
        return this.name;
    }

    /** Get the index that netcap uses to reference the interface */
    public byte getNetcap()
    {
        return this.netcap;
    }

    /** Get the index that argon uses to reference the interface */
    public byte getArgon()
    {
        return this.argon;
    }
    
    public String toString()
    {
        return this.string;
    }
}
