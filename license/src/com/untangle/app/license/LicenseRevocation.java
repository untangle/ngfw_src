/**
 * $Id$
 */
package com.untangle.app.license;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import org.apache.log4j.Logger;

/**
 * License revocations
 */
@SuppressWarnings("serial")
public class LicenseRevocation implements Serializable, JSONString
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
     *
     * @return
     *     Name of product
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
     *
     * @return
     *     UID for license revocation.
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

    /**
     * Convert settings to JSON string.
     *
     * @return
     *      JSON string.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
