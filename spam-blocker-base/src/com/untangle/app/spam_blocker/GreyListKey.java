/**
 * $Id: GreyListKey.java,v 1.00 2014/12/06 16:20:33 dmorris Exp $
 */

package com.untangle.app.spam_blocker;

import java.io.Serializable;
import java.net.InetAddress;
import org.json.JSONString;

/**
 * Grey List Key Implementation
 */
@SuppressWarnings("serial")
public class GreyListKey implements Serializable, JSONString
{
    public InetAddress client;
    public String envelopeFrom;
    public String envelopeTo;

    public GreyListKey() {}
    
    public GreyListKey( InetAddress client, String envelopeFrom, String envelopeTo ) 
    {
        this.client = client;
        this.envelopeTo = envelopeTo;
        this.envelopeFrom = envelopeFrom;
    }

    public int hashCode()
    {
        return ( client == null ? 0 : client.hashCode() ) +
            ( envelopeFrom == null ? 0 : envelopeFrom.hashCode() ) +
            ( envelopeTo == null ? 0 : envelopeTo.hashCode() );
    }

    public InetAddress getClient() { return this.client; }
    public void setClient( InetAddress newValue ) { this.client = newValue; }

    public String getEnvelopeFrom() { return this.envelopeFrom; }
    public void setEnvelopeFrom( String newValue ) { this.envelopeFrom = newValue; }

    public String getEnvelopeTo() { return this.envelopeTo; }
    public void setEnvelopeTo( String newValue ) { this.envelopeTo = newValue; }
    
    public boolean equals( Object o2 )
    {
        if ( ! ( o2 instanceof GreyListKey ) ) {
            return false;
        }
        GreyListKey o = (GreyListKey) o2;
        if ( ! ( o.client == null ? this.client == null : o.client.equals(this.client) ) ) {
            return false;
        }
        if ( ! ( o.envelopeFrom == null ? this.envelopeFrom == null : o.envelopeFrom.equals(this.envelopeFrom) ) ) {
            return false;
        }
        if ( ! ( o.envelopeTo == null ? this.envelopeTo == null : o.envelopeTo.equals(this.envelopeTo) ) ) {
            return false;
        }
        return true;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
