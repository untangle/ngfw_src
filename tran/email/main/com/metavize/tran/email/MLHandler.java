/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MLHandler.java,v 1.14 2005/03/15 02:11:53 amread Exp $
 */
package com.metavize.tran.email;

import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.util.*;
import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class MLHandler
{
    class WarningInfo
    {
        private ByteBuffer zUserAddr;
        private ArrayList zFroms;
        private ArrayList zRecips;
        private ArrayList zDates;
        private ArrayList zSubjs;
        private int iMsgSz;

        public WarningInfo(MLMessage zMsg)
        {
            zUserAddr = getUserAddress(zMsg).get();

            /* copy date and subject field lists (if present)
             * because we may flush message later
             * (flush operation clears original lists)
             */
            ArrayList zTmps = zMsg.getDate();
            if (null != zTmps)
            {
                zDates = (ArrayList) zTmps.clone();
            }
            else
            {
                zDates = null;
            }

            MLMessagePTO zMsgPTO = zMsg.getPTO();

            zTmps = (ArrayList) zMsgPTO.get(Constants.FROM_IDX);
            if (null != zTmps)
            {
                zFroms = (ArrayList) zTmps.clone();
            }
            else
            {
                zFroms = null;
            }

            zTmps = (ArrayList) zMsgPTO.get(Constants.RECIPIENT_IDX);
            if (null != zTmps)
            {
                zRecips = (ArrayList) zTmps.clone();
            }
            else
            {
                zRecips = null;
            }

            zTmps = (ArrayList) zMsgPTO.get(Constants.SUBJECT_IDX);
            if (null != zTmps)
            {
                zSubjs = (ArrayList) zTmps.clone();
            }
            else
            {
                zSubjs = null;
            }

            iMsgSz = zMsg.getSize();
        }

        public ByteBuffer getUserAddr()
        {
            return zUserAddr;
        }

        public ArrayList getFrom()
        {
            return zFroms;
        }

        public ArrayList getRecip()
        {
            return zRecips;
        }

        public ArrayList getDate()
        {
            return zDates;
        }

        public ArrayList getSubj()
        {
            return zSubjs;
        }

        public int getMsgSz()
        {
            return iMsgSz;
        }
    }

    /* constants */
    private static final Logger zLog = Logger.getLogger(MLHandler.class.getName());
    private static final Logger zUserLog = MvvmContextFactory.context().eventLogger();

    /* RFC 821:
     *  command line
     *     The maximum total length of a command line including the
     *     command word and the <CRLF> is 512 characters.
     *
     *  reply line
     *     The maximum total length of a reply line including the
     *     reply code and the <CRLF> is 512 characters.
     *
     *  text line
     *     The maximum total length of a text line including the
     *     <CRLF> is 1000 characters (but not counting the leading
     *     dot duplicated for transparency).
     *
     * For now, we'll apply these limits to RFC 1939 and 1730/2060 too.
     */
    public final static int READSZ = 1024; /* round up from 1000 */

    /* endpoint types */
    public final static int DRIVER = Constants.CLIENT;
    public final static int PASSENGER = Constants.SERVER;

    private final static int MAX_LINE_SZ = 65536;
    private final static int EOS = -1; /* end of stream indicator */
    private final static int COPYALL = -1;

    private final static int REJECT_BUFFER_CT = 10;

    private final static byte NOUSERBA[] = { 'U', 'n', 'k', 'n', 'o', 'w', 'n', 'U', 's', 'e', 'r' };

    /* class variables */

    /* instance variables */
    protected MLHandlerInfo zHdlInfo;

    protected StateMachine zStateMachine;
    protected CBufferWrapper zCDummy; /* for temp use only */
    protected CBufferWrapper zUserName;
    protected CBufferWrapper zNoUser;

    private WarningInfo zWarningInfo;

    protected MLMessage zMsg = null;
    protected MLMessageInfo zMsgInfo;
    protected ArrayList zMsgDatas; /* reference copy of message data */
    protected Integer zReadDataSz; /* expected size of message */
    protected int iReadDataLimit;
    protected int iRejectedCt;
    protected boolean bRejectData;

    protected ByteBuffer zPostmaster = null;
    protected int iMsgSzLimit = Constants.NO_MSGSZ_LIMIT;
    protected boolean bReturnErr = false;

    /* endpoint that sends message
     * if driver supplies message, message is outbound
     * if passenger supplies message, message is inbound
     */
    protected int iSendsMsg = PASSENGER;

    private int iMsgSrc = PASSENGER; /* endpoint that is source of message */

    /* constructors */
    protected MLHandler()
    {
        zStateMachine = new StateMachine();
        zCDummy = new CBufferWrapper(null);

        zUserName = zNoUser = new CBufferWrapper(ByteBuffer.wrap(NOUSERBA, NOUSERBA.length, 0));
    }

    /* public methods */
    /* identify e-mail handler */
    public static MLHandler identify(TCPChunkEvent zEvent, XMSEnv zEnv)
    {
        /* get server service type */
        try
        {
            if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
            {
                /* line is not complete - get rest of line */
                return null;
            }
        }
        catch (ReadException e)
        {
            /* may not be e-mail session so discard read exception */
            return null;
        }

        CBufferWrapper zCLine = zEnv.getReadCLine();
        zLog.debug("identify: " + zCLine);

        MLHandler zHandler = zEnv.getHandler();
        if (null != zHandler)
        {
            /* we have already identified endpoint on other side;
             * it is passenger so endpoint on this side must be driver
             * - other bookend has already identified server service type
             *   so this data must be something else
             */
            return zHandler;
        }

        int zSessionId = zEvent.session().id();

        if (true == isMatch(zCLine, SMTPHandler.SERVICEOPENP))
        {
            zLog.debug("is SMTP");
            zHandler = new SMTPHandler();
            zHandler.iSendsMsg = DRIVER;
            zHandler.iMsgSrc = DRIVER;
            zHandler.zHdlInfo = new MLHandlerInfo(zSessionId, Constants.SMTP_RID, zCLine);
        }
        else if (true == isMatch(zCLine, POPHandler.SERVICEOPENP))
        {
            zLog.debug("is POP");
            zHandler = new POPHandler();
            zHandler.zHdlInfo = new MLHandlerInfo(zSessionId, Constants.POP3_RID, zCLine);
        }
        else if (true == isMatch(zCLine, IMAPHandler.SERVICEOPENP))
        {
            zLog.debug("is IMAP");
            zHandler = new IMAPHandler();
            /* for IMAP, either endpoint may send msg
             * (DRIVER sends msg for APPEND cmd,
             *  PASSENGER sends msg for FETCH cmd)
             */
            zHandler.zHdlInfo = new MLHandlerInfo(zSessionId, Constants.IMAP4_RID, zCLine);
        }
        else
        {
            /* else could not identify service type
             * so session is not servicing expected mail protocols
             * - pass through line and
             *   let caller release session
             */
            zLog.debug("is not mail");
            zHandler = null;
        }

        /* we are passing through id line so we do not need to save it */
        zEnv.resetReadCLine();
        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return zHandler;
    }

    public void setOptions(XMailScannerCache zXMSCache)
    {
        return;
    }

    /* check if command matches state machine window of message transaction */
    public void checkCmd(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException, ParseException
    {
        if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
        {
            /* line is not complete - get rest of line */
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
            return;
        }

        CBufferWrapper zCLine = zEnv.getReadCLine();
        zLog.debug("check cmd: " + zCLine);
        /* we are passing through cmd so we do not need to save it */
        zEnv.resetReadCLine();
        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    /* check if reply matches state machine window of message transaction */
    public void checkReply(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException, ParseException
    {
        if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
        {
            /* line is not complete - get rest of line */
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
            return;
        }

        CBufferWrapper zCLine = zEnv.getReadCLine();
        zLog.debug("check reply: " + zCLine);
        /* we are passing through reply so we do not need to save it */
        zEnv.resetReadCLine();
        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    /* msg upload - msg flows from transform client to transform server
     *            - msg is outbound
     * msg download - msg flows from transform server to transform client
     *              - msg is inbound
     *
     * outbound session - transform client initiates session
     * inbound session - transform server initiates session
     *
     * SMTP - e-mail client sends msg to e-mail server
     *        (client is message source)
     * POP3 - e-mail client retrieves msg from e-mail server
     *        (server is message source)
     * IMAP4 - e-mail client retrieves msg from e-mail server,
     *         e-mail client copies msg to e-mail server
     *         (e-mail client retrieves msg,
     *          that it copies,
     *          from e-mail server)
     *         (server is message source)
     */
    public boolean isMsgUploaded(boolean bSessionIsInbound)
    {
        if (PASSENGER == iMsgSrc) /* POP3, IMAP4 */
        {
           return (false == bSessionIsInbound ? false : true);
        }
        else /* SMTP */
        {
           return (false == bSessionIsInbound ? true : false);
        }
    }

    public boolean isMsgReady(int iEndpoint)
    {
        //zLog.debug("endpoint: " + iEndpoint + ", endpoint (sends msg): " + iSendsMsg);
        if (iEndpoint != iSendsMsg ||
            null == zMsg ||
            true == zMsg.isEmpty())
        {
            return false;
        }

        boolean bReady = zMsg.isReady();
        if (true == bReady)
        {
            zLog.debug("msg is ready");
        }
        return bReady;
    }

    public MLHandlerInfo getHdlInfo()
    {
        return zHdlInfo;
    }

    public MLMessageInfo getMsgInfo()
    {
        return zMsgInfo;
    }

    public MLMessage getMsg()
    {
        return zMsg;
    }

    public void block(XMSEnv zEnv, boolean bCopy, int iType)
    {
        return;
    }

    public void modify(XMailScannerCache zXMSCache, Object zAction) throws ModifyException
    {
        if (true == zMsg.isEmpty())
        {
            zMsg.flush(); /* delete remaining contents of message */
            return;
        }

        zMsg.modify(zXMSCache, zAction);
        return;
    }

    public VirusScannerResult scan(XMailScannerCache zXMSCache, VirusScanner zScanner, boolean bReplace) throws ModifyException, IOException, InterruptedException
    {
        if (true == zMsg.isEmpty())
        {
            zMsg.flush(); /* delete remaining contents of message */
            return null;
        }

        return zMsg.scan(zXMSCache, zScanner, bReplace);
    }

    /* resend buffered message
     * - returns true if resend succeeds or
     *   false if resend fails
     */
    public boolean resend(XMSEnv zEnv)
    {
        return true;
    }

    public void setup()
    {
        return;
    }

    public void flushMsg(XMSEnv zEnv)
    {
        zMsg.flush(); /* delete contents of message but reuse this object */
        zEnv.clearReadDataCt();
        return;
    }

    /* private methods */
    /* read command or reply line
     * - returns true if line is complete or
     *           false if line is incomplete
     */
    protected static boolean readLine(TCPChunkEvent zEvent, XMSEnv zEnv, Pattern zEOLPattern) throws ReadException
    {
        /* by default,
         * when line is returned,
         * line position is set to 0 and
         * line limit indicates location of end of data
         */
        ByteBuffer zLine = zEvent.chunk();
        int iPosition = zLine.limit();
        if (0 == iPosition)
        {
            /* buffer is empty - retry read operation */
            zLog.debug("readline (empty - read more)");
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
            return false;
        }

        /* reset line position so that we can work with data */
        zLine.position(iPosition);

        CBufferWrapper zCLine = zEnv.getReadCLine();
        if (null == zCLine)
        {
            zCLine = new CBufferWrapper(zLine);
            zEnv.setReadCLine(zCLine);
        }
        else
        {
            zCLine.renew(zLine); /* reuse existing ByteBuffer */
        }
        //zLog.debug("readline: " + zCLine + ", " + zLine);
        zLog.debug("readline");

        if (true == isMatch(zCLine, zEOLPattern))
        {
            copy(zCLine, COPYALL);

            /* if we have complete line,
             * we'll let caller decide what result to return
             */
            return true;
        }

        if (READSZ == iPosition)
        {
            /* line does not end with EOL
             * - although data can be fragmented,
             *   command and reply lines cannot be fragmented
             *   (e.g., cannot span multiple lines) and
             *   each line must end with EOL
             * - ignore this line (pass it through) and
             *   request another
             *   (hopefully, next line will make sense)
             */
            //zLog.debug("readline (no EOL - buffer is full): " + zCLine + ", " + zLine);
            /* we are passing through line so we do not need to save it */
            zEnv.resetReadCLine();
            zEnv.setFixedResult(Constants.PASS_THROUGH);

            CBufferWrapper zTmp = new CBufferWrapper(zLine);
            throw new ReadException("Passing through data fragment; line is full and is not terminated by end-of-line: " + zTmp);
        }
        /* else no EOL yet and buffer has space for more */

        //zLog.debug("readline (no EOL - read more): " + zLine);
        zLog.debug("readline (no EOL - read more)");
        zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
        return false;
    }

    /* read data
     * (note that each buffer may contain multiple text lines)
     * - returns true if data block is complete or
     *           false if data block is incomplete
     */
    protected boolean readData(TCPChunkEvent zEvent, XMSEnv zEnv, ArrayList zDatas, Pattern zEOLPattern, Pattern zEODPattern) throws ReadException
    {
        /* by default,
         * when line is returned,
         * line position is set to 0 and
         * line limit indicates location of end of data
         */
        ByteBuffer zLine = zEvent.chunk();
        int iPosition = zLine.limit();
        if (0 == iPosition)
        {
            /* buffer is empty - retry read operation */
            zLog.debug("readdata (empty - read more)");
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
            return false;
        }

        /* reset line position so that we can work with data */
        zLine.position(iPosition);
        CBufferWrapper zCLine = new CBufferWrapper(zLine);
        //zLog.debug("readdata: " + zCLine + ", " + zLine);

        Matcher zMatcher;

        if (null != zEODPattern)
        {
            zMatcher = zEODPattern.matcher(zCLine);
            if (true == zMatcher.find())
            {
                /* if we found EOD (which implicitly ends with EOL),
                 * we have no more data waiting for us
                 * (e.g., client must wait for server to reply
                 *  before client can send additional commands and
                 *  server only sends replies after client issues commands)
                 */
                zEnv.incrementReadDataCt(copy(zCLine, COPYALL));
                zDatas.add(zCLine);

                /* if we have complete data,
                 * we'll let caller decide what result to return
                 */
                return true;
            }
        }
        /* else EODPattern does not terminate data
         * - data terminates after some # of bytes
         */

        /* if this buffer doesn't end with EOL and
         * text requires more space than buffer capacity (should not occur),
         * then pass through everything that we have collected so far
         * or
         * if this buffer doesn't end with EOL and
         * last text line extends from this buffer into next buffer
         * (last text line spans two buffers),
         * then copy head of this buffer
         * (e.g., all data from start to and including last EOL)
         * to new buffer and
         * shift tail of this buffer to start of this buffer
         * (note that since we must always copy contents of read buffer,
         *  we do this even when buffer ends with EOL)
         */
        int iEnd = 0;
        zMatcher = zEOLPattern.matcher(zCLine);
        while (true == zMatcher.find())
        {
            iEnd = zMatcher.end();
        }

        if (0 == iEnd)
        {
            /* we have not received EOD and
             * buffer does not contain any EOL
             * (e.g., buffer is very long)
             */
            if (READSZ == iPosition)
            {
                /* line does not end with EOL
                 * - although data can be fragmented,
                 *   each line must end with EOL
                 */
                //zLog.debug("readdata (no EOL - buffer is full): " + zCLine + ", " + zLine);
                throw new ReadException("Passing through data fragment; line is full and is not terminated by end-of-line: " + zCDummy.renew(zLine));
            }
            /* else no EOL yet and buffer has space for more */

            //zLog.debug("readdata (no EOL - read more): " + zLine);
            zLog.debug("readdata (no EOL - read more)");
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
        }
        else
        {
            int iCopySz;

            /* we have not received EOD and
             * buffer contains at least one EOL
             */

            if (iEnd < zCLine.length())
            {
                /* this buffer does not end with EOL
                 * so copy up to and including last EOL
                 */
                iCopySz = copy(zCLine, iEnd);
            }
            else
            {
                /* this buffer ends with EOL so simply copy its contents */
                iCopySz = copy(zCLine, COPYALL);
            }

            zDatas.add(zCLine);

            /* note that since we modified original read buffer,
             * we cannot use READ_MORE_NO_WRITE here
             * even though we are reusing original read buffer
             * to read another buffer of data
             * but are not writing anything
             */
            zEnv.incrementReadDataCt(iCopySz);
        }

        return false;
    }

    /* replace Smith buffer with private copy of buffer
     * (copy contents of current backing buffer to new buffer)
     */
    private static int copy(CBufferWrapper zCLine, int iCopySz) throws ReadException
    {
        ByteBuffer zLine = zCLine.get(); /* get source buffer for copy op */

        ByteBuffer zCopyLine;
        int iSz;

        if (COPYALL == iCopySz)
        {
            iSz = zLine.position();
            zCopyLine = ByteBuffer.allocate(iSz); /* allocate new buffer */

            try
            {
                zLine.rewind();
                zCopyLine.put(zLine); /* we don't set limit since we allocated buffer to exact size */
                zLine.clear(); /* reset so that we can recycle this buffer */
            }
            catch (BufferOverflowException e)
            {
                zLine.clear(); /* reset so that we can recycle this buffer */
                throw new ReadException("Unable to copy read buffer (it is larger than originally requested)");
            }
        }
        else
        {
            iSz = iCopySz;
            zCopyLine = ByteBuffer.allocate(iSz); /* allocate new buffer */

            /* copy bytes from start to and including last EOL
             * from this buffer to new buffer
             */
            for (int iIdx = 0; iSz > iIdx; iIdx++)
            {
                zCopyLine.put(iIdx, zLine.get(iIdx)); /* we don't set limit since we allocated buffer to exact size */
            }

            zLine.position(iSz);
            zLine.compact(); /* move remaining bytes to start of this buffer */
            //zLog.debug("org (compact): " + zLine);
            zLog.debug("org (compact)");
        }

        //zLog.debug("copy: " + zCLine + ", " + zCopyLine);
        zCLine.renew(zCopyLine); /* swap original buffer with copy */
        return iSz;
    }

    /* write command or reply line or write data */
    protected IPDataResult write(TCPChunkEvent zEvent, XMSEnv zEnv, int iDriver)
    {
        IPDataResult zResult = zEnv.getFixedResult();
        if (null != zResult)
        {
            zLog.debug("write: from " + (Constants.CLIENT == iDriver ? "c" : "s") + ", return fixed: " + zResult);
            zEnv.setFixedResult(null); /* set up for next write */
            return zResult;
        }

        ArrayList zToDrivers = zEnv.getToDriver();
        ArrayList zToPassengers = zEnv.getToPassenger();

        ByteBuffer azToClient[];
        ByteBuffer azToServer[];
        Iterator zIter;
        int iIdx;

        switch(iDriver)
        {
        default:
        case Constants.CLIENT:
            if (false == zToDrivers.isEmpty())
            {
                zLog.debug("write: (c2c) mult lines: " + zToDrivers.size());
                azToClient = concat(zToDrivers);
            }
            else
            {
                azToClient = null;
            }

            if (false == zToPassengers.isEmpty())
            {
                zLog.debug("write: (c2s) mult lines: " + zToPassengers.size());
                azToServer = concat(zToPassengers);
            }
            else
            {
                azToServer = null;
            }

            break;

        case Constants.SERVER:
            if (false == zToDrivers.isEmpty())
            {
                zLog.debug("write: (s2s) mult lines: " + zToDrivers.size());
                azToServer = concat(zToDrivers);
            }
            else
            {
                azToServer = null;
            }

            if (false == zToPassengers.isEmpty())
            {
                zLog.debug("write: (s2c) mult lines: " + zToPassengers.size());
                azToClient = concat(zToPassengers);
            }
            else
            {
                azToClient = null;
            }

            break;
        }

        ByteBuffer zLine = zEvent.chunk(); /* recycle original read buffer */
        zLog.debug("write: from " + (Constants.CLIENT == iDriver ? "c" : "s") + ", return 2c: " + azToClient + ", 2s: " + azToServer + ", read: " + zLine);
        return new TCPChunkResult(azToClient, azToServer, zLine);
    }

    protected static boolean isMatch(CBufferWrapper zCLine, Pattern zPattern)
    {
        Matcher zMatcher = zPattern.matcher(zCLine);
        return zMatcher.find();
    }

    /* cache info from message header to build warning and
     * buffer/discard parts of this message
     * (we only keep last REJECT_BUFFER_CT buffers that we have read)
     * so that we can search these buffers for EODATA sequence
     */
    protected void setupWarning(XMSEnv zEnv)
    {
        try
        {
            zMsg.parse(false); /* parse header only; we don't need body */
        }
        catch (ParseException e)
        {
            /* ignore exception - we'll do what we can when we build warning */
        }

        zMsgInfo = new MLMessageInfo(zHdlInfo, zEnv, zMsg);
        zWarningInfo = new WarningInfo(zMsg);
        zMsg.reset();

        int iCnt = zMsgDatas.size() - REJECT_BUFFER_CT;
        if (0 < iCnt)
        {
            CBufferWrapper zCLine;
            int iIdx;

            for (iIdx = 0; iIdx < iCnt; iIdx++)
            {
                zCLine = (CBufferWrapper) zMsgDatas.get(iIdx);
                iRejectedCt += zCLine.length(); /* count discarded buffer */
                /* WarningInfo selectively references parts of message header
                 * so let GC figure out what it can process;
                 * we do not explicitly release message header for GC
                 */
                //zCLine.renew(null);
            }

            /* we can't use removeRange because it is protected method;
             * we'll indirectly remove elements in range that interests us
             * by defining this range as sublist (of source list) and
             * clearing sublist
             * - sublist includes 0 but excludes iCnt
             * - changes to sublist also change backing (source) list
             */
            zMsgDatas.subList(0, iCnt).clear();
        }

        return;
    }

    /* strip EOD sequence from message and return stripped EOD sequence */
    protected ArrayList stripEOD()
    {
        CBufferWrapper zCLine = null;
        int iMsgSz = zMsg.getSize();
        if (0 < iRejectedCt)
        {
            /* discount # of bytes, of buffers that we've already discarded,
             * from message size
             */
            iMsgSz -= iRejectedCt;
        }

        int iCnt = zMsgDatas.size();

        int iIdx;

        for (iIdx = 0; iIdx < iCnt; iIdx++)
        {
            zCLine = (CBufferWrapper) zMsgDatas.get(iIdx);
            iMsgSz -= zCLine.length();
            if (0 >= iMsgSz)
            {
                break; /* rest is EOD sequence */
            }
        }

        int iStart;

        if (0 == iMsgSz)
        {
            iIdx++; /* EOD sequence starts on next text line */
            zCLine = (CBufferWrapper) zMsgDatas.get(iIdx);
            iStart = 0;
        }
        else /* iMsgSz < 0 */
        {
            /* EOD sequence starts on current text line */
            iStart = zCLine.length() + iMsgSz;
        }

        ByteBuffer zLine = zCLine.get();
        int iPosition = zLine.position();
        //int iLimit = zLine.limit();

        int iSize = iPosition - iStart;
        zLine.position(iStart);
        zLine.limit(iPosition);

        /* copy 1st EOD sequence (which may be fragment) to new line */
        ByteBuffer zTmp = ByteBuffer.allocate(iSize);
        zTmp.put(zLine); /* we don't set limit since we allocated buffer to exact size */

        /* truncate text line (to end of message) */
        zLine.position(iStart);
        zLine.limit(iStart);

        ArrayList zTEODatas = new ArrayList(); /* list of ByteBuffers */
        zTEODatas.add(zTmp);

        /* handle rest of EOD sequence (if any) */
        iIdx++;
        for (; iIdx < iCnt; iIdx++)
        {
            /* swap out EOD sequence (at end of list) with empty lines */
            zCLine = (CBufferWrapper) zMsgDatas.get(iIdx);
            zTmp = zCLine.get();
            zTEODatas.add(zTmp);

            zCLine.renew(ByteBuffer.allocate(1));
        }

        return zTEODatas;
    }

    /* strip EOD sequence from text line and return stripped EOD sequence */
    protected ByteBuffer stripEOD(CBufferWrapper zCLine, Pattern zEODPattern)
    {
        ByteBuffer zLine = zCLine.get();
        //zLog.debug("last line: " + zCLine + ", " + zLine);

        Matcher zEODMatcher = zEODPattern.matcher(zCLine);
        int iStart = -1;
        int iEnd = -1;

        while (true == zEODMatcher.find())
        {
            iStart = zEODMatcher.start();
            iEnd = zEODMatcher.end();
        }

        ByteBuffer zTEOData;

        if (-1 != iStart) /* also implies that iEnd != -1 */
        {
            try
            {
                int iSize = iEnd - iStart;

                /* define EODATA range in text line */
                zLine.position(iStart);
                zLine.limit(iEnd);

                /* create copy of EODATA */
                zTEOData = ByteBuffer.allocate(iSize);
                zTEOData.put(zLine); /* we don't set limit since we allocated buffer to exact size */

                /* truncate text line (to start of EODATA) */
                zLine.position(iStart); /* reset */
                zLine.limit(iStart); /* reset */
            }
            catch (BufferOverflowException e)
            {
                zLog.error("Unable to copy EODATA sequence) from source (" + zCLine + ") into temporary buffer (sequence is larger than originally calculated): " + e);
                zTEOData = null;
            }
        }
        else
        {
            /* if we don't begin to match EODATA immediately,
             * then assume that EODATA doesn't exist in this buffer
             */
            zTEOData = null;
        }

        //zLog.debug("last line (new): " + zCLine + ", " + zLine);
        return zTEOData;
    }

    /* to maximize performance, we send lines as large chunks of data
     * rather than send each line "separately"
     */
    private ByteBuffer[] concat(ArrayList zList)
    {
        ByteBuffer zLine;

        if (1 == zList.size())
        {
            /* if list only contains single element,
             * we have no lines to concat
             */
            ByteBuffer azDataSE[] = new ByteBuffer[1];
            zLine = (ByteBuffer) zList.get(0);
            //zLog.debug("write: one line: " + zCDummy.renew(zLine) + ", " + zLine);
            zLine.rewind();
            azDataSE[0] = zLine;
            zList.clear(); /* set up for next write */
            return azDataSE;
        }

        ListIterator zLIter;
        int iDivisor = 0;

        for (zLIter = zList.listIterator(); true == zLIter.hasNext(); )
        {
            zLine = (ByteBuffer) zLIter.next();
            if (iDivisor < zLine.position())
            {
                /* choose line w/ most data as (rough) divisor */
                iDivisor = zLine.position();
            }
        }

        int iMaxLineSz;

        if (iDivisor > MAX_LINE_SZ)
        {
            iMaxLineSz = iDivisor + MAX_LINE_SZ;
            zLog.debug("increasing line size from " + MAX_LINE_SZ + " to " + iMaxLineSz + " bytes");
        }
        else
        {
            iMaxLineSz = MAX_LINE_SZ;
        }

        /* we estimate that we need this many new lines
         * to hold concatenation of org lines
         */
        ArrayList zNewList = new ArrayList((zList.size() / iDivisor) + 1);
        ByteBuffer zNewLine = ByteBuffer.allocate(iMaxLineSz);
        zNewList.add(zNewLine);

        for (zLIter = zList.listIterator(); true == zLIter.hasNext(); )
        {
            zLine = (ByteBuffer) zLIter.next();
            zLIter.set(null); /* release reference; let GC process it */

            if (zLine.position() > zNewLine.remaining())
            {
                /* new line is full, get another */
                zNewLine = ByteBuffer.allocate(iMaxLineSz);
                zNewList.add(zNewLine);
            }

            //zLog.debug("write: org line: " + zCDummy.renew(zLine) + ", " + zLine);
            zLine.rewind();
            zNewLine.put(zLine); /* concat org line into new line */
        }
        zList.clear(); /* set up for next write */

        ByteBuffer azData[] = new ByteBuffer[zNewList.size()];
        int iIdx = 0;

        for (zLIter = zNewList.listIterator(); true == zLIter.hasNext(); )
        {
            zNewLine = (ByteBuffer) zLIter.next();
            zLIter.set(null); /* release reference; let GC process it */
            //zLog.debug("write: new line: " + zCDummy.renew(zNewLine) + ", " + zNewLine);
            zNewLine.flip(); /* we use flip to set limit since we didn't allocate buffer to exact size */
            azData[iIdx++] = zNewLine;
        }
        zNewList.clear();

        return azData;
    }

    /* block message because it matches custom, spam, or virus rule */
    protected void block(XMSEnv zEnv, ByteBuffer zOKLine, ByteBuffer zErrLine, boolean bCopy)
    {
        if (true == bCopy)
        {
            /* copy this message to file */
            String zTmpFile = zMsg.toFile(Constants.BLOCKDIR_VAL);
            zLog.info("A copy of the blocked message has been saved: " + zTmpFile);
        }

        flushMsg(zEnv);
        zReadDataSz = null; /* clear now even though we'll setup soon */

        if (true == bReturnErr)
        {
            /* block message and send err reply */
            zLog.debug("block msg and report msg err");

            zEnv.sendToDriver(zErrLine);
            zEnv.resetReadCLine();
            return;
        }
        /* else silently block message */

        /* we did not ack EODATA with pseudo-server reply (OK) yet
         * - we'll send pseudo-server reply now
         */
        zEnv.sendToDriver(zOKLine);
        zEnv.clearReadCLine();
        return;
    }

    protected void block(XMSEnv zEnv, ByteBuffer zErrLine, boolean bCopy, int iType)
    {
        String zTmpFile;
        if (true == bCopy)
        {
            /* copy this message to file */
            zTmpFile = zMsg.toFile(Constants.BLOCKDIR_VAL);
            zLog.info("A copy of the blocked message has been saved: " + zTmpFile);
        }
        else
        {
            zTmpFile = null;
        }

        if (true == bReturnErr)
        {
            /* block message and send err reply */
            zLog.debug("block msg and report msg err");

            flushMsg(zEnv);
            zReadDataSz = null; /* clear now even though we'll setup soon */

            zEnv.sendToDriver(zErrLine);
            zEnv.resetReadCLine();
            return;
        }
        /* else block message and
         * warn recipient that this message has been blocked
         */

        setupWarning(zEnv);
        buildWarning(zEnv, zTmpFile, iType);
        return;
    }

    protected void rejectData()
    {
        if (REJECT_BUFFER_CT < zMsgDatas.size())
        {
            /* we only keep last REJECT_BUFFER_CT buffers that we have read
             * - we need these buffers to search for EODATA sequence
             */
            CBufferWrapper zCLine = (CBufferWrapper) zMsgDatas.remove(0);
            iRejectedCt += zCLine.length(); /* count discarded buffer */
            /* WarningInfo does not reference these parts of message
             * so we can explicitly release this data;
             * let GC process it
             */
            zCLine.renew(null);
        }

        /* we don't clear/reset read data size;
         * we continue to monitor # of data bytes read
         */
        return;
    }

    /* reject (block) message because it exceeds size limit */
    protected void rejectEOData(TCPChunkEvent zEvent, XMSEnv zEnv, ByteBuffer zOKLine, ByteBuffer zErrLine)
    {
        zMsgInfo.setSize(zEnv.getReadDataCt());
        zUserLog.info(new SizeLimitEvent(zMsgInfo));

        flushMsg(zEnv); /* flush any message remnants */
        zReadDataSz = null; /* clear now even though we'll setup soon */

        if (true == bReturnErr)
        {
            /* reject message and send err reply */
            zLog.debug("reject msg and report msg err");

            /* we will not ack EODATA with pseudo-server reply (OK)
             * - instead, we'll reject message with pseudo-server reply (ERR)
             */
            zEnv.sendToDriver(zErrLine);
            zEnv.clearReadCLine();
            return;
        }
        /* else silently reject message */

        /* we did not ack EODATA with pseudo-server reply (OK) yet
         * - we'll send pseudo-server reply now
         */
        zEnv.sendToDriver(zOKLine);
        zEnv.clearReadCLine();
        return;
    }

    protected void rejectEOData(TCPChunkEvent zEvent, XMSEnv zEnv, ByteBuffer zErrLine, int iType)
    {
        zMsgInfo.setSize(zEnv.getReadDataCt());
        zUserLog.info(new SizeLimitEvent(zMsgInfo));

        if (true == bReturnErr)
        {
            /* reject message and send err reply */
            zLog.debug("reject msg and report msg err");

            flushMsg(zEnv); /* flush any message remnants */
            zReadDataSz = null; /* clear now even though we'll setup soon */

            zEnv.sendToDriver(zErrLine);
            zEnv.clearReadCLine();
            return;
        }
        /* else reject message and
         * warn recipient that this message has been rejected
         */

        buildWarning(zEnv, null, iType);
        return;
    }

    private CBufferWrapper getUserAddress(MLMessage zMsg)
    {
        CharsetDecoder zDecoder = Constants.CHARSET.newDecoder();
        ByteBuffer zUNLine = zUserName.get();

        Pattern zUserNameP;
        try
        {
            zUNLine.rewind(); /* we will not use this ByteBuffer again */
            zUserNameP = Pattern.compile(zDecoder.decode(zUNLine).toString());
        }
        catch (CharacterCodingException e)
        {
            zLog.error("Unable to encode line: " + zUNLine + " : " + e);
            return zNoUser;
        }

        Matcher zUserNameMatcher = zUserNameP.matcher(zUserName);
        Matcher zContListMatcher = Constants.CONTLISTP.matcher(zUserName);

        CBufferWrapper zUserAddress;

        /* try To list first */
        ArrayList zList = zMsg.getToList();
        if (null != zList)
        {
            //zLog.debug("to list: " + zList);
            zUserAddress = getUserAddress(zList, zUserNameMatcher, zContListMatcher);
            if (null != zUserAddress)
            {
                return zUserAddress;
            }
        }
        /* then try Cc list */
        zList = zMsg.getCcList();
        if (null != zList)
        {
            //zLog.debug("cc list: " + zList);
            zUserAddress = getUserAddress(zList, zUserNameMatcher, zContListMatcher);
            if (null != zUserAddress)
            {
                return zUserAddress;
            }
        }
        /* finally, try Bcc list */
        zList = zMsg.getBccList();
        if (null != zList)
        {
            //zLog.debug("bcc list: " + zList);
            zUserAddress = getUserAddress(zList, zUserNameMatcher, zContListMatcher);
            if (null != zUserAddress)
            {
                return zUserAddress;
            }
        }

        /* no success, so build pseudo username address */
        ByteBuffer zLine = ByteBuffer.allocate(READSZ);
        zUNLine.rewind();
        zLine.put(zUNLine);

        ByteBuffer zDomain = ByteBuffer.wrap(Constants.LOCALHOSTBA, Constants.LOCALHOSTBA.length, 0);
        zDomain.flip();
        zLine.put(zDomain);

        zLine.limit(zLine.position()); /* we set limit since we didn't allocate buffer to exact size */

        zUserAddress = new CBufferWrapper(zLine);
        return zUserAddress;
    }

    private CBufferWrapper getUserAddress(ArrayList zList, Matcher zUserNameMatcher, Matcher zContListMatcher)
    {
        Iterator zIter;
        CBufferWrapper zCLine;
        CBufferWrapper zUserAddress;
        int iStart;

        for (zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zUserNameMatcher.reset(zCLine);
            if (true == zUserNameMatcher.find())
            {
                iStart = zUserNameMatcher.start();
                zContListMatcher.reset(zCLine);
                if (false == zContListMatcher.find(iStart))
                {
                    continue; /* for loop */
                }

                zUserAddress = (CBufferWrapper) zCLine.subSequence(iStart, zContListMatcher.start());
                //zLog.debug("user address is " + zUserAddress);
                return zUserAddress;
            }
        }

        return null;
    }

    /* swap original message with warning message */
    private void buildWarning(XMSEnv zEnv, String zTmpFile, int iType)
    {
        ByteBuffer zFromLine = ByteBuffer.allocate(READSZ);
        ByteBuffer zLine = ByteBuffer.wrap(Constants.BLOCKFROMBA, Constants.BLOCKFROMBA.length, 0);
        zLine.flip();
        zFromLine.put(zLine);
        zLine = zPostmaster;
        zLine.rewind();
        zFromLine.put(zLine);
        ByteBuffer zEOLine = ByteBuffer.wrap(Constants.EOLINEBA, Constants.EOLINEBA.length, 0);
        zEOLine.flip();
        zFromLine.put(zEOLine);
        zFromLine.limit(zFromLine.position()); /* we set limit since we didn't allocate buffer to exact size */

        ByteBuffer zToLine = ByteBuffer.allocate(READSZ);
        zLine = ByteBuffer.wrap(Constants.BLOCKTOBA, Constants.BLOCKTOBA.length, 0);
        zLine.flip();
        zToLine.put(zLine);
        zLine = zWarningInfo.getUserAddr();
        zLine.rewind();
        zToLine.put(zLine);
        zEOLine.rewind();
        zToLine.put(zEOLine);
        zToLine.limit(zToLine.position()); /* we set limit since we didn't allocate buffer to exact size */

        ByteBuffer zEmptyLine = ByteBuffer.allocate(zEOLine.limit());
        zEOLine.rewind();
        zEmptyLine.put(zEOLine); /* we don't set limit since we allocated buffer to exact size */

        CBufferWrapper zFromCLine = new CBufferWrapper(zFromLine);
        CBufferWrapper zToCLine = new CBufferWrapper(zToLine);
        CBufferWrapper zEmptyCLine = new CBufferWrapper(zEmptyLine);

        int iMsgSz = zWarningInfo.getMsgSz(); /* save original size */
        flushMsg(zEnv);
        /* restore original size for resend
         * - even though we will replace message,
         *   we must restore original size
         *   so that resend can fix retrieve ok reply
         */
        zMsg.setSize(iMsgSz);
        /* since we've restored original size, we keep zReadDataSz as is */

        zMsgDatas.add(zFromCLine);
        zMsgDatas.add(zToCLine);
        ArrayList zTmps = zWarningInfo.getDate();
        if (null != zTmps)
        {
            zMsgDatas.addAll(zTmps);
        }
        zTmps = zWarningInfo.getSubj();
        if (null != zTmps)
        {
            zMsgDatas.addAll(zTmps);
        }
        zMsgDatas.add(zEmptyCLine);

        ByteBuffer zBodyLine = ByteBuffer.allocate(READSZ);
        switch(iType)
        {
        case Constants.AVBLOCK:
            zBodyLine = ByteBuffer.wrap(Constants.BLOCKAVBODYBA, Constants.BLOCKAVBODYBA.length, 0);
            break;

        case Constants.ASBLOCK:
            zBodyLine = ByteBuffer.wrap(Constants.BLOCKASBODYBA, Constants.BLOCKASBODYBA.length, 0);
            break;

        case Constants.REJECT:
            zBodyLine = ByteBuffer.wrap(Constants.REJECTBODYBA, Constants.REJECTBODYBA.length, 0);
            break;

        default:
        case Constants.CSBLOCK:
            zBodyLine = ByteBuffer.wrap(Constants.BLOCKCSBODYBA, Constants.BLOCKCSBODYBA.length, 0);
            break;
        }

        zBodyLine.limit(zBodyLine.position()); /* we set limit since we didn't allocate buffer to exact size */
        CBufferWrapper zBodyCLine = new CBufferWrapper(zBodyLine);
        zMsgDatas.add(zBodyCLine);

        zBodyLine = ByteBuffer.wrap(Constants.BLOCKBODYAUTOBA, Constants.BLOCKBODYAUTOBA.length, 0);
        zBodyCLine = new CBufferWrapper(zBodyLine);
        zMsgDatas.add(zBodyCLine);

        if (null != zTmpFile)
        {
            zBodyLine = ByteBuffer.wrap(Constants.BLOCKBODYCOPYBA, Constants.BLOCKBODYCOPYBA.length, 0);
            zBodyCLine = new CBufferWrapper(zBodyLine);
            zMsgDatas.add(zBodyCLine);

            String zFNStr = "(" + zTmpFile + ")" + Constants.PCRLF;

            try
            {
                CharsetEncoder zEncoder = Constants.CHARSET.newEncoder();
                ByteBuffer zFNLine = zEncoder.encode(CharBuffer.wrap(zFNStr));
                zFNLine.position(zFNLine.limit()); /* set position to indicate that ByteBuffer contains data */

                zBodyCLine = new CBufferWrapper(zFNLine);
                zMsgDatas.add(zBodyCLine);
            }
            catch (CharacterCodingException e)
            {
                zLog.error("Unable to encode line: " + zFNStr + " : " + e);
                /* fall through */
            }
        }

        zWarningInfo = null; /* release; let GC process */

        //zLog.debug("insert warning msg: " + zMsgDatas);
        zLog.debug("insert warning msg");
        return;
    }
}
