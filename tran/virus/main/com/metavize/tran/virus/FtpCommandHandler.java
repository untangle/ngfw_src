/*
 * Copyright (c) 2003, 2004, 2005 Metvize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FtpCommandHandler.java,v 1.4 2005/02/08 02:45:03 rbscott Exp $
 */
package com.metavize.tran.virus;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.Interface;
import com.metavize.mvvm.tapi.LiveSubscription;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.MPipeException;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.IPDataResult;
import com.metavize.mvvm.tapi.event.TCPChunkEvent;
import com.metavize.mvvm.tapi.event.TCPChunkResult;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;


public class FtpCommandHandler extends AbstractEventHandler
{
    public static final int CLIENT_TO_SERVER_FILE = 1;
    public static final int SERVER_TO_CLIENT_FILE = 2;

    private static final int NOT_RELATED = 0;
    private static final int RELATED_PASV = 1;
    private static final int RELATED_PORT = 2;

    /* Reply to send to the client when it initiates a disabled session type (eg. EPSV) */
    private static final IPDataResult REPLY_UNRECOGNIZED;
    
    /* Command to send to the server when it initiates a disabled session type (eg. EPSV)  */
    private static final IPDataResult COMMAND_ABORT;

    private static final PipelineFoundry FOUNDRY = MvvmContextFactory.context()
        .pipelineFoundry();

    private static final Logger logger = Logger
        .getLogger(FtpCommandHandler.class);

    private final VirusTransformImpl transform;

    private final Map relatedSessions = new HashMap();

    // constructors -----------------------------------------------------------

    FtpCommandHandler(VirusTransformImpl transform)
    {
        this.transform = transform;
    }

    // EventHandler methods ---------------------------------------------------

    public void handleTCPNewSession(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        FtpSessionState ftpss = new FtpSessionState();
        sess.clientLineBuffering(true);
        sess.serverLineBuffering(true);
        sess.attach(ftpss);
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        ByteBuffer buf  = event.chunk();

        if (buf.get(buf.limit()-1) != '\n') {
            logger.debug("FTP: Incomplete line...");
            return TCPChunkResult.READ_MORE_NO_WRITE;
        }

        AsciiCharBuffer abuf = AsciiCharBuffer.wrap(buf);
        return handleClientCommand(sess,abuf.toString());
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        ByteBuffer buf  = event.chunk();


        if (buf.get(buf.limit()-1) != '\n') {
            logger.debug("Incomplete line...");
            return TCPChunkResult.READ_MORE_NO_WRITE;
        }

        AsciiCharBuffer abuf = AsciiCharBuffer.wrap(buf);
        return handleServerCommand(sess,abuf.toString());
    }

    public void handleTCPClientClosed(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        FtpSessionState ftpss = (FtpSessionState)sess.attachment();

        logger.debug("FTP: CClosed");
        removeRelatedSub(sess);

        if (sess.serverState() != IPSessionDesc.CLOSED) {
            sess.shutdownServer();
        }
    }

    public void handleTCPServerClosed(TCPSessionEvent event)
        throws MPipeException
    {
        TCPSession sess = event.session();
        FtpSessionState ftpss = (FtpSessionState)sess.attachment();

        logger.debug("FTP: SClosed");
        removeRelatedSub(sess);

        if (sess.clientState() != IPSessionDesc.CLOSED) {
            sess.shutdownClient();
        }
    }

    // package protected methods ----------------------------------------------

    int isRelated(TCPSession sess)
    {
        InetSocketAddress srv = new InetSocketAddress(sess.serverAddr(),
                                                      sess.serverPort());
        FtpSessionState ftpss = (FtpSessionState)relatedSessions.get(srv);

        if (ftpss == null) {
            logger.debug("Not FOUND: " + srv);
            return NOT_RELATED;
        }

        /*
         * If the direction isnt set, its because we didnt see a STOR
         * assuming its for a LIST or RETR something, meaning from the
         * ftpserver to the ftpclient
         */
        if (ftpss.relatedPortPasv == RELATED_PASV) {
            if (ftpss.relatedDirection == 0) {
                return SERVER_TO_CLIENT_FILE;
            } else {
                return ftpss.relatedDirection;
            }
        } else if (ftpss.relatedPortPasv == RELATED_PORT) {
            /*
             * PORT means the server becomes the client for the data
             * session so we must retun the opposite
             */
            if (ftpss.relatedDirection == 0) {
                return CLIENT_TO_SERVER_FILE;
            }

            if (ftpss.relatedDirection == CLIENT_TO_SERVER_FILE) {
                return SERVER_TO_CLIENT_FILE;
            } else if (ftpss.relatedDirection == SERVER_TO_CLIENT_FILE) {
                return CLIENT_TO_SERVER_FILE;
            }
        }

        logger.debug("ERROR: Constraint failed: " +
                     ftpss.relatedPortPasv + " : " +
                     ftpss.relatedDirection);
        return NOT_RELATED;
    }

    // private classes --------------------------------------------------------

    private static class FtpSessionState
    {
        /*
         * this stores a live subscription to any related sessions
         * that are created on PASV/PORT commands
         */
        LiveSubscription relatedSub = null;

        /*
         * this stores the direction of dataflow on the related
         * session CLIENT_TO_SERVER_FILE or SERVER_TO_CLIENT_FILE
         *
         * WARNING: the concept of client and server set here are NOT
         * WARNING: necessarily the same as data session's client and server.
         * WARNING: the client here is the ftp client
         */
        int relatedDirection = 0;

        /*
         * this stores whether or not the related session in pasv or port
         * if port, the server is the client in the data session
         * if pasv, the client is the client in the data session
         */
        int relatedPortPasv = 0;

        /* this stores the server of the related data session */
        InetSocketAddress relatedSrv = null;
    }

    // private methods --------------------------------------------------------

    private IPDataResult handleClientCommand(TCPSession sess, String cmd)
    {
        StringTokenizer strtok = new StringTokenizer(cmd);
        String clientCmd;
        String token;
        FtpSessionState ftpss = (FtpSessionState)sess.attachment();

        logger.debug("FTP: C2S: " + cmd);

        if (!strtok.hasMoreTokens())  {
            logger.debug("FTP: Bad Command, no tokens"); /* XXX */
            return IPDataResult.PASS_THROUGH;
        }

        clientCmd = strtok.nextToken();

        if (clientCmd.equalsIgnoreCase("PORT")) {
            if (!strtok.hasMoreTokens())  {
                logger.debug("FTP: Bad Command, no tokens"); /* XXX */
                return IPDataResult.PASS_THROUGH;
            }

            token = strtok.nextToken();
            InetSocketAddress addr = parseIPPortTuple(token);

            if (addr == null) {
                logger.debug("FTP: Invalid PORT tuple:" + token); /* XXX */
                return IPDataResult.PASS_THROUGH;
            }

            /* the server will be the client in the data connection,
             * the specified addr will be the server */
            addSub(sess, sess.serverAddr(),addr);

            ftpss.relatedPortPasv  = RELATED_PORT;
            ftpss.relatedDirection = 0;
        }
        else if (clientCmd.equalsIgnoreCase("PASV")) {
            ftpss.relatedPortPasv = RELATED_PASV;
            ftpss.relatedDirection = 0;
        }
        else if (clientCmd.equalsIgnoreCase("STOR")) {
            ftpss.relatedDirection = CLIENT_TO_SERVER_FILE;
        }
        else if (clientCmd.equalsIgnoreCase("EPSV")) {
            /* XXX Extended Passive Mode is unsupported, send back a rejection code
             * is sent back */
            /* XXX how should ftpss.relatedDirection and ftpss.relatedPortPasv change,
             * I don't think it changes since the request is being dropped. */
            logger.info( "Extended Passive Mode (EPSV) is not supported, return 500" );
            return REPLY_UNRECOGNIZED;
        }

        return IPDataResult.PASS_THROUGH;
    }

    private IPDataResult handleServerCommand(TCPSession sess, String cmd)
    {
        StringTokenizer strtok = new StringTokenizer(cmd," \t\r\n\f-");
        FtpSessionState ftpss = (FtpSessionState)sess.attachment();
        int replyCode;
        String token;

        logger.debug("FTP: S2C: " + cmd);

        if (!strtok.hasMoreTokens())  {
            logger.debug("FTP: Bad Command, no tokens"); /* XXX */
            return IPDataResult.PASS_THROUGH;
        }

        try {
            replyCode = Integer.parseInt(strtok.nextToken());
        } catch (NumberFormatException e) {
            logger.debug("FTP: Invalid reply Code."); /* XXX */
            return IPDataResult.PASS_THROUGH;
        }

        /* new StringTokenizer without "-" in seperators */
        strtok = new StringTokenizer(cmd);

        /*
         * Handle the codes
         */
        switch(replyCode) {

            /* 211 is Expanded Functionalities supported by the server */
        case 211:
            if (!strtok.hasMoreTokens())
                break;
            token = strtok.nextToken();
            if (token.equalsIgnoreCase("211-REST")
                && strtok.hasMoreTokens()
                && strtok.nextToken().equalsIgnoreCase("STREAM")) {
                if (transform.getFtpDisableResume()) {
                    logger.debug("FTP: Disabling Resume");
                    return IPDataResult.DO_NOT_PASS;
                }
            }
            break;

        case 229:
            /* This is unsupported, pass or reject */
            /* XXX ABORT or drop, use drop because ABOR will return a response.
             * The server should NEVER send a 229 because all outgoing EPSV requests
             * are dropped.
             **/
            logger.warn( "Extended Passive Mode(EPSV) is disabled, dropping" );
            return IPDataResult.DO_NOT_PASS;

            /* 227 Entering Passive Mode (1,2,3,4,8,9) */
        case 227:
            strtok.nextToken();
            /* XXX - Arbitray limit, host could send a string longer than twenty words and
             * not violate the RFC
             */
            for ( int i = 0 ; i < 20 ; i++ ) {
                if (!strtok.hasMoreTokens())
                    break;

                token = strtok.nextToken();

                /* Skip over all of the human readable text, and look for the first
                 * instance of (, this marks the start of the IP port tuple 
                 */
                if ( token.startsWith( "(" )) {
                    InetSocketAddress addr = parseIPPortTuple(token);

                    if (addr == null) {
                        logger.debug("FTP: Invalid 227 tuple:" + token); /* XXX */
                        return IPDataResult.PASS_THROUGH;
                    }

                    /* the client will be the client in the data connection,
                     * the specified addr will be the server */
                    addSub(sess, sess.clientAddr(), addr);
                    return IPDataResult.PASS_THROUGH;
                }
            }
            logger.debug("FTP: Invalid 227 reply:" + cmd); /* XXX */
            break;

        default:
            break;
        }
        return IPDataResult.PASS_THROUGH;
    }

    private InetSocketAddress parseIPPortTuple(String tuple)
    {
        if (tuple == null)
            return null;

        StringTokenizer strtok = new StringTokenizer(tuple,",() \t\r\n\f");
        InetAddress iaddr;
        int port = 0;
        byte[] addr = new byte[4];

        for(int i=0;i<4;i++) {

            if (!strtok.hasMoreTokens()) {
                logger.debug("FTP: tuple too short");
                return null;
            }

            try {
                addr[i] = (byte)(Short.parseShort(strtok.nextToken()));
            } catch (NumberFormatException e) {
                logger.debug("FTP: bad byte in addr tuple" + e);
                return null;
            }
        }

        try {
            iaddr = InetAddress.getByAddress(addr);
        } catch (UnknownHostException h) {
            logger.debug("FTP: invalid inet address");
            return null;
        }

        for (int i=0;i<2;i++) {
            if (!strtok.hasMoreTokens()) {
                logger.debug("FTP: tuple too short");
                return null;
            }

            try {
                /* short needed here because java can't do unsigned bytes */
                short sbyte = (Short.parseShort(strtok.nextToken()));

                if (sbyte < 0 || sbyte > 255) {
                    logger.debug("FTP: malformed port tuple number:" + sbyte);
                    return null;
                }

                if (i==0)
                    port += sbyte*256;
                else
                    port += sbyte;

            } catch (NumberFormatException e) {
                logger.debug("FTP: bad byte in port tuple" + e);
                return null;
            }
        }

        return new InetSocketAddress(iaddr,port);
    }

    private void addSub(TCPSession sess, InetAddress cli,
                        InetSocketAddress srv)
    {
        FtpSessionState ftpss = (FtpSessionState)sess.attachment();

        /*
         * remove old, ignore error because it might be gone
         * meaning the session already happened
         */
        removeRelatedSub(sess);

        IPMaddr cliMaddr = new IPMaddr(cli);
        IPMaddr srvMaddr = new IPMaddr(srv.getAddress());
        PortRange srvPort = new PortRange(srv.getPort());

        logger.debug("FTP: Adding PASV/PORT Subscription: " + cli + ":* -> " +
                     srv.getAddress() + ":" + srv.getPort());
        Subscription s = new Subscription
            (Protocol.TCP, Interface.ANY, Interface.ANY, cliMaddr,
             PortRange.ANY, srvMaddr, srvPort);

        LiveSubscription ls = transform.getFtpDataMPipe().addSubscription(s);

        ftpss.relatedSrv = srv;
        ftpss.relatedSub = ls;
        logger.debug("FTP: Added  PASV/PORT MPipe: " + ftpss.relatedSub);

        // XXX doesn't this mean only one connection to server at a time?
        relatedSessions.put(srv, ftpss);
    }

    private void removeRelatedSub(TCPSession sess)
    {
        FtpSessionState ftpss = (FtpSessionState)sess.attachment();

        if (ftpss.relatedSrv != null)
            relatedSessions.remove(ftpss.relatedSrv);
        if (ftpss.relatedSub != null) {
            logger.debug("FTP: Removing PASV/PORT MPipe: "
                         + ftpss.relatedSub);
            transform.getFtpDataMPipe().removeSubscription(ftpss.relatedSub);
        }
    }

    static {
        /* A response for the client when there is an unrecognized response */
        ByteBuffer[] unrecognized = new ByteBuffer[]  {
            ByteBuffer.wrap( "500 Syntax error, Command unrecognized\r\n".getBytes())
        };
        
        ByteBuffer[] abort = new ByteBuffer[] {
            ByteBuffer.wrap( "ABOR\r\n".getBytes())
        };
        
        REPLY_UNRECOGNIZED = new TCPChunkResult( unrecognized, null, null );
        COMMAND_ABORT = new TCPChunkResult( null, abort, null );
    }
}


