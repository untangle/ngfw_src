package com.untangle.app.web_cache; // IMPL

import java.lang.Thread;
import java.lang.Number;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;

public enum WebCacheParent implements Runnable
{
    INSTANCE;

    private Hashtable<Long,WebCacheSessionInfo> socklist = new Hashtable<Long,WebCacheSessionInfo>();
    private final Logger logger = Logger.getLogger(getClass());
    private AtomicInteger UserCount = new AtomicInteger();
    private AtomicLong SessionIndex = new AtomicLong();
    private ServerSocketChannel server = null;
    private InetSocketAddress address = null;
    private Boolean killflag = false;
    private Thread runner = null;

    WebCacheParent()
    {
    }

    public void run()
    {
    logger.debug("WebCacheParent socket thread is starting");
    killflag = false;

        try
        {
        // open and bind socket for inbound connections
        address = new InetSocketAddress("127.0.0.1",8888);
        server = ServerSocketChannel.open();
        server.configureBlocking(true);
        server.socket().setReuseAddress(true);
        server.socket().bind(address);
        }

        catch(Throwable t)
        {
        WebCacheStackDump.error(logger,"WebCacheParent","run(open)",t);
        killflag = true;
        }

    logger.debug("WebCacheParent is waiting for connections on " + address.toString());

        // sit in a loop listening for and processing squid cache parent requests
        while (killflag == false)
        {
            try
            {
            SocketChannel client = server.accept();

                if (client != null)
                {
                logger.debug("WebCacheParent received an inbound connection from " + client.toString());
                client.configureBlocking(true);
                Boolean callstat = ProcessChildSession(client);
                if (callstat == false) client.close();
                }
            }

            catch(Throwable t)
            {
            if (killflag == false) WebCacheStackDump.error(logger,"WebCacheParent","run(loop)",t);
            }
        }

    logger.debug("WebCacheParent socket thread is terminating");
    }

    private Boolean ProcessChildSession(SocketChannel client)
    {
    ByteBuffer rxbuff = ByteBuffer.allocate(8192);
    long myindex = 0;
    int retval = 0;

        try
        {
        // read the request from squid
        rxbuff.clear();
        retval = client.read(rxbuff);
        }

        catch(Throwable t)
        {
        WebCacheStackDump.error(logger,"WebCacheParent","ProcessChildSession()",t);
        return(false);
        }

        if (retval < 1)
        {
        logger.debug("----- PARENT DETECTED CLOSED SOCKET -----");
        return(false);
        }

    String scanner = new String(rxbuff.array(),0,retval);
    logger.debug("----- PARENT RECEIVED " + scanner.length() + " BYTES FROM SQUID -----\n" + scanner);

    // extract our magic header from the request
    String look = "X-UNTANGLE-WEBCACHE: ";
    int top = scanner.toUpperCase().indexOf(look);
    int end = scanner.indexOf("\r\n",top);

        if ((top < 0) || (end < 0))
        {
        logger.debug("----- PARENT MISSING WEBCACHE TAG -----");
        return(false);
        }

    // extract the client session object index from the request header
    myindex = Long.parseLong(scanner.substring(top + look.length(),end));

    logger.debug("----- PARENT PROCESSING INDEX " + myindex + " -----");

    // find the corresponding client session object in the hashtable and bail if missing
    WebCacheSessionInfo sessInfo = SearchSockObject(myindex);

        if (sessInfo == null)
        {
        logger.debug("----- PARENT MISSING HASHTABLE ENTRY -----");
        return(false);
        }

        if (sessInfo.squidSelector == null)
        {
        logger.debug("----- PARENT FOUND EMPTY SELECTOR -----");
        return(false);
        }

    // store the squid parent socket channel and wake up the
    // thread that is waiting for a response from squid
    sessInfo.parentChannel = client;
    sessInfo.squidSelector.wakeup();
    return(true);
    }

    public void connect()
    {
    int local = UserCount.getAndIncrement();
    logger.debug("WebCacheParent instance " + (local + 1) + " connected");
    if (local != 0) return;

    runner = new Thread(this,"WebCacheParent");
    runner.start();
    }

    public void goodbye()
    {
    int local = UserCount.decrementAndGet();
    logger.debug("WebCacheParent instance " + (local + 1) + " disconnected");
    if (local != 0) return;

        try
        {
        killflag = true;
        server.close();
        }

        catch(Throwable t)
        {
        WebCacheStackDump.error(logger,"WebCacheParent","shutdown()",t);
        }
    }

    public long GetCacheIndex()
    {
    long local = SessionIndex.getAndIncrement();
    return(local);
    }

    public void InsertSockObject(WebCacheSessionInfo sessInfo)
    {
        synchronized(socklist)
        {
        socklist.put(sessInfo.myIndex,sessInfo);
        }
    }

    public void RemoveSockObject(WebCacheSessionInfo sessInfo)
    {
        synchronized(socklist)
        {
        socklist.remove(sessInfo.myIndex);
        }
    }

    public WebCacheSessionInfo SearchSockObject(long argIndex)
    {
    WebCacheSessionInfo local;

        synchronized(socklist)
        {
        local = socklist.get(argIndex);
        }

    return(local);
    }
}
