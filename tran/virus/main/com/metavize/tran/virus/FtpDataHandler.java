/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.virus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.FileStreamer;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.MPipeException;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.IPDataResult;
import com.metavize.mvvm.tapi.event.TCPChunkEvent;
import com.metavize.mvvm.tapi.event.TCPChunkResult;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;


public class FtpDataHandler extends AbstractEventHandler
{
    public static final int CLIENT_TO_SERVER_FILE = 1;
    public static final int SERVER_TO_CLIENT_FILE = 2;

    // When true, we reset (RST) rather than shutdown (FIN)
    public static boolean ResetOnVirus = false;

    private static final Logger logger = Logger.getLogger(FtpDataHandler.class);

    private final VirusTransformImpl transform;
    private final Logger eventLogger;

    // constructors -----------------------------------------------------------

    public FtpDataHandler(VirusTransformImpl transform)
    {
        this.transform = transform;

        eventLogger = MvvmContextFactory.context().eventLogger();
    }

    // EventHandler methods ---------------------------------------------------

    public void handleTCPNewSession(TCPSessionEvent event)
    {
        logger.debug("handling new FTP data stream");

        TCPSession sess = event.session();
        String fileName = makeFileName(sess);
        VirusSessionState vss = new VirusSessionState();
        logger.debug("ATTCHING TO : " + sess);
        sess.attach(vss);

        if (logger.isDebugEnabled()) {
            logger.debug("VIRUS: New     TCP Session " +
                         sess.clientAddr().getHostAddress()
                         + ":" + sess.clientPort() + " -> "
                         + sess.serverAddr().getHostAddress()
                         + ":" + sess.serverPort());

            logger.debug("VIRUS: Flushing to file: " + fileName);
        }

        int direction = transform.getFtpCommandHandler()
            .isRelated(event.session());
        if (logger.isDebugEnabled())
            logger.debug("got direction: " + direction);

        try {
            File fileBuf = File.createTempFile(fileName,null);
            fileBuf.deleteOnExit(); /* just in case */
            if (logger.isDebugEnabled())
                logger.debug("created file: " + fileBuf);

            vss.fileName = fileBuf.getAbsolutePath();
            vss.outFile = (new FileOutputStream(fileBuf)).getChannel();
            vss.inFile = (new FileInputStream(fileBuf)).getChannel();
            vss.file = fileBuf;

            if (direction != SERVER_TO_CLIENT_FILE
                && direction != CLIENT_TO_SERVER_FILE) {
                logger.debug("VIRUS: Invalid direction, Assuming C2S");
                vss.direction = CLIENT_TO_SERVER_FILE;
            } else {
                vss.direction = direction;
            }
        } catch (IOException e) {
            logger.error("Unable to create file: " + fileName + "\n" + e);
        }
    }

    public void handleTCPClientFIN(TCPSessionEvent event) throws MPipeException
    {
        VirusSessionState vss = (VirusSessionState)event.session()
            .attachment();
        if (vss.direction == CLIENT_TO_SERVER_FILE) {
            logger.debug("handleTCPClientFIN()");
            handleTCPShutdown(event);
        } else {
            // Unexpected from the other direction
            logger.warn("Unexpected client FIN for ftp data");
            super.handleTCPClientFIN(event);
        }
    }

    public void handleTCPServerFIN(TCPSessionEvent event) throws MPipeException
    {
        VirusSessionState vss = (VirusSessionState)event.session()
            .attachment();
        if (vss.direction == SERVER_TO_CLIENT_FILE) {
            logger.debug("handleTCPServerFIN()");
            handleTCPShutdown(event);
        } else {
            // Unexpected from the other direction
            logger.warn("Unexpected server FIN for ftp data");
            super.handleTCPServerFIN(event);
        }
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
    {
        logger.debug("handleTCPClientChunk()");
        logger.debug("DETACH: " + event.session());
        VirusSessionState vss = (VirusSessionState)event.session()
            .attachment();

        if (vss.direction == CLIENT_TO_SERVER_FILE) {
            logger.debug("trickling client");
            return handleTCPChunkTrickle(event);
        } else {
            if (logger.isDebugEnabled())
                logger.debug("VIRUS: Passing through:\n"
                             + AsciiCharBuffer.wrap(event.chunk()));
            return IPDataResult.PASS_THROUGH;
        }
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
    {
        logger.debug("handleTCPServerChunk()");
        VirusSessionState vss = (VirusSessionState)event.session()
            .attachment();

        if (vss.direction == SERVER_TO_CLIENT_FILE) {
            logger.debug("trickling server");
            return handleTCPChunkTrickle(event);
        } else {
            if (logger.isDebugEnabled())
                logger.debug("VIRUS: Passing through:\n"
                             + AsciiCharBuffer.wrap(event.chunk()));
            return IPDataResult.PASS_THROUGH;
        }
    }

    public void handleTCPFinalized(TCPChunkEvent event)
    {
        logger.debug("handleTCPFinalized()");
        TCPSession sess = (TCPSession)event.session();
        VirusSessionState vss = (VirusSessionState)sess.attachment();

        try {
            vss.outFile.close();
            vss.inFile.close();
            vss.file.delete();
        } catch (IOException e) {
            logger.error("VIRUS Unable to cleanup session: " + e);
        }
    }

    // private classes --------------------------------------------------------

    private static class VirusSessionState {
        File file;
        FileChannel outFile;
        FileChannel inFile;
        String fileName;
        int direction;
    }

    // private methods --------------------------------------------------------

    /**
     * Writes the entire chunk to the file
     * Trickles one byte from the file
     */
    private IPDataResult handleTCPChunkTrickle(TCPChunkEvent event)
    {
        logger.debug("handling trickle");
        TCPSession sess = event.session();
        ByteBuffer buff = event.chunk();
        VirusSessionState vss = (VirusSessionState)sess.attachment();
        int trickleLen = (buff.remaining() * transform.getTricklePercent())
            / 100;
        ByteBuffer inbuf = ByteBuffer.allocate(trickleLen);
        ByteBuffer[] bufs = new ByteBuffer[1];
        bufs[0] = inbuf;

        try {
            ByteBuffer bb = buff.duplicate();
            while (bb.remaining()>0)
                vss.outFile.write(bb);
            vss.outFile.force(false); // Ensure that inFile has the right bytes.
        }
        catch (IOException e) {
            logger.error("Unable to write to buffer file: " + e);
            return IPDataResult.PASS_THROUGH;
        }

        inbuf.limit(trickleLen);

        try {
            logger.debug( "trickle - before at position: " + vss.inFile.position());
            if (vss.inFile.read(inbuf)<trickleLen)
                logger.error("Read unsuccessful/truncated");
            logger.debug( "trickle - after at position: " + vss.inFile.position());
        }
        catch (IOException e) {
            logger.error("Unable to read from buffer file: " + e);
            return IPDataResult.DO_NOT_PASS;
        }

        inbuf.flip();

        if (vss.direction == CLIENT_TO_SERVER_FILE) {
            /* pass the one byte - trickle */
            return new TCPChunkResult(null, bufs, null);
        } else {
            /* pass the one byte - trickle */
            return new TCPChunkResult(bufs,null,null);
        }
    }

    /**
     * Starts the SEM task to flush the rest of the file
     */
    private void handleTCPShutdown(TCPSessionEvent event)
    {
        TCPSession sess = event.session();
        VirusSessionState vss = (VirusSessionState)sess.attachment();
        int direction = vss.direction;
        VirusScannerResult result;

        logger.debug("VIRUS: Shutdown TCP Session "
                     + sess.clientAddr().getHostAddress()
                     + ":" + sess.clientPort() + " -> "
                     + sess.serverAddr().getHostAddress()
                     + ":" + sess.serverPort());

        if (direction == CLIENT_TO_SERVER_FILE) {
            logger.debug("shutting down client");
            event.session().shutdownClient();
            if (sess.serverState() == IPSessionDesc.CLOSED) {
                return;
            }
        } else {
            logger.debug("shutting down server");
            event.session().shutdownServer();
            if (sess.clientState() == IPSessionDesc.CLOSED) {
                return;
            }
        }

        transform.incrementCount(Transform.GENERIC_0_COUNTER, 1); // SCAN
        try {
            result = transform.getScanner().scanFile(vss.fileName);
        } catch (IOException e) {
            logger.error("Virus scan failed: " + e);
            result = VirusScannerResult.ERROR;
        } catch (InterruptedException e) {
            logger.error("Virus scan failed: " + e);
            result = VirusScannerResult.ERROR;
        }
        if (result == null) {
            logger.error("Virus scan failed: null"); 
            result = VirusScannerResult.ERROR;
        }

        eventLogger.info(new VirusLogEvent(sess.id(), result));

        if (result.isVirusCleaned()) {
            logger.info("VIRUS: Cleaned file: Starting C2S Flush task...");
            // BLOCK/FOUND
            transform.incrementCount(Transform.GENERIC_1_COUNTER, 1);

            if (CLIENT_TO_SERVER_FILE == direction) {
                logger.debug("begin server stream");
                sess.beginServerStream(new FileStreamer(vss.inFile, true));
            } else {
                logger.debug("begin client stream");
                sess.beginClientStream(new FileStreamer(vss.inFile, true));
            }
        } else if (result.isClean()) {
            logger.info("VIRUS: Clean, Starting Flush task...");
            // PASS/CLEAN
            transform.incrementCount(Transform.GENERIC_2_COUNTER, 1);

            if (CLIENT_TO_SERVER_FILE == direction) {
                logger.debug("begin server stream");
                sess.beginServerStream(new FileStreamer(vss.inFile, true));
            } else {
                logger.debug("begin client stream");
                sess.beginClientStream(new FileStreamer(vss.inFile, true));
            }
        } else { /* infected */
            logger.info("VIRUS: Found virus! Killing Session!");
            // BLOCK/FOUND
            transform.incrementCount(Transform.GENERIC_1_COUNTER, 1);
            if (sess.serverState() != IPSessionDesc.CLOSED) {
                if (ResetOnVirus)
                    event.session().resetServer();
                else
                    event.session().shutdownServer();
            }

            if (sess.clientState() != IPSessionDesc.CLOSED) {
                if (ResetOnVirus)
                    event.session().resetClient();
                else
                    event.session().shutdownClient();
            }
        }
    }

    /**
     * Creates a temporary filename
     */
    private String makeFileName(TCPSession sess)
    {
        return "filebuf-" + sess.clientAddr().getHostAddress().toString()
            + ":" + Integer.toString(sess.clientPort()) + "-"
            + sess.serverAddr().getHostAddress().toString() + ":"
            + Integer.toString(sess.serverPort()) + ".";
    }
}
