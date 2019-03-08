/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;

/**
 * This class implments a singleton for managing data that is cached by Squid.
 * We operate as the parent cache, and squid operates as a child cache. When
 * procesing client requests, we first check to see if Squid has the content. If
 * it does, we serve the content from Squid and we're done. If not, we fetch the
 * content from the external server, and we send it to both the client and to
 * Squid so it will be cached to satisfy subsequent requests.
 * 
 * @author mahotz
 * 
 */
public enum WebCacheParent implements Runnable
{
    INSTANCE;

    private Hashtable<Long, WebCacheSessionInfo> socklist = new Hashtable<Long, WebCacheSessionInfo>();
    private final Logger logger = Logger.getLogger(getClass());
    private AtomicInteger UserCount = new AtomicInteger();
    private AtomicLong SessionIndex = new AtomicLong();
    private ServerSocketChannel server = null;
    private InetSocketAddress address = null;
    private Boolean killflag = false;
    private Thread runner = null;

    /**
     * Our main function where we open a socket and listen for cache peer
     * connections from Squid, and process them when received.
     * 
     */
    public void run()
    {
        logger.debug("WebCacheParent socket thread is starting");
        killflag = false;

        try {
            // open and bind socket for inbound connections
            address = new InetSocketAddress("127.0.0.1", 8888);
            server = ServerSocketChannel.open();
            server.configureBlocking(true);
            server.socket().setReuseAddress(true);
            server.socket().bind(address);
        }

        catch (Exception exn) {
            logger.warn("Exception connecting to cache peer socket", exn);
            killflag = true;
        }

        logger.debug("WebCacheParent is waiting for connections on " + address.toString());

        // sit in a loop listening for and processing squid cache parent requests
        while (killflag == false) {
            try {
                SocketChannel client = server.accept();

                if (client != null) {
                    logger.debug("WebCacheParent received an inbound connection from " + client.toString());
                    client.configureBlocking(true);
                    Boolean callstat = ProcessChildSession(client);
                    if (callstat == false) client.close();
                }
            }

            catch (Exception exn) {
                logger.error("Exception accepting cache peer connection", exn);
            }
        }

        logger.debug("WebCacheParent socket thread is terminating");
    }

    /**
     * This function reads child peer requests from Squid, searches for our
     * magic header, and when found, notifies the corresponding client that data
     * is available.
     * 
     * @param client
     *        The socket where we accepted the connection
     * @return False for any error condition, otherwise true
     */
    private Boolean ProcessChildSession(SocketChannel client)
    {
        ByteBuffer rxbuff = ByteBuffer.allocate(8192);
        long myindex = 0;
        int retval = 0;

        try {
            // read the request from squid
            rxbuff.clear();
            retval = client.read(rxbuff);
        }

        catch (Exception exn) {
            logger.error("Exception reading from cache peer socket", exn);
            return (false);
        }

        if (retval < 1) {
            logger.debug("----- PARENT DETECTED CLOSED SOCKET -----");
            return (false);
        }

        String scanner = new String(rxbuff.array(), 0, retval);
        logger.debug("----- PARENT RECEIVED " + scanner.length() + " BYTES FROM SQUID -----\n" + scanner);

        // extract our magic header from the request
        String look = "X-UNTANGLE-WEBCACHE: ";
        int top = scanner.toUpperCase().indexOf(look);
        int end = scanner.indexOf("\r\n", top);

        if ((top < 0) || (end < 0)) {
            logger.debug("----- PARENT MISSING WEBCACHE TAG -----");
            return (false);
        }

        // extract the client session object index from the request header
        myindex = Long.parseLong(scanner.substring(top + look.length(), end));

        logger.debug("----- PARENT PROCESSING INDEX " + myindex + " -----");

        // find the corresponding client session object in the hashtable and bail if missing
        WebCacheSessionInfo sessInfo = SearchSockObject(myindex);

        if (sessInfo == null) {
            logger.debug("----- PARENT MISSING HASHTABLE ENTRY -----");
            return (false);
        }

        if (sessInfo.squidSelector == null) {
            logger.debug("----- PARENT FOUND EMPTY SELECTOR -----");
            return (false);
        }

        // store the squid parent socket channel and wake up the
        // thread that is waiting for a response from squid
        sessInfo.parentChannel = client;
        sessInfo.squidSelector.wakeup();
        return (true);
    }

    /**
     * Called once during app startup to activate our run function
     */
    public void connect()
    {
        int local = UserCount.getAndIncrement();
        logger.debug("WebCacheParent instance " + (local + 1) + " connected");
        if (local != 0) return;

        runner = new Thread(this, "WebCacheParent");
        runner.start();
    }

    /**
     * Called during application startup to terminate our run function
     */
    public void goodbye()
    {
        int local = UserCount.decrementAndGet();
        logger.debug("WebCacheParent instance " + (local + 1) + " disconnected");
        if (local != 0) return;

        try {
            killflag = true;
            server.close();
        }

        catch (Exception exn) {
            logger.warn("Exception closing cache peer socket", exn);
        }
    }

    /**
     * Gets the next cache index identifier
     * 
     * @return A cache index identifier value
     */
    public long GetCacheIndex()
    {
        long local = SessionIndex.getAndIncrement();
        return (local);
    }

    /**
     * Inserts a session info object in our hashtable
     * 
     * @param sessInfo
     *        The object to insert
     */
    public void InsertSockObject(WebCacheSessionInfo sessInfo)
    {
        synchronized (socklist) {
            socklist.put(sessInfo.myIndex, sessInfo);
        }
    }

    /**
     * Removes a session info object from our hashtable
     * 
     * @param sessInfo
     *        The object to remove
     */
    public void RemoveSockObject(WebCacheSessionInfo sessInfo)
    {
        synchronized (socklist) {
            socklist.remove(sessInfo.myIndex);
        }
    }

    /**
     * Searches our hashtable for a session info object
     * 
     * @param argIndex
     *        The index value to search
     * @return The session info object if found, otherwise null
     */
    public WebCacheSessionInfo SearchSockObject(long argIndex)
    {
        WebCacheSessionInfo local;

        synchronized (socklist) {
            local = socklist.get(argIndex);
        }

        return (local);
    }
}
