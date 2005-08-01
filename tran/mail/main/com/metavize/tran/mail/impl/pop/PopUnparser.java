/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.pop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mail.papi.ByteBufferByteStuffer;
import com.metavize.tran.mail.papi.pop.PopCommand;
import com.metavize.tran.mail.papi.pop.PopReply;
import com.metavize.tran.mail.papi.MIMEMessageT;
import com.metavize.tran.mail.papi.MIMEMessageTrickleT;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.UnparseException;
import com.metavize.tran.token.UnparseResult;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

public class PopUnparser extends AbstractUnparser
{
    private final static Logger logger = Logger.getLogger(PopUnparser.class);

    private final static String ENCODING = System.getProperty("file.encoding");
    private final static Charset CHARSET = Charset.forName(ENCODING);

    private final static int DATA_SZ = 4192;

    private PopReply zMsgDataReply;
    private ByteBufferByteStuffer zByteStuffer;

    public PopUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);

        zMsgDataReply = null;
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        logger.debug("unparser got: " + token.getClass());

        List<ByteBuffer> zWriteBufs = new LinkedList<ByteBuffer>();

        if (token instanceof PopCommand) {
            zWriteBufs.add(token.getBytes());
        } else if (token instanceof PopReply) {
            PopReply zReply = (PopReply) token;

            if (null == zMsgDataReply &&
                true == zReply.isMsgData()) {
                zMsgDataReply = zReply;
                zByteStuffer = new ByteBufferByteStuffer();
            } else if (null != zMsgDataReply) {
                /* something is wrong with parser
                 * - it sent 2nd message before terminating 1st message
                 * - dump everything
                 */
                logger.error("new message has been received but previous message may be incomplete; sending new message");

                zMsgDataReply = zReply;
                zByteStuffer = new ByteBufferByteStuffer();
            } else { /* non-message reply */
                zWriteBufs.add(zReply.getBytes());
            }
        } else if (token instanceof MIMEMessageT) {
            writeData((MIMEMessageT) token, zWriteBufs, true);
            zByteStuffer = null;
            zMsgDataReply = null;
        } else if (token instanceof Chunk) { /* trickle continue */
            writeData((Chunk) token, zWriteBufs);
        } else if (token instanceof MIMEMessageTrickleT) { /* trickle start */
            writeData((MIMEMessageTrickleT) token, zWriteBufs);
        } else if (token instanceof EndMarker) { /* trickle end */
            writeEOD(zWriteBufs);
            zByteStuffer = null;
            zMsgDataReply = null;
        } else { /* unknown/unsupported */
            logger.error("cannot handle token: " + token.getClass());
            return UnparseResult.NONE;
        }

        return new UnparseResult(zWriteBufs.toArray(new ByteBuffer[zWriteBufs.size()]));
    }

    private void writeData(MIMEMessageT zMMessageT, List<ByteBuffer> zWriteBufs, boolean bIsComplete)
    {
        /* we'll insert message reply to start of list later
         * (after we determine if message has been modified)
         *
         * XXXX change later
         *      (to obtain count of and to stream byte stuffed data
         *       from file and
         *       to avoid reading byte stuffed data into memory)
         */

        File zMsgFile = zMMessageT.getFile();

        try {
            FileChannel zMsgChannel = new FileInputStream(zMsgFile).getChannel();
            long lMsgFileSz = zMsgFile.length();
            int iDataSz = (int) ((DATA_SZ < lMsgFileSz) ? DATA_SZ : lMsgFileSz);
            ByteBuffer zReadBuf = ByteBuffer.allocate(iDataSz);
            int iNewMsgDataSz = 0;

            ByteBuffer zWriteBuf;

            try {
                while (0 < zMsgChannel.read(zReadBuf)) {
                    zReadBuf.flip();
                    zWriteBuf = ByteBuffer.allocate(iDataSz);
                    zByteStuffer.transfer(zReadBuf, zWriteBuf);
                    iNewMsgDataSz += zWriteBuf.remaining();
                    zWriteBufs.add(zWriteBuf);

                    zReadBuf.clear();
                }

                zWriteBuf = zByteStuffer.getLast(bIsComplete);
                iNewMsgDataSz += zWriteBuf.remaining();
                zWriteBufs.add(zWriteBuf);

                if (false == bIsComplete) {
                    /* if trickling,
                     * we assume that message has not been modified and
                     * original message size is unchanged
                     */
                    zWriteBufs.add(0, zMsgDataReply.getBytes());

                    /* remaining data will trickle as chunks and
                     * terminate with end marker
                     */
                } else {
                    zWriteBufs.add(0, updateMsgDataSz(iNewMsgDataSz));
                }
            } catch (IOException exn2) {
                zWriteBufs.clear();
                logger.warn("cannot read data from message file: ", exn2);
            } finally {
                closeMsgChannel(zMsgChannel);
            }
        } catch (FileNotFoundException exn) {
            zWriteBufs.clear();
            logger.warn("cannot access message file: ", exn);
        } finally {
            MIMEMessage zMMessage = zMMessageT.getMIMEMessage();
            if (null != zMMessage) {
                zMMessage.dispose();
            } else {
                //XXXX trickling but still need to dispose message header???
                zMsgFile.delete();
            }
            zMsgFile = null;
        }

        return;
    }

    private void writeData(MIMEMessageTrickleT zMMTrickleT, List<ByteBuffer> zWriteBufs)
    {
        writeData(zMMTrickleT.getMMessageT(), zWriteBufs, false);
        return;
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

    private void writeEOD(List<ByteBuffer> zWriteBufs)
    {
        ByteBuffer zWriteBuf = zByteStuffer.getLast(true);
        zWriteBufs.add(zWriteBuf);

        return;
    }

    private ByteBuffer updateMsgDataSz(int iNewMsgDataSz)
    {
        ByteBuffer zNewBuf = zMsgDataReply.getBytes();

        String zMsgDataSz = zMsgDataReply.getMsgDataSz();
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
                logger.error("Unable to encode line: " + zNewDataOK + " : " + exn);
                zNewBuf = null;
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

    private void closeMsgChannel(FileChannel zMsgChannel)
    {
        try {
            zMsgChannel.close();
        } catch (IOException exn) {
            logger.warn("cannot close message file: ", exn);
        } finally {
            zMsgChannel = null;
        }

        return;
    }

    public TCPStreamer endSession() { return null; }
}
