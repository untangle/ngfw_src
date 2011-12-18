/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.util.List;

/**
 * Allows <code>LogEvent</code>s to be logged 
 */
public abstract class EventLogger<E extends LogEvent>
{
    public abstract void log(E e);
}
