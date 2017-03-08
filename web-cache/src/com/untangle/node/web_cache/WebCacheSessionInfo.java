package com.untangle.node.web_cache; // IMPL

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class WebCacheSessionInfo
{
    SocketChannel parentChannel;    // squid cache peer connection back to us
    SocketChannel squidChannel;     // our client side connection to squid
    ByteBuffer clientBuffer;        // used to buffer a complete client request
    SelectionKey squidKey;
    Selector squidSelector;
    long myIndex;

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
