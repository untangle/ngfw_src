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

package com.metavize.tran.mail;

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
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mail.papi.ByteBufferByteStuffer;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.UnparseException;
import com.metavize.tran.token.UnparseResult;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

class PopUnparser extends AbstractUnparser
{
    private final static Logger logger = Logger.getLogger(PopUnparser.class);

    private final static String ENCODING = System.getProperty("file.encoding");
    private final static Charset CHARSET = Charset.forName(ENCODING);

    private final static int LINE_SZ = 1024;

    private PopReply zMsgDataReply;

    public PopUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);

        zMsgDataReply = null;
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        logger.debug("unparser got: " + token.getClass());

        List<ByteBuffer> zWriteBufs = new LinkedList<ByteBuffer>();

        if (token instanceof MIMEMessageHolderT)
        {
            /* we'll insert message reply to start of list later
             * (after we determine if message has been modified)
             *
             * XXXX change later
             *      (to obtain count of and to stream byte stuffed data
             *       from file and
             *       to avoid reading byte stuffed data into memory)
             */

            MIMEMessageHolderT zMMHolderT = (MIMEMessageHolderT) token;
            File zMsgFile = zMMHolderT.getFile();

            try
            {            
                FileChannel zMsgChannel = new FileInputStream(zMsgFile).getChannel();

                ByteBufferByteStuffer zByteStuffer = new ByteBufferByteStuffer();
                ByteBuffer zReadBuf = ByteBuffer.allocate(LINE_SZ);
                int iNewMsgDataSz = 0;

                ByteBuffer zWriteBuf;

                try
                {
                    while (0 < zMsgChannel.read(zReadBuf))
                    {
                        zWriteBuf = ByteBuffer.allocate(LINE_SZ);
                        iNewMsgDataSz += zByteStuffer.transfer(zReadBuf, zWriteBuf);
                        zWriteBufs.add(zWriteBuf);
                    }

                    zWriteBuf = zByteStuffer.getLast(true);
                    iNewMsgDataSz += zWriteBuf.limit();
                    zWriteBufs.add(zWriteBuf);

                    zWriteBufs.add(0, updateMsgDataSz(iNewMsgDataSz));
                }
                catch (IOException exn2)
                {
                    zWriteBufs.clear();
                    logger.warn("cannot read data from message file: ", exn2);
                }
                finally
                {
                    closeMsgChannel(zMsgChannel);
//XXXX                zMsgFile.delete(); //XXXX
                    zMsgFile = null;
                }
            }
            catch (FileNotFoundException exn)
            {
                zWriteBufs.clear();
                logger.warn("cannot access message file: ", exn);
            }
            finally
            {
                MIMEMessage zMMessage = zMMHolderT.getMIMEMessage();
                if (null != zMMessage)
                {
                    zMMessage.dispose();
                }
                else
                {
//XXXX
                }
            }
        }
        else if (token instanceof PopReply)
        {
            PopReply zReply = (PopReply) token;

            if (null == zMsgDataReply &&
                true == zReply.isMsgData())
            {
                zMsgDataReply = zReply;
            }
            else if (null != zMsgDataReply)
            {
                /* something went wrong with parser so dump everything */
                zWriteBufs.add(zMsgDataReply.getBytes());
                zWriteBufs.add(zReply.getBytes());
                zMsgDataReply = null;
            }
            else
            {
                zWriteBufs.add(zReply.getBytes());
            }
        }
        else
        {
            zWriteBufs.add(token.getBytes());
        }

        return new UnparseResult(zWriteBufs.toArray(new ByteBuffer[zWriteBufs.size()]));
    }

    public TokenStreamer endSession() { return null; }

    private ByteBuffer updateMsgDataSz(int iNewMsgDataSz)
    {
        String zMsgDataSz = zMsgDataReply.getMsgDataSz();
        int iMsgDataSz = Integer.valueOf(zMsgDataSz).intValue();

        ByteBuffer zNewBuf = zMsgDataReply.getBytes();

        if (iMsgDataSz != iNewMsgDataSz)
        {
            /* rebuild retrieve-ok reply because message size has changed */

            logger.debug("orig size: " + iMsgDataSz + ", new size: " + iNewMsgDataSz);

            Pattern zMsgDataSzP = Pattern.compile(zMsgDataSz);

            String zDataOK = AsciiCharBuffer.wrap(zNewBuf).toString();
            Matcher zMatcher = zMsgDataSzP.matcher(zDataOK);

            String zNewDataOK = zMatcher.replaceAll(String.valueOf(iNewMsgDataSz));
            try
            {
                zNewBuf = toByteBuffer(zNewDataOK);
            }
            catch (CharacterCodingException exn)
            {
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
        zLine.position(zLine.limit()); /* set position to indicate that ByteBuffer contains data */

        return zLine;
    }

    private void closeMsgChannel(FileChannel zMsgChannel)
    {
        try
        {
            zMsgChannel.close();
        }
        catch (IOException exn)
        {
            logger.warn("cannot close message file: ", exn);
        }
        finally
        {
            zMsgChannel = null;
        }

        return;
    }

    public TCPStreamer endSession() { return null; }
}
