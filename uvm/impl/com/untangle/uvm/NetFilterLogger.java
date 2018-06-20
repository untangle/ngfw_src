/**
 * $Id: NetFilterLogger.java 40599 2015-06-30 05:20:44Z dmorris $
 */

package com.untangle.uvm;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.UvmContextFactory;

/**
 * This process connects to the untangle-nflogd daemon to log network packets
 * that can't or aren't otherwise logged in some other way or place.
 * 
 * The untangle-nflogd receives raw packets from iptables using the -j NFLOG
 * target. It extracts the relevant details from the packet and creates a simple
 * text format message that is passed to his process on a TCP socket. We receive
 * those message and parse each of the fields, and use them to create a LogEvent
 * that is then written to the database.
 * 
 */
public class NetFilterLogger
{
    private final Logger logger = Logger.getLogger(getClass());

    protected final InetSocketAddress daemonAddress = new InetSocketAddress("127.0.0.1", 1999);
    protected SocketChannel daemonSocket;
    protected SelectionKey daemonKey;
    protected Selector daemonSelector;

    protected ByteBuffer rxbuffer = ByteBuffer.allocate(4096);
    protected boolean socketReset = false;
    private long snoozeTime = 1000;

    private volatile Thread captureThread;
    private NetFilterCapture capture = new NetFilterCapture();

    public static final long SELECT_TIMEOUT = 1000;

    /**
     * Constructor
     */
    protected NetFilterLogger()
    {
        UvmContextFactory.context().newThread(this.capture).start();
    }

    /**
     * The Runnable class for the main thread function
     * 
     */
    private class NetFilterCapture implements Runnable
    {
        /**
         * The main run function
         */
        public void run()
        {
            logger.info("The capture thread is starting.");

            socketStartup();

            captureThread = Thread.currentThread();

            while (captureThread != null) {
                try {

                    // if the reset flag is set we shut down the socket and start it
                    // up again after a brief delay so we don't spin in a tight loop
                    if (socketReset == true) {
                        socketReset = false;
                        Thread.sleep(snoozeTime);
                        snoozeTime += 1000;
                        logger.warn("Attempting to connect to daemon socket");
                        socketDestroy();
                        socketStartup();
                        continue;
                    }

                    // wait until the socket has something for us
                    int keycount = daemonSelector.select(SELECT_TIMEOUT);
                    if (keycount == 0) continue;

                    // walk through the keys and handle any that are ready
                    Set<SelectionKey> selectedKeys = daemonSelector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {

                        SelectionKey key = keyIterator.next();

                        // We connect in non-blocking mode so we get this when the operation has
                        // completed.  Java socket selectors are quiry so we must finish the
                        // connect and then replace OP_CONNECT with OP_READ in the selector.
                        // Setting them both all the time causes select to immediately return zero
                        // after the connection has been established which causes a tight loop. 
                        if (key.isConnectable()) {
                            try {
                                daemonSocket.finishConnect();
                                daemonKey = daemonSocket.register(daemonSelector, SelectionKey.OP_READ);
                                logger.info("The capture thread has connected to the daemon.");
                                snoozeTime = 1000;

                            } catch (ConnectException net) {
                                logger.warn("Timeout connecting to daemon socket");
                                socketReset = true;
                            }
                        }

                        // When we get a message from the daemon we dump it into the database
                        else if (key.isReadable()) {
                            rxbuffer.clear();
                            int size = daemonSocket.read(rxbuffer);
                            if (size == -1) {
                                socketReset = true;
                                logger.warn("Lost connection to daemon socket.");
                            } else HandleLoggerMessage(rxbuffer);
                        }

                        keyIterator.remove();
                    }

                } catch (Exception exn) {
                    // for any exception we set the reset flag to recycle the socket
                    logger.warn("Exception handling capture socket", exn);
                    socketReset = true;
                }
            }

            socketDestroy();
            logger.info("The capture thread is finished.");
        }
    }

    /**
     * Called to open the socket and connect to the netfilter log daemon
     */
    protected void socketStartup()
    {
        try {
            daemonSocket = SocketChannel.open();
            daemonSocket.socket().setTcpNoDelay(true);
            daemonSocket.configureBlocking(false);

            daemonSelector = Selector.open();

            // Since we connect in non-blocking mode we only care about the CONNECT
            // event right now.  Once connected we replace with the OP_READ event.            
            daemonKey = daemonSocket.register(daemonSelector, SelectionKey.OP_CONNECT);

            daemonSocket.connect(daemonAddress);
        }

        catch (Exception exn) {
            logger.error("socketStartup()", exn);
            socketDestroy();
            socketReset = true;
        }
    }

    /**
     * Closes our socket connection to the daemon
     */
    protected void socketDestroy()
    {
        try {
            if (daemonKey != null) daemonKey.cancel();
        } catch (Exception e) {
            logger.error("socketDestroy(daemonKey)", e);
        }
        daemonKey = null;

        try {
            if (daemonSelector != null) daemonSelector.close();
        } catch (Exception e) {
            logger.error("socketDestroy(daemonSelector)", e);
        }
        daemonSelector = null;

        try {
            if (daemonSocket != null) daemonSocket.close();
        } catch (Exception e) {
            logger.error("socketDestroy(daemonSocket)", e);
        }
        daemonSocket = null;
    }

    /**
     * Called to handle messages received from the netfilter log daemon
     * 
     * @param logMessage
     *        The message
     * @throws Exception
     */
    protected void HandleLoggerMessage(ByteBuffer logMessage) throws Exception
    {
        if (!UvmContextFactory.context().networkManager().getNetworkSettings().getLogBlockedSessions()) return;

        String message = new String(logMessage.array(), 0, logMessage.position());
        String srcAddressStr, dstAddressStr;
        String logPrefix;
        int netProto, srcIntf, dstIntf;
        int srcPort, dstPort;
        int icmpType;
        int counter = 0;

        // we may get more than one message so split on end of line markers
        for (String item : message.split("\r\n")) {

            try {
                // extract all of the fields from the message
                netProto = Integer.valueOf(extractField(item, "PROTO:", "0"));
                icmpType = Integer.valueOf(extractField(item, "ICMP:", "999"));
                srcIntf = Integer.valueOf(extractField(item, "SINTF:", "0"));
                srcAddressStr = extractField(item, "SADDR:", "0.0.0.0");
                srcPort = Integer.valueOf(extractField(item, "SPORT:", "0"));
                dstIntf = Integer.valueOf(extractField(item, "DINTF:", "0"));
                dstAddressStr = extractField(item, "DADDR:", "0.0.0.0");
                dstPort = Integer.valueOf(extractField(item, "DPORT:", "0"));
                logPrefix = extractField(item, "PREFIX:", "");

                InetAddress srcAddress = InetAddress.getByName(srcAddressStr);
                InetAddress dstAddress = InetAddress.getByName(dstAddressStr);

                // create a new session event and fill it with our data
                SessionEvent event = new SessionEvent();
                event.setSessionId(com.untangle.jnetcap.Netcap.nextSessionId());
                event.setProtocol(new Short((short) netProto));

                HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(srcAddress);
                String username = null;
                String hostname = null;
                if (entry != null) {
                    username = entry.getUsername();
                    hostname = entry.getHostname();
                }

                if (hostname == null || hostname.length() == 0) {
                    hostname = SessionEvent.determineBestHostname(srcAddress, srcIntf, dstAddress, dstIntf);
                }

                // Since zero is a valid ICMP type, both the untangle-nflogd daemon as well
                // as this code use the value 999 to indicate empty or unknown.  If icmpType
                // is any other value, we set it in the event, otherwise we leave it null.
                if (icmpType != 999) {
                    event.setIcmpType(new Short((short) icmpType));
                }

                event.setUsername(username);
                event.setHostname(hostname);

                event.setCClientAddr(srcAddress);
                event.setSClientAddr(srcAddress);

                event.setCServerAddr(dstAddress);
                event.setSServerAddr(dstAddress);

                event.setSClientPort(new Integer(srcPort));
                event.setCClientPort(new Integer(srcPort));

                event.setSServerPort(new Integer(dstPort));
                event.setCServerPort(new Integer(dstPort));

                event.setClientIntf(new Integer(srcIntf));
                event.setServerIntf(new Integer(dstIntf));

                if (srcIntf != 0 && UvmContextFactory.context().networkManager().isWanInterface(srcIntf)) {
                    event.setLocalAddr(dstAddress);
                    event.setRemoteAddr(srcAddress);
                } else {
                    event.setLocalAddr(srcAddress);
                    event.setRemoteAddr(dstAddress);
                }

                event.setFilterPrefix(logPrefix);

                // log the event
                UvmContextFactory.context().logEvent(event);

            } catch (Exception exn) {
                logger.warn("Unable to parse message: " + item);
            }
        }
    }

    // THIS IS FOR ECLIPSE - @formatter:off

/**
 * The daemon sends netfilter log messages in a format something like this:
 * 
 * |PROTO:17|SINTF:1|SADDR:192.168.222.1|SPORT:137|DINTF:0|DADDR:192.168.222.255|DPORT:137|PREFIX:filter-rules-input|
 *
 * We use the function below to isolate the value of a field and return to the caller
 *  
 */

    // THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Finds and extracts a delimited field value from a buffer
     * 
     * @param buffer
     *        The buffer to search
     * @param findstr
     *        The field to locate
     * @param missing
     *        The default value to return if the field is not found
     * @return The value if found, or the missing argument
     */
    protected String extractField(String buffer, String findstr, String missing)
    {
        int findlen = findstr.length();
        int top = buffer.indexOf(findstr);
        if (top > 0) {
            int len = buffer.substring(top + findlen).indexOf('|');
            if (len > 0) {
                String data = buffer.substring(top + findlen, top + findlen + len);
                return (data);
            }
        }
        return missing;
    }
}
