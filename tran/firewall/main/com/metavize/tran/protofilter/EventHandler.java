/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EventHandler.java,v 1.7 2005/03/15 02:11:52 amread Exp $
 */

package com.metavize.tran.protofilter;

import java.nio.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(EventHandler.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private ArrayList  _patternList = null;
    private int        _bufferSize = 4096;
    private int        _byteLimit  = 2048;
    private int        _chunkLimit = 8;
    private String     _unknownString  = "unknown";
    private boolean    _stripZeros = false;

    private class SessionInfo {

        public byte[] serverBuffer;
        public byte[] clientBuffer;

        public int serverBufferSize;
        public int clientBufferSize;

        public int serverChunkCount;
        public int clientChunkCount;

        public String protocol;
        public boolean identified;

        private Pipeline pipeline;
    }

    public EventHandler() {}

    public void handleTCPNewSession (TCPSessionEvent event)
    {
        TCPSession sess = event.session();

        SessionInfo sessInfo = new SessionInfo();
        sessInfo.clientBuffer = new byte[this._bufferSize];
        sessInfo.serverBuffer = new byte[this._bufferSize];
        sessInfo.pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(sess.id());
        sess.attach(sessInfo);
    }

    public void handleUDPNewSession (UDPSessionEvent event)
    {
        UDPSession sess = event.session();

        SessionInfo sessInfo = new SessionInfo();
        sessInfo.clientBuffer = new byte[this._bufferSize];
        sessInfo.serverBuffer = new byte[this._bufferSize];
        sessInfo.pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(sess.id());
        sess.attach(sessInfo);
    }

    public IPDataResult handleTCPClientChunk (TCPChunkEvent e)
    {
        _handleChunk(e, e.session(), false);
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleTCPServerChunk (TCPChunkEvent e)
    {
        _handleChunk(e, e.session(), true);
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleUDPClientPacket (UDPPacketEvent e)
    {
        _handleChunk(e, e.session(), false);
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleUDPServerPacket (UDPPacketEvent e)
    {
        _handleChunk(e, e.session(), true);
        return IPDataResult.PASS_THROUGH;
    }

    public void handleTCPFinalized(TCPChunkEvent event)
    {
        /* XXX */
    }

    public void patternList (ArrayList patternList)
    {
        _patternList = patternList;
    }

    public void bufferSize (int bufferSize)
    {
        _bufferSize = bufferSize;
    }

    public void chunkLimit (int chunkLimit)
    {
        _chunkLimit = chunkLimit;
    }

    public void byteLimit (int byteLimit)
    {
        _byteLimit  = byteLimit;
    }

    public void stripZeros (boolean stripZeros)
    {
        _stripZeros = stripZeros;
    }

    public void unknownString (String unknownString)
    {
        _unknownString  = unknownString;
    }



    private void _handleChunk (IPDataEvent event, IPSession sess, boolean server)
    {
        ByteBuffer buffer = event.data();
        SessionInfo sessInfo = (SessionInfo)sess.attachment();

        if (sessInfo.identified == true)
            return;

        int bufferSize = server ? sessInfo.serverBufferSize: sessInfo.clientBufferSize;
        if (bufferSize >= this._byteLimit)
            return;

        int bytesToWrite = (buffer.remaining() > (this._byteLimit - bufferSize) ?
                            this._byteLimit - bufferSize : buffer.remaining());
        byte[] buf;
        int chunkCount;
        int written = 0;

        /**
         * grab the buffer
         */
        if (server) {
            buf = sessInfo.serverBuffer;
            chunkCount = sessInfo.serverChunkCount;
        }
        else {
            buf = sessInfo.clientBuffer;
            chunkCount = sessInfo.clientChunkCount;
        }

        /**
         * copy the data into buf, possibly stripping zeros
         */
        for (int i=0;i<bytesToWrite;i++){
            byte b = buffer.get();
            if ((!_stripZeros) || (b != 0x00)) {
                buf[bufferSize+written] = b;
                written++;
            }
        }
        bufferSize += written;
        chunkCount++;

        /**
         * update the buffer metadata
         */
        if (server) {
            sessInfo.serverBuffer = buf;
            sessInfo.serverBufferSize = bufferSize;
            sessInfo.serverChunkCount = chunkCount;
        } else {
            sessInfo.clientBuffer = buf;
            sessInfo.clientBufferSize = bufferSize;
            sessInfo.clientChunkCount = chunkCount;
        }

        ProtoFilterPattern elem = _findMatch(sessInfo, sess, server);
        incrementCount(Transform.GENERIC_0_COUNTER); // SCAN COUNTER
        if (elem != null) {
            sessInfo.protocol = elem.getProtocol();
            sessInfo.identified = true;
            String l4prot = "";
            if (sess instanceof TCPSession)
                l4prot = "TCP";
            if (sess instanceof UDPSession)
                l4prot = "UDP";

            if (logger.isDebugEnabled()) {
                logger.debug(" ----------------LOG: " + sessInfo.protocol + " traffic----------------");
                logger.debug( l4prot + ": " + sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                              sess.serverAddr().getHostAddress() + ":" + sess.serverPort() + " matched " + sessInfo.protocol);
                logger.debug(" ----------------LOG: "+ sessInfo.protocol + " traffic----------------");
            }

            incrementCount(Transform.GENERIC_1_COUNTER); // DETECT COUNTER

            if(elem.getAlert()) {
                /* XXX Do alert here */
            }

            if (elem.isBlocked() == true) {
                incrementCount(Transform.GENERIC_2_COUNTER); // BLOCK COUNTER

                if (logger.isDebugEnabled()) {
                    logger.debug(" ----------------BLOCKED: " + sessInfo.protocol + " traffic----------------");
                    logger.debug( l4prot + ": " + sess.clientAddr().getHostAddress() + ":" + sess.clientPort() + " -> " +
                                  sess.serverAddr().getHostAddress() + ":" + sess.serverPort() + " matched " + sessInfo.protocol);
                    logger.debug(" ----------------BLOCKED: "+ sessInfo.protocol + " traffic----------------");
                }

                if (sess instanceof TCPSession) {
                    ((TCPSession)sess).resetClient();
                    ((TCPSession)sess).resetServer();
                }
                else if (sess instanceof UDPSession) {
                    ((UDPSession)sess).expireClient(); /* XXX correct? */
                    ((UDPSession)sess).expireServer(); /* XXX correct? */
                }
            }

            ProtoFilterLogEvent evt = new ProtoFilterLogEvent
                (sess.id(), sessInfo.protocol, elem.isBlocked());
            eventLogger.info(evt);

        } else if (bufferSize >= this._byteLimit || (sessInfo.clientChunkCount+sessInfo.serverChunkCount) >= this._chunkLimit) {
            sessInfo.protocol = this._unknownString;
            sessInfo.identified = true;
            sess.release();
        }
    }

    private ProtoFilterPattern _findMatch (SessionInfo sessInfo, IPSession sess, boolean server)
    {
        /**
         * FIXME - creates a copy
         */
        String buffer = server ? new String(sessInfo.serverBuffer) : new String(sessInfo.clientBuffer);

        for (int i = 0; i < _patternList.size(); i++) {
            ProtoFilterPattern elem = (ProtoFilterPattern)_patternList.get(i);
            Pattern pat = PatternFactory.createRegExPattern(elem.getDefinition());
            if (pat.matcher(buffer).find())
                return elem; /* XXX - can match multiple patterns */
        }

        return null;
    }

}
