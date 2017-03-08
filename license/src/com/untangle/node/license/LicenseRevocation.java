/*
 * $Id$
 */
package com.untangle.node.license;

import java.io.Serializable;
import java.util.Date;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class LicenseRevocation implements Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    /** Identifier for the product this license revocation is for */
    private String name;

    /** The UID for this license revocation */
    private String uid;

    public LicenseRevocation()
    {
    }

    public LicenseRevocation( String uid, String name )
    {
        this.uid = uid;
        this.name = name;
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
     * Returns the unique identifier of the product of this license
     */
    public String getUID()
    {
        return this.uid;
    }
    
    public void setUID( String uid )
    {
        this.uid = uid;
    }
    
    public String toString()
    {
        return "<" + this.uid + "/" + this.name + ">";
    }

}
