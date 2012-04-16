/**
 * $Id$
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
