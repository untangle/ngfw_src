/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl.pop;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.node.mail.PopCasing;
import com.untangle.node.mail.papi.ByteBufferByteStuffer;
import com.untangle.node.mail.papi.DoNotCareChunkT;
import com.untangle.node.mail.papi.DoNotCareT;
import com.untangle.node.mail.papi.MIMEMessageT;
import com.untangle.node.mail.papi.MIMEMessageTrickleT;
import com.untangle.node.mail.papi.pop.PopCommand;
import com.untangle.node.mail.papi.pop.PopCommandMore;
import com.untangle.node.mail.papi.pop.PopReply;
import com.untangle.node.mail.papi.pop.PopReplyMore;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseException;
import com.untangle.node.token.UnparseResult;
import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;

public class PopUnparser extends AbstractUnparser
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String ENCODING = System.getProperty("file.encoding");
    private final static Charset CHARSET = Charset.forName(ENCODING);

    private final PopCasing zCasing;

    private PopReply zMsgDataReply;
    private ByteBufferByteStuffer zByteStuffer;

    private TempFileFactory zTempFactory;

    public PopUnparser(NodeTCPSession session, boolean clientSide, PopCasing zCasing)
    {
        super(session, clientSide);
        this.zCasing = zCasing;

        zMsgDataReply = null;
        zByteStuffer = null;
        zTempFactory = new TempFileFactory(UvmContextFactory.context().pipelineFoundry()
                                           .getPipeline(session.id()));
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        logger.debug("unparser got: " + token.getClass());

        PopStreamer zPopStreamer;

        if (token instanceof MIMEMessageT) { /* complete message */
            zPopStreamer = writeData((MIMEMessageT) token);
            return new UnparseResult(zPopStreamer);
        } else if (token instanceof MIMEMessageTrickleT) { /* trickle start */
            zPopStreamer = writeData((MIMEMessageTrickleT) token);
            return new UnparseResult(zPopStreamer);
        }
        /* else individually write out buffers as they arrive */

        List<ByteBuffer> zWriteBufs = new LinkedList<ByteBuffer>();

        if (token instanceof PopCommand) {
            PopCommand zCommand = (PopCommand) token;

            if (null == zCasing.getUser() &&
                true == zCommand.isUser()) {
                /* workaround to supply user to server parser */
                zCasing.setUser(zCommand.getUser());
            } else if (true == zCommand.isRETR()) {
                /* workaround to supply client's RETR command to server parser */
                zCasing.setIncomingMsg(true);
            } else if (true == zCommand.isTOP()) {
                /* workaround to supply client's TOP command to server parser */
                zCasing.setIncomingMsgHdr(true);
            }

            zWriteBufs.add(zCommand.getBytes());
        } else if (token instanceof PopCommandMore) {
            PopCommandMore zCommandMore = (PopCommandMore) token;

            if (null == zCasing.getUser() &&
                true == zCommandMore.isUser()) {
                /* workaround to supply user to server parser */
                zCasing.setUser(zCommandMore.getUser());
            }

            zWriteBufs.add(zCommandMore.getBytes());
        } else if (token instanceof PopReply) {
            PopReply zReply = (PopReply) token;

            if (null == zMsgDataReply &&
                (true == zReply.isMsgData() ||
                 true == zReply.isMsgHdrData())) {
                zMsgDataReply = zReply; /* save for later */
                zByteStuffer = new ByteBufferByteStuffer();
                return UnparseResult.NONE;
            } else if (null != zMsgDataReply) {
                /* something is wrong with parser
                 * - it sent 2nd message before terminating 1st message
                 * - dump everything
                 */
                logger.error("new message has been received before previous message has been sent; trying to send new message");

                zMsgDataReply = zReply; /* save for later */
                zByteStuffer = new ByteBufferByteStuffer();
                return UnparseResult.NONE;
            } else { /* non-message reply */
                zWriteBufs.add(zReply.getBytes());
            }
        } else if (token instanceof PopReplyMore) {
            zWriteBufs.add(token.getBytes());
        } else if (token instanceof Chunk) { /* trickle continue */
            writeData((Chunk) token, zWriteBufs);
        } else if (token instanceof EndMarker) { /* trickle end */
            writeEOD(zWriteBufs);
        } else if (token instanceof DoNotCareT) { /* do not care */
            writeData((DoNotCareT) token, zWriteBufs);
        } else if (token instanceof DoNotCareChunkT) { /* do not care */
            writeData((DoNotCareChunkT) token, zWriteBufs);
        } else { /* unknown/unsupported */
            logger.error("cannot handle token: " + token.getClass());
            return UnparseResult.NONE;
        }

        return new UnparseResult(zWriteBufs.toArray(new ByteBuffer[zWriteBufs.size()]));
    }

    public TCPStreamer endSession()
    {
        logger.debug("(pop)(" + (true == isClientSide() ? "client":"server") + ") End Session");

        return null;
    }

    private PopStreamer writeData(MIMEMessageT zMMessageT, boolean bIsComplete) throws UnparseException
    {
        /* we'll insert message reply to start of list later
         * (after we determine if message has been modified)
         */

        File zOrgMsgFile = zMMessageT.getFile();
        File zNewMsgFile;
        try {
            zNewMsgFile = zTempFactory.createFile("bs");
            //logger.debug("created byte stuffed message file: " + zNewMsgFile);
        } catch (IOException exn) {
            /* cannot recover if byte stuffed mesasge file cannot be created */
            throw new UnparseException("cannot create byte stuffed message file: " + exn);
        }

        /* note that MIMEMessage dispose will delete byte unstuffed file and
         * PopStreamer closeWhenDone will delete byte stuffed file
         */
        PopStreamer zPopStreamer = PopStreamer.stuffFile(zOrgMsgFile, zNewMsgFile, zByteStuffer, bIsComplete);
        if (null == zPopStreamer) {
            /* cannot recover if byte stuffed message file cannot be updated */
            throw new UnparseException("cannot create streamer for byte stuffed message file");
        }

        if (false == bIsComplete) {
            /* if trickling,
             * we assume that message has not been modified and
             * original message size is unchanged
             */
            zPopStreamer.prepend(zMsgDataReply.getBytes());

            /* remaining data will trickle as chunks and
             * terminate with end marker
             * - these will be individually written and not streamed
             */
        } else {
            int iNewMsgDataSz = zPopStreamer.getSize();
            zPopStreamer.prepend(updateMsgDataSz(iNewMsgDataSz));
            reset();
        }

        MIMEMessage zMMessage = zMMessageT.getMIMEMessage();
        if (null != zMMessage) {
            zMMessage.dispose();
        } else {
            //XXXX trickling - need to dispose message header???
            zOrgMsgFile.delete();
        }

        return zPopStreamer;
    }

    private PopStreamer writeData(MIMEMessageT zMMessageT) throws UnparseException
    {
        boolean bIsComplete;

        if (null == zMMessageT.getMIMEMessageHeader() &&
            null != zMMessageT.getMIMEMessage()) {
            /* message has been re-assembled */
            bIsComplete = true;
        } else {
            /* message has not been re-assembled yet;
             * more chunks and marker have yet to arrive
             */
            bIsComplete = false;
        }

        return writeData(zMMessageT, bIsComplete);
    }

    private PopStreamer writeData(MIMEMessageTrickleT zMMTrickleT) throws UnparseException
    {
        return writeData(zMMTrickleT.getMMessageT());
    }

    private void writeData(Chunk zChunk, List<ByteBuffer> zWriteBufs)
    {
        ByteBuffer zReadBuf = zChunk.getBytes();
        ByteBuffer zWriteBuf = ByteBuffer.allocate(zReadBuf.remaining());
        zByteStuffer.transfer(zReadBuf, zWriteBuf);
        zWriteBufs.add(zWriteBuf);

        /* getLast returns any remaining data later */
        return;
    }

    private void writeData(DoNotCareT zDoNotCareT, List<ByteBuffer> zWriteBufs)
    {
        if (null != zMsgDataReply) {
            zWriteBufs.add(zMsgDataReply.getBytes());
        }

        /* since do-not-care data are not byte unstuffed,
         * it does not need to be byte stuffed
         */
        zWriteBufs.add(zDoNotCareT.getBytes());
        reset();

        return;
    }

    private void writeData(DoNotCareChunkT zDoNotCareChunkT, List<ByteBuffer> zWriteBufs)
    {
        /* since do-not-care data are not byte unstuffed,
         * it does not need to be byte stuffed
         */
        zWriteBufs.add(zDoNotCareChunkT.getBytes());

        return;
    }

    private void writeEOD(List<ByteBuffer> zWriteBufs)
    {
        ByteBuffer zWriteBuf = zByteStuffer.getLast(true);
        zWriteBufs.add(zWriteBuf);
        reset();

        return;
    }

    private ByteBuffer updateMsgDataSz(int iNewMsgDataSz)
    {
        ByteBuffer zNewBuf = zMsgDataReply.getBytes();

        String zMsgDataSz = zMsgDataReply.getMsgDataSz();
        if (null == zMsgDataSz) {
            // original +OK reply didn't include octet count so leave as is
            zMsgDataReply = null;
            return zNewBuf;
        }

        // original +OK reply included octet count so update if necessary
        int iMsgDataSz = Integer.valueOf(zMsgDataSz).intValue();
        if (iMsgDataSz != iNewMsgDataSz) {
            /* rebuild retrieve-ok reply because message size has changed */

            //logger.debug("orig size: " + iMsgDataSz + ", new size: " + iNewMsgDataSz);

            Pattern zMsgDataSzP = Pattern.compile(zMsgDataSz);

            String zDataOK = AsciiCharBuffer.wrap(zNewBuf).toString();
            Matcher zMatcher = zMsgDataSzP.matcher(zDataOK);

            String zNewDataOK = zMatcher.replaceAll(String.valueOf(iNewMsgDataSz));

            try {
                zNewBuf = toByteBuffer(zNewDataOK);
                zNewBuf.rewind();
            } catch (CharacterCodingException exn) {
                /* may not recover if new message reply cannot be formed */
                logger.error("Unable to encode new message reply line: " + zNewDataOK + ", reusing org reply line as last resort: " + zMsgDataReply + ": ", exn);
                zNewBuf = zMsgDataReply.getBytes();
            }
        }

        zMsgDataReply = null;
        return zNewBuf;
    }

    private ByteBuffer toByteBuffer(String zStr) throws CharacterCodingException
    {
        ByteBuffer zLine = CHARSET.newEncoder().encode(CharBuffer.wrap(zStr));
        zLine.position(zLine.limit()); /* set position because ByteBuffer contains data */

        return zLine;
    }

    private void reset()
    {
        zByteStuffer = null;
        zMsgDataReply = null;
        return;
    }
}
