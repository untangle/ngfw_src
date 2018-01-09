/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * This class is used to hold the objects that are used to pass data between the
 * network client and squid when processing requests.
 * 
 * @author mahotz
 * 
 */
public class WebCacheSessionInfo
{
    SocketChannel parentChannel; // squid cache peer connection back to us
    SocketChannel squidChannel; // our client side connection to squid
    ByteBuffer clientBuffer; // used to buffer a complete client request
    SelectionKey squidKey;
    Selector squidSelector;
    long myIndex;

    /**
     * Constructor
     */
    public WebCacheSessionInfo()
    {
        parentChannel = null;
        squidChannel = null;
        clientBuffer = null;
        squidKey = null;
        squidSelector = null;
        myIndex = 0;
    }
}
