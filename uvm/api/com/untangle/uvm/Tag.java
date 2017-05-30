/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * A tag is just a generic string that you can append to various objects (hosts, sessions, devices, users) that expired at a certain time.
 */
@SuppressWarnings("serial")
public class Tag implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(Tag.class);

    public static final int EXPIRE_NEVER = 0;
    public static final int EXPIRE_END_OF_HOUR  = -1;
    public static final int EXPIRE_END_OF_DAY   = -2;
    public static final int EXPIRE_END_OF_WEEK  = -3;
    public static final int EXPIRE_END_OF_MONTH = -4;

    private String name;
    private long expirationTime = 1;

    public Tag() {}

    public Tag( String name )
    {
        this.name = name;
        this.expirationTime = 0;
    }

    public Tag( String name, long expirationTime )
    {
        setName( name );
        setExpirationTime( expirationTime );
    }

    public String getName()
    {
        return this.name;
    }

    public void setName( String newValue )
    {
        this.name = newValue;
    }

    public long getExpirationTime()
    {
        return this.expirationTime;
    }

    public void setExpirationTime( long newValue )
    {
        this.expirationTime = calculateExpirationTime( newValue );
    }

    public void setLifetimeMillis( long millis )
    {
        if ( millis > 0 )
            setExpirationTime( System.currentTimeMillis() + millis );
        else
            setExpirationTime( millis );
    }
    
    public boolean isExpired()
    {
        /* automatically expired blank tags */
        if ( this.name == null || "".equals(this.name)) {
            return true;
        }
        if ( this.expirationTime < 0 ) {
            logger.warn("Invalid expiration time: " + this.expirationTime );
            return false;
        }
        if ( this.expirationTime == EXPIRE_NEVER ) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now >= this.expirationTime)
            return true;
        else
            return false;
    }

    public boolean isValid()
    {
        return !isExpired();
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public String toString()
    {
        return getName();
    }
    
    public static long calculateExpirationTime( long millis )
    {
        switch (((int)millis)) {
        case EXPIRE_END_OF_HOUR: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.HOUR, 1);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            expireDate = calendar.getTime();
            return expireDate.getTime();
        }
                
        case EXPIRE_END_OF_DAY: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.HOUR, 24); // add day
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.SECOND, -1);
            /* subtract one second so as to avoid the whole AM/PM midnight confusion*/

            expireDate = calendar.getTime();
            return expireDate.getTime();
        }
        case EXPIRE_END_OF_WEEK: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.HOUR, 168); // add week
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.SECOND, -1);
            /* subtract one second so as to avoid the whole AM/PM midnight confusion*/

            expireDate = calendar.getTime();
            return expireDate.getTime();
        }

        case EXPIRE_END_OF_MONTH: {
            GregorianCalendar calendar = new GregorianCalendar();
            Date now = calendar.getTime();
            Date expireDate = null;
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.SECOND, -1);
            /* subtract one second so as to avoid the whole AM/PM midnight confusion*/

            expireDate = calendar.getTime();
            return expireDate.getTime();
        }

        case EXPIRE_NEVER:
            return 0;
            
        default:
            return millis;
        }
    }

    public static String tagsToString( Collection<Tag> tags )
    {
        if ( tags == null ) return "";
        return tags.stream().map(i -> i.getName()).collect(Collectors.joining(","));
    }
}
