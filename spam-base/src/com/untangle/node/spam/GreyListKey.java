/**
 * $Id: GreyListKey.java,v 1.00 2014/12/06 16:20:33 dmorris Exp $
 */
package com.untangle.node.spam;

import java.net.InetAddress;
import java.util.Map;
import java.util.LinkedHashMap;

public class GreyListKey
{
    public InetAddress client;
    public String envelopeFrom;
    public String envelopeTo;

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

    public String toString()
    {
        return "GreyListKey [ " + this.client.getHostAddress() + ", " + this.envelopeFrom + ", " + this.envelopeTo + " ]";
    }
}
