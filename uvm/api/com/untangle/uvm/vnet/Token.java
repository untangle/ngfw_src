/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

/**
 * A pipeline token. This interface will have stuff like toBytes,
 * size, etc...
 *
 */
public interface Token
{
    ByteBuffer getBytes();
}
