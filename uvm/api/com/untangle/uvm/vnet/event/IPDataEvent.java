/**
 * $Id: IPDataEvent.java 31696 2012-04-16 21:05:30Z dmorris $
 */
package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;

/**
 * Base of all IP data events
 */
public interface IPDataEvent
{
    ByteBuffer data();
}
