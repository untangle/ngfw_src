/*
 * $Id: Message.java 35447 2013-07-29 17:24:43Z dmorris $
 */
package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Marker class for messages.
 *
 * @see MessageQueue
 */
@SuppressWarnings("serial")
public class Message implements Serializable, JSONString
{
    private final Date time = new Date();

    @SuppressWarnings("unused")
    public final String getMessageType()
    {
        return getClass().getName();
    }

    @SuppressWarnings("unused")
    public final Date getTime()
    {
        return time;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
