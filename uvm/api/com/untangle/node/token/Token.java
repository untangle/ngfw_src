/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

/**
 * A pipeline token. This interface will have stuff like toBytes,
 * size, etc...
 *
 */
public interface Token
{
    ByteBuffer getBytes();
    int getEstimatedSize();
}
