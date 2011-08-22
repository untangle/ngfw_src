/*
 * $Id$
 */
package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.Date;

/**
 * Marker class for messages.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 * @see MessageQueue
 */
@SuppressWarnings("serial")
public class Message implements Serializable
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
}
