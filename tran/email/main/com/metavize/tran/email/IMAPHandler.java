/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IMAPHandler.java,v 1.8 2005/02/24 03:46:39 cng Exp $
 */
package com.metavize.tran.email;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.util.*;

/* IMAP - RFC1730,2060 */
/* FETCH - client fetches message from mailbox on server
 * APPEND - client appends message to mailbox on server
 *
 * 19 UID fetch 115354 (UID RFC822.SIZE BODY[])^M
 * * 3 FETCH (UID 115354 RFC822.SIZE 6977 BODY[] {6977}^M
 * ****SIZE = 6977 (variable)
 * ****BODY follows
 *  FLAGS (\Seen \Recent XAOL-READ))^M
 * 19 OK UID FETCH command completed^M
 *
 * msg_no list = msg_no:msg_no
 * uniq_id list = uniq_id:uniq_id
 *
 * cmd_id <UID> fetch msg_no_list <or uniq_id_list> (...)
 * * msg_no <or msg_seq_no> fetch (... {size}
 * ****BODY follows
 *  ...)
 * cmd_id ok <UID> fetch (...)
 *
 *   Example:    C: A654 FETCH 2 (FLAGS BODY[])
 *               S: * 2 FETCH (... BODY[] {(size)}
 *               S: ...(msg)
 *               S:  ...)
 *               S: A654 OK FETCH completed
 *
 *   Example:    C: A999 UID FETCH (FLAGS UID BODY[])
 *               S: * 23 FETCH (... UID (uid) BODY[] {(size)}
 *               S: ...(msg)
 *               S:  ...)
 *               S: A999 OK UID FETCH completed
 *
 * 003A APPEND "INBOX" "22-Nov-2004 10:06:31 -0800" {567}^M
 * ****SIZE = 567 (variable)
 * + Ready for argument^M
 * ****BODY follows
 * 003A OK APPEND completed^M
 *
 * RFC1730/2060 does not document command continuation request
 * ("+ Ready for argument") for APPEND cmd but
 * IMAP client/server implementations appear to expect it
 *
 *   Example:    C: A003 APPEND saved-messages (\Seen) {(size)}
 *               S: + Ready for argument
 *               C: ...(msg)
 *               S: A003 OK APPEND completed
 */
public class IMAPHandler extends MLHandler
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(IMAPHandler.class.getName());

    private final static String TAG_ID = "^(\\p{Alnum})++"; /* cmd tag id */
    private final static String MSEQ_NO = "^\\* (\\p{Digit})++"; /* msg seq no */
    private final static String CONT_FLAG = "^\\+"; /* cmd continuation flag */
    private final static Pattern TAG_IDP = Pattern.compile(TAG_ID, Pattern.CASE_INSENSITIVE);

    /* currently, we only handle complete (not partial) messages
     * BODY[]
     * BODY.PEEK[]
     * RFC822 = BODY[]
     */
    private final static String BODY = "(BODY((.PEEK)??)\\[]|RFC822[[^\\.]&&[^S]&&[^I]&&[^Z]&&[^E]])+?";

    /* for now,
     * we do not handle fetches of message parts
     * (with part specifiers - BODY[HEADER], BODY[3.TEXT], ...)
     * or
     * partial fetches of message or message parts
     * (BODY[]<0.2048> - 1st 2048 bytes of message)
     * BODY[...] -> BODY[], BODY[HEADER], BODY[TEXT], BODY[3.TEXT], ...
     * BODY.PEEK[...] -> BODY.PEEK[], BODY.PEEK[HEADER], BODY.PEEK[TEXT], ...
     * RFC822 = BODY[], RFC822.HEADER = BODY.PEEK[HEADER],
     *  RFC822.TEXT = BODY[TEXT]
     private final static String HDRTXT = "(HEADER|TEXT)";
     private final static String SEC_HDRTXT = "(((\\p{Digit}+?)\\.)*?" + HDRTXT + ")??";
     private final static String BODY = "(BODY((.PEEK)??)\\[" + SEC_HDRTXT + "\\]|RFC822[[^\\.]&&[^S]&&[^I]&&[^Z]&&[^E]]|RFC822\\." + HDRTXT + "(<\\p{Digit}+?\\.\\p{Digit}+?>)??)+?";
     */

    private final static String LOGIN_KEY = "LOGIN";
    private final static Pattern LOGIN_KEYP = Pattern.compile(LOGIN_KEY, Pattern.CASE_INSENSITIVE);
    private final static String APPEND_KEY = "APPEND";
    private final static String FETCH_KEY = "FETCH";

    private final static String DIGVAL = "(\\p{Digit})++";
    private final static String SZVAL = "\\{" + DIGVAL + "\\}";
    private final static Pattern DIGVALP = Pattern.compile(DIGVAL);
    private final static Pattern SZVALP = Pattern.compile(SZVAL);

    /* (client) IMAP commands - ignore other commands */
    private final static String LOGIN = TAG_ID + " " + LOGIN_KEY + ".+?" + Constants.PEOLINE;
    private final static String APPEND = TAG_ID + " .*?" + APPEND_KEY + ".+?" + SZVAL + Constants.PEOLINE;
    private final static String FETCH = TAG_ID + " .*?" + FETCH_KEY + " .*?" + BODY + ".*?" + Constants.PEOLINE;
    private final static String LOGOUT = TAG_ID + " LOGOUT.*?" + Constants.PEOLINE;
    private final static Pattern LOGINP = Pattern.compile(LOGIN, Pattern.CASE_INSENSITIVE);
    private final static Pattern APPENDP = Pattern.compile(APPEND, Pattern.CASE_INSENSITIVE);
    private final static Pattern FETCHP = Pattern.compile(FETCH, Pattern.CASE_INSENSITIVE);
    private final static Pattern LOGOUTP = Pattern.compile(LOGOUT, Pattern.CASE_INSENSITIVE);

    /* (server) reply codes */
    private final static String SERVICEOPEN = "\\* OK.+?" + Constants.PEOLINE;
    private final static String SERVICECLOSE = "\\* BYE.+?" + Constants.PEOLINE;
    private final static String CONTREQ = CONT_FLAG + ".*?" + Constants.PEOLINE;
    /* DATAOK = fetch ok; msg follows */
    private final static String DATAOK = MSEQ_NO + " " + FETCH_KEY + ".+?" + SZVAL + Constants.PEOLINE;
    /* EODATA = fetch cmd ack; msg sent */
    private final static String EODATA = TAG_ID + " OK( UID)??( " + FETCH_KEY + ")??.*?" + Constants.PEOLINE;
    protected final static Pattern SERVICEOPENP = Pattern.compile(SERVICEOPEN, Pattern.CASE_INSENSITIVE);
    private final static Pattern SERVICECLOSEP = Pattern.compile(SERVICECLOSE, Pattern.CASE_INSENSITIVE);
    private final static Pattern CONTREQP = Pattern.compile(CONTREQ);
    private final static Pattern DATAOKP = Pattern.compile(DATAOK, Pattern.CASE_INSENSITIVE);
    private final static Pattern EODATAP = Pattern.compile(EODATA, Pattern.CASE_INSENSITIVE);

    private final static String OLWSPQUOTE = Constants.LWSP + "+?(\")??";
    private final static String CLWSPQUOTE = "(\")??" + Constants.LWSP + "+?";
    private final static Pattern OLWSPQUOTEP = Pattern.compile(OLWSPQUOTE);
    private final static Pattern CLWSPQUOTEP = Pattern.compile(CLWSPQUOTE);

    /* (pseudo-server) reply codes */
    private final static byte CMDCONTREQBA[] = { '+', ' ', 'R', 'e', 'a', 'd', 'y', 13, 10 };
    private final static byte APPENDDATAERRBA[] = { ' ', 'N', 'O', ' ', 'A', 'P', 'P', 'E', 'N', 'D', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e', 'n', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', 13, 10 };
    private final static byte FETCHDATAERRBA[] = { ' ', 'N', 'O', ' ', 'F', 'E', 'T', 'C', 'H', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e', 'n', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', 13, 10 };
    private final static byte APPENDTOOLARGEERRBA[] = { ' ', 'N', 'O', ' ', 'A', 'P', 'P', 'E', 'N', 'D', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'e', 'x', 'c', 'e', 'e', 'd', 's', ' ', 'm', 'a', 'x', 'i', 'm', 'u', 'm', ' ', 's', 'i', 'z', 'e', ' ', 'l', 'i', 'm', 'i', 't', 13, 10 };
    private final static byte FETCHTOOLARGEERRBA[] = { ' ', 'N', 'O', ' ', 'F', 'E', 'T', 'C', 'H', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'e', 'x', 'c', 'e', 'e', 'd', 's', ' ', 'm', 'a', 'x', 'i', 'm', 'u', 'm', ' ', 's', 'i', 'z', 'e', ' ', 'l', 'i', 'm', 'i', 't', 13, 10 };

    /* cmds */
    private final static int APPEND_VAL = 0;
    private final static int FETCH_VAL = 1;
    private final static int LOGIN_VAL = 2;
    private final static int LOGOUT_VAL = 3;

    private final static Integer APPEND_INT = new Integer(APPEND_VAL);
    private final static Integer FETCH_INT = new Integer(FETCH_VAL);
    private final static Integer LOGIN_INT = new Integer(LOGIN_VAL);
    private final static Integer LOGOUT_INT = new Integer(LOGOUT_VAL);

    /* replies */
    private final static int CONTREQ_VAL = 0;
    private final static int DATAOK_VAL = 1;

    private final static Integer CONTREQ_INT = new Integer(CONTREQ_VAL);
    private final static Integer DATAOK_INT = new Integer(DATAOK_VAL);

    /* do not care cmd or reply */
    private final static int DNC_VAL = -1; /* do not care */

    private final static Integer DNC_INT = new Integer(DNC_VAL);

    /* class variables */

    /* instance variables */
    private ByteBuffer zCmdContReq;
    private ByteBuffer zAppendDataERR;
    private ByteBuffer zFetchDataERR;
    private ByteBuffer zAppendTooLargeERR;
    private ByteBuffer zFetchTooLargeERR;

    private CBufferWrapper zDataOK;
    private ArrayList zEODatas;
    private String zTagId;
    private boolean bUseAppendCmd; /* append = true, fetch = false (default) */
    private boolean bReadAppendData;
    private boolean bReadFetchData;

    /* constructors */
    public IMAPHandler()
    {
        super();

        zCmdContReq = ByteBuffer.wrap(CMDCONTREQBA, CMDCONTREQBA.length, 0);
        zAppendDataERR = ByteBuffer.wrap(APPENDDATAERRBA, APPENDDATAERRBA.length, 0);
        zFetchDataERR = ByteBuffer.wrap(FETCHDATAERRBA, FETCHDATAERRBA.length, 0);
        zAppendTooLargeERR = ByteBuffer.wrap(APPENDTOOLARGEERRBA, APPENDTOOLARGEERRBA.length, 0);
        zFetchTooLargeERR = ByteBuffer.wrap(FETCHTOOLARGEERRBA, FETCHTOOLARGEERRBA.length, 0);

        setup(true);
    }

    /* public methods */
    public void setOptions(XMailScannerCache zXMSCache)
    {
        zPostmaster = zXMSCache.getIMAP4Postmaster();
        iMsgSzLimit = zXMSCache.getMsgSzLimit();
        bReturnErr = zXMSCache.getReturnErrOnIMAP4Block();
        return;
    }

    /* for IMAP4,
     * each message transaction involves one cmd/reply sequence
     * - we intercept fetch message
     *   (during checkReply, we intercept reply from passenger),
     *   filter message, and
     *   resend message
     *   (during checkReply, we resend reply to driver)
     * - we intercept append message
     *   (during checkCmd, we intercept cmd from driver),
     *   filter message, and
     *   resend message
     *   (during checkCmd, we resend reply to passenger)
     */
    /* IMAP4 client */
    public void checkCmd(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException, ParseException
    {
        if (false == bReadAppendData)
        {
            if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
            {
                /* line is not complete - get rest of line */
                zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
                return;
            }
            /* else line is now complete */
        }
        else
        {
            /* note that for append data,
             * we do not search for EOData to determine end of data;
             * instead, we monitor read data byte count
             */
            if (false == readData(zEvent, zEnv, zMsgDatas, Constants.EOLINEFEEDP, null) &&
                (zEnv.getReadDataCt() < zReadDataSz.intValue()))
            {
                if (Constants.NO_MSGSZ_LIMIT == iReadDataLimit ||
                    zEnv.getReadDataCt() < iReadDataLimit)
                {
                    //zLog.debug("read more (cmd): " + zEnv.getReadDataCt() + ", " + iReadDataLimit);
                    return; /* data is not complete - get rest of append data */
                }

                /* after we've collected enough of message
                 * (to build warning that we issue when we reject message),
                 * we selectively buffer/discard parts of message
                 * that we've already read
                 * as well as parts that follow
                 */
                if (false == bRejectData)
                {
                    setupWarning(zEnv);
                    bRejectData = true; /* reject message data */
                }
                rejectData();
                //zLog.debug("read more reject (cmd): " + zEnv.getReadDataCt() + ", " + iReadDataLimit);
                return; /* data is not complete - get rest of append data */
            }
            /* else data is now complete */

            if (false == bRejectData &&
                Constants.NO_MSGSZ_LIMIT != iReadDataLimit &&
                zEnv.getReadDataCt() >= iReadDataLimit)
            {
                /* message had been under size limit but
                 * now that data is complete,
                 * message exceeds size limit
                 */
                setupWarning(zEnv);
                bRejectData = true; /* reject message data */
                rejectData();
            }
            /* else data is complete and message doesn't exceed size limit */

            if (false == bRejectData)
            {
                setAppendEOData(zEnv);
            }
            else
            {
                rejectAppendEOData(zEvent, zEnv); /* reject remaining data */
            }
            return;
        }

        CBufferWrapper zCLine = zEnv.getReadCLine();
        ArrayList zStatePairs = zStateMachine.get();
        zLog.debug("check cmd: " + zCLine + ", state machine: " + zStatePairs.size() + " states");

        StatePair zStatePair;

        for (Iterator zIter = zStatePairs.iterator(); true == zIter.hasNext(); )
        {
            zStatePair = (StatePair) zIter.next();
            switch(zStatePair.getCmd().intValue())
            {
            case APPEND_VAL:
                zLog.debug("append cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, APPENDP))
                {
                    setAppendData(zEnv, zCLine);
                    return;
                }

                break;

            case FETCH_VAL:
                zLog.debug("fetch cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, FETCHP))
                {
                    setFetch(zEnv, zCLine);
                    return;
                }

                break;

            case LOGOUT_VAL:
                zLog.debug("logout cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, LOGOUTP))
                {
                    ackLogout(zEnv);
                    return;
                }

                break;

            case LOGIN_VAL:
                zLog.debug("login cmd?: " + zStatePair.getCmd());
                /* only LOGIN cmd includes username - strip out username */
                if (true == checkUserName(zEnv, zCLine, LOGINP))
                {
                    return;
                }

                break;

            default:
            case DNC_VAL:
                zLog.debug("do not care cmd: " + zStatePair.getCmd());
                /* note that unlike POP3,
                 * NOOP cmd in IMAP4 serves an indirect purpose:
                 * - can be periodic new message poll
                 * - can update message status during period of inactivity
                 * - can reset inactivity autologout timer on server
                 * so we'll pass NOOP cmd through
                 */
                zEnv.setFixedResult(Constants.PASS_THROUGH);
                return;
            }
        }

        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    /* IMAP4 server */
    public void checkReply(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException, ParseException
    {
        if (false == bReadFetchData)
        {
            if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
            {
                /* line is not complete - get rest of line */
                zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
                return;
            }
            /* else line is now complete */
        }
        else
        {
            if (false == readData(zEvent, zEnv, zMsgDatas, Constants.EOLINEFEEDP, EODATAP))
            {
                if (Constants.NO_MSGSZ_LIMIT == iReadDataLimit ||
                    zEnv.getReadDataCt() < iReadDataLimit)
                {
                    //zLog.debug("read more (reply): " + zEnv.getReadDataCt() + ", " + iReadDataLimit);
                    return; /* data is not complete - get rest of fetch data */
                }

                /* after we've collected enough of message
                 * (to build warning that we issue when we reject message),
                 * we selectively buffer/discard parts of message
                 * that we've already read
                 * as well as parts that follow
                 */
                if (false == bRejectData)
                {
                    setupWarning(zEnv);
                    bRejectData = true; /* reject message data */
                }
                rejectData();
                //zLog.debug("read more reject (reply): " + zEnv.getReadDataCt() + ", " + iReadDataLimit);
                return; /* data is not complete - get rest of fetch data */
            }
            /* else data is now complete */

            if (false == bRejectData &&
                Constants.NO_MSGSZ_LIMIT != iReadDataLimit &&
                zEnv.getReadDataCt() >= iReadDataLimit)
            {
                /* message had been under size limit but
                 * now that data is complete,
                 * message exceeds size limit
                 */
                setupWarning(zEnv);
                bRejectData = true; /* reject message data */
                rejectData();
            }
            /* else data is complete and message doesn't exceed size limit */

            if (false == bRejectData)
            {
                setFetchEOData(zEnv);
            }
            else
            {
                rejectFetchEOData(zEvent, zEnv); /* reject remaining data */
            }
            return;
        }

        CBufferWrapper zCLine = zEnv.getReadCLine();
        ArrayList zStatePairs = zStateMachine.get();
        zLog.debug("check reply: " + zCLine + ", state machine: " + zStatePairs.size() + " states");

        StatePair zStatePair;

        for (Iterator zIter = zStatePairs.iterator(); true == zIter.hasNext(); )
        {
            zStatePair = (StatePair) zIter.next();
            switch(zStatePair.getReply().intValue())
            {
            case CONTREQ_VAL:
                zLog.debug("cmd continuation request reply?: " + zStatePair.getReply());
                if (true == isMatch(zCLine, CONTREQP))
                {
                    unsetAppendData(zEnv);
                    return;
                }

                break;

            case DATAOK_VAL:
                zLog.debug("data ok reply?: " + zStatePair.getReply());
                if (true == isMatch(zCLine, DATAOKP))
                {
                    setFetchData(zEnv, zCLine);
                    return;
                }

                break;

            default:
            case DNC_VAL:
                zLog.debug("do not care reply: " + zStatePair.getReply());
                zEnv.setFixedResult(Constants.PASS_THROUGH);
                return;
            }
        }

        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    public void block(XMSEnv zEnv, boolean bCopy, int iType)
    {
        zLog.debug("block message");

        ByteBuffer zErrLine;

        if (true == bReturnErr)
        {
            zErrLine = ByteBuffer.allocate(READSZ);
            byte azTagId[] = zTagId.getBytes();
            ByteBuffer zTmp = ByteBuffer.wrap(azTagId);
            //zTmp.flip();
            zErrLine.put(zTmp);

            if (true == bUseAppendCmd)
            {
                zAppendDataERR.rewind();
                zErrLine.put(zAppendDataERR);
            }
            else
            {
                zFetchDataERR.rewind();
                zErrLine.put(zFetchDataERR);
            }

            /* we set limit since we didn't allocate buffer to exact size */
            zErrLine.limit(zErrLine.position());
        }
        else
        {
            zErrLine = null;
        }

        block(zEnv, zErrLine, bCopy, iType);
        /* we'll setup again during resend */
        return;
    }

    public boolean resend(XMSEnv zEnv)
    {
        if (null == zMsg ||
            true == zMsg.isEmpty())
        {
            zLog.debug("message is empty");
            /* during block,
             * we've set return result already so we can return now
             */
            setup(false);
            return true;
        }

        /* retrieve actual message size and
         * not expected message size
         * (that we used to monitor read data byte count)
         */
        int iMsgSz = zMsg.getSize();
        /* forcibly recalculate message size - we may have modified message */
        zMsg.clearSize();
        int iNewMsgSz = zMsg.getSize();

        ByteBuffer zLine;

        if (iMsgSz != iNewMsgSz)
        {
            zLog.debug("orig size: " + iMsgSz + ", new size: " + iNewMsgSz);

            /* rebuild retrieve ok reply - message size has changed */
            String zMsgSz = String.valueOf(iMsgSz);
            Pattern zMsgSzP = Pattern.compile(zMsgSz);
            Matcher zMatcher = zMsgSzP.matcher(zDataOK.toString());

            String zNewDataOK = zMatcher.replaceAll(String.valueOf(iNewMsgSz));
            CharBuffer zCBLine = CharBuffer.wrap(zNewDataOK);
            try
            {
                zLine = Constants.CHARSET.newEncoder().encode(zCBLine);
                zLine.position(zLine.limit()); /* set position to indicate that ByteBuffer contains data */
            }
            catch (CharacterCodingException e)
            {
                zLog.error("Unable to encode line: " + zNewDataOK + " : " + e);
                return false;
            }
        }
        else
        {
            zLine = zDataOK.get();
        }

        zLog.debug("(SODATA): " + zLine);

        /* resend message data */
        if (true == bUseAppendCmd)
        {
            zStateMachine.reset(DNC_INT, CONTREQ_INT);

            zEnv.sendToPassenger(zLine);
            zEnv.resetReadCLine();
        }
        else
        {
            //zLog.debug("resend message: " + zMsg);
            zLog.debug("(EODATA): " + zEODatas);

            zEnv.sendToDriver(zLine);
            zEnv.convertToDriver(zMsgDatas);
            zEnv.sendToDriver(zEODatas);
            zEnv.resetReadCLine();

            flushMsg(zEnv);
            setup(false); /* since we're sending EODATA, we'll setup again */
        }

        return true;
    }

    public void setup()
    {
        setup(false);
        return;
    }

    /* private methods */
    private void setup(boolean bCheckUserName)
    {
        zMsgInfo = null;
        zMsgDatas = null;
        zReadDataSz = null;
        iReadDataLimit = Constants.NO_MSGSZ_LIMIT;
        iRejectedCt = 0;
        bRejectData = false; /* accept message data */

        zDataOK = null;
        zEODatas = null;
        zTagId = null;
        bUseAppendCmd = false; /* use fetch cmd mode (default) */
        bReadAppendData = false; /* use read line mode for append */
        bReadFetchData = false; /* use read line mode for fetch */

        /* in order of most likely to least likely to occur
         * - we are either intercepting or passing these cmds through
         *   so we don't care about replies yet
         */
        zStateMachine.reset(FETCH_INT, DNC_INT);
        zStateMachine.set(APPEND_INT, DNC_INT);
        zStateMachine.set(LOGOUT_INT, DNC_INT);

        if (false == bCheckUserName)
        {
            zStateMachine.set(DNC_INT, DNC_INT);
        }
        else
        {
            /* temporarily add these cmds to state machine
             * so that we can grab username info if available
             * - we'll reset state machine
             *   after we begin 1st message transaction
             */
            zStateMachine.set(LOGIN_INT, DNC_INT);
            zStateMachine.set(DNC_INT, DNC_INT);
        }

        iSendsMsg = PASSENGER; /* restore default */
        return;
    }

    /* RFC 1730,2060:
     * FETCH msg
     */
    private void setFetch(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("fetch");

        /* we are passing FETCH through so we do not need to save it
         * (but copy its tag id because we may need to use tag id later)
         */
        Matcher zMatcher = TAG_IDP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            zTagId = zMatcher.group();
        }
        else
        {
            /* should never occur
             * (if tag id is missing,
             *  then we should not have reached here)
             */
            zLog.error("Unable to locate tag id in fetch cmd: " + zCLine);
        }

        /* prepare for reply (containing messages) from passenger */
        zStateMachine.reset(DNC_INT, DATAOK_INT);

        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    /* RFC 1730,2060:
     * LOGIN name
     */
    private boolean checkUserName(XMSEnv zEnv, CBufferWrapper zCLine, Pattern zPattern)
    {
        Matcher zMatcher = zPattern.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (false == saveUserName(zCLine))
            {
                return false;
            }

            zEnv.setFixedResult(Constants.PASS_THROUGH);

            setup(false); /* we got user name info so setup again */
            return true;
        }

        return false;
    }

    private boolean saveUserName(CBufferWrapper zCLine)
    {
        ByteBuffer zLine = zCLine.get();
        int iPosition = zLine.position();
        int iLimit = zLine.limit();

        Matcher zMatcher = LOGIN_KEYP.matcher(zCLine);
        if (false == zMatcher.find())
        {
            /* should never occur
             * (if LOGIN cmd is missing,
             *  then we should not have reached here)
             */
            zLog.error("Unable to locate login key in LOGIN cmd: " + zCLine);
            return false;
        }

        int iStart = zMatcher.end();
        int iSize = iPosition - iStart;
        zLine.position(iStart);
        zLine.limit(iPosition);

        ByteBuffer zTmp = ByteBuffer.allocate(iSize);
        zTmp.put(zLine); /* we don't set limit since we allocated buffer to exact size */

        zLine = zTmp;
        zCDummy.renew(zLine);

        zMatcher = OLWSPQUOTEP.matcher(zCDummy);
        if (false == zMatcher.find())
        {
            zLog.error("Unable to locate user name in LOGIN cmd (start parse error): " + zCLine);
            return false;
        }

        iStart = zMatcher.end();
        iSize = iSize - iStart;
        zLine.position(iStart);

        zTmp = ByteBuffer.allocate(iSize);
        zTmp.put(zLine); /* we don't set limit since we allocated buffer to exact size */

        zLine = zTmp;
        zCDummy.renew(zLine);

        zMatcher = CLWSPQUOTEP.matcher(zCDummy);
        if (false == zMatcher.find())
        {
            zLog.error("Unable to locate user name in LOGIN cmd (end parse error): " + zCLine);
            return false;
        }

        zUserName = (CBufferWrapper) zCDummy.subSequence(iStart, zMatcher.start());
        zHdlInfo.setUserName(zUserName);
        zLog.debug("user name is " + zUserName);
        return true;
    }

    /* RFC 1730,2060:
     * LOGOUT
     */
    private void ackLogout(XMSEnv zEnv)
    {
        zLog.debug("logout");
        zEnv.setFixedResult(Constants.PASS_THROUGH);

        if (null != zMsg)
        {
            flushMsg(zEnv);
            zMsg = null;
        }
        setup(false); /* not really necessary to setup again but ... */
        return;
    }

    private void setAppendData(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("append data");
        if (null == zMsg)
        {
            zMsg = new MLMessage();
        }
        /* else recycle original MLMessage */

        bUseAppendCmd = true; /* use append cmd mode */
        /* in read data mode,
         * we will not receive any more replies from passenger
         * until we have sent data to passenger
         */
        bReadAppendData = true; /* use read data mode */

        /* we are not passing APPEND through yet
         * (but copy its tag id because we may need to use tag id later)
         */
        Matcher zMatcher = TAG_IDP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            zTagId = zMatcher.group();
        }
        else
        {
            /* should never occur
             * (if tag id is missing,
             *  then we should not have reached here)
             */
            zLog.error("Unable to locate tag id in append cmd: " + zCLine);
        }

        /* since we're buffering message, passenger does not need cmd yet
         * - intercept/save cmd (we'll resend it)
         */
        saveDataOK(zCLine);
        if (zReadDataSz.intValue() > iMsgSzLimit)
        {
            /* if message exceeds size limit,
             * we'll mark message for rejection later
             * after we've collected enough of message
             * to parse message header
             * to report log event
             * (and if necessary, to build warning message)
             */
            iReadDataLimit = Constants.MSGSZ_MIN;
        }

        /* although it's not documented,
         * driver sends empty line immediately after append cmd + DATA
         * so consider empty line part of DATA
         * - setAppendEOData will strip out empty line and
         *   save it for later
         */
        zReadDataSz = new Integer(zReadDataSz.intValue() + Constants.PCRLF.length());

        zStateMachine.set(DNC_INT, DNC_INT);

        zMsgDatas = zMsg.getData();

        /* although it's not documented,
         * passenger sends cmd continuation request immediately after append cmd
         * (driver waits for cmd continuation request before it sends DATA)
         * - we'll prompt for DATA by sending cmd continuation request
         */
        zEnv.sendToDriver(zCmdContReq); /* ack with pseudo-server reply (+) */
        zEnv.clearReadCLine();
        return;
    }

    private void setFetchData(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("fetch data");
        if (null == zMsg)
        {
            zMsg = new MLMessage();
        }
        /* else recycle original MLMessage */

        /* in read data mode,
         * we will not receive any more cmds from driver
         * until we have received data and EODATA from passenger
         */
        bReadFetchData = true; /* use read data mode */

        /* since we're buffering message, driver does not need reply yet
         * - intercept/save reply (we'll resend it later)
         */
        saveDataOK(zCLine);
        if (zReadDataSz.intValue() > iMsgSzLimit)
        {
            /* if message exceeds size limit,
             * we'll mark message for rejection later
             * after we've collected enough of message
             * to parse message header
             * to report log event
             * (and if necessary, to build warning message)
             */
            iReadDataLimit = Constants.MSGSZ_MIN;
        }

        zStateMachine.reset(DNC_INT, DNC_INT);

        zMsgDatas = zMsg.getData();

        zEnv.clearReadCLine();
        return;
    }

    private void saveDataOK(CBufferWrapper zCLine)
    {
        /* save data ok cmd/reply
         * - we may need to modify cmd/reply contents later (e.g., size info)
         *   (IMAP4 append cmd/fetch cmd reply has dynamic/variable syntax
         *    (unlike POP3 retrieve cmd reply which has fixed syntax))
         * - note that fetch ok reply is never embedded with message
         */
        zDataOK = zCLine;

        /* also, save expected message size */
        Matcher zMatcher = SZVALP.matcher(zDataOK);
        if (false == zMatcher.find())
        {
            /* should never occur
             * (if size fragment is missing,
             *  then we should not have reached here)
             */
            zLog.error("Unable to locate fragment specifying size of message in fetch/append ok reply: " + zDataOK);
            return;
        }

        String zSzVal = zMatcher.group();
        zMatcher = DIGVALP.matcher(zSzVal);
        if (false == zMatcher.find())
        {
            /* should never occur
             * (if size value is missing,
             *  then we should not have reached here)
             */
            zLog.error("Unable to locate size value of message in fetch/append ok reply: " + zDataOK);
            return;
        }

        String zDigVal = zMatcher.group();
        zMsg.setSize(zDigVal);
        zReadDataSz = new Integer(zMsg.getSize());
        return;
    }

    private void setAppendEOData(XMSEnv zEnv) throws ParseException
    {
        zLog.debug("append end of data");

        /* we're buffering message; driver does not need data yet */
        bReadAppendData = false; /* use read line mode */

        if (true == zMsgDatas.isEmpty())
        {
            zLog.warn("retrieved empty append message");
            zEnv.resetReadCLine();
            setup(false);
            return;
        }

        /* we'll restore EODATA in resend
         * (stripEOD determines position of EODATA from message size)
         * - note that we get back everything that we need
         *   including all EOLINEs and anything else at end-of-data
         */
        zEODatas = stripEOD();

        /* we will not send cmd (containing message) yet
         * - we'll send cmd to passenger later
         *   (after we have scanned this message and
         *    are ready to resend this message to passenger)
         */
        zEnv.clearReadCLine();

        zMsg.parse(true);
        zMsgInfo = new MLMessageInfo(zHdlInfo, zEnv, zMsg);
        //zLog.debug("message: " + zMsg);
        iSendsMsg = DRIVER; /* client copies msg to server */
        return;
    }

    private void setFetchEOData(XMSEnv zEnv) throws ParseException
    {
        zLog.debug("fetch end of data");
        /* we're buffering message; driver does not need data yet */
        bReadFetchData = false; /* use read line mode */

        if (true == zMsgDatas.isEmpty())
        {
            zLog.warn("retrieved empty fetch message");
            zEnv.resetReadCLine();
            setup(false);
            return;
        }

        /* we'll restore EODATA in resend
         * (stripEOD determines position of EODATA from message size)
         *  - for fetch cmd,
         *    we can mimic append cmd and
         *    monitor byte count of message as we read data but
         *    since we don't know EODATA length
         *    (e.g., EODATA sequence is not fixed/static):
         *    - we'd need to use another state to collect EODATA
         *    - also, EODATA may start on text line that
         *      contains end of message and
         *      continue on one or more text lines
         *  - thus, for simplicity,
         *    we strip EODATA after we have read data and EODATA
         */
        zEODatas = stripEOD();

        /* we will not send reply (containing message) yet
         * - we'll send reply to driver later
         *   (after we have scanned this message and
         *    are ready to resend this message to driver)
         */
        zEnv.clearReadCLine();

        zMsg.parse(true);
        zMsgInfo = new MLMessageInfo(zHdlInfo, zEnv, zMsg);
        //zLog.debug("message: " + zMsg);
        iSendsMsg = PASSENGER; /* client retrieves msg from server */
        return;
    }

    private void rejectAppendEOData(TCPChunkEvent zEvent, XMSEnv zEnv)
    {
        bReadAppendData = false; /* use read line mode */
        /* not necessary since we immediately resend message
         * but for clarity, we identify endpoint that sends message
         * - client copies msg to server
         */
        iSendsMsg = DRIVER;
        rejectEOData(zEvent, zEnv);
        return;
    }

    private void rejectFetchEOData(TCPChunkEvent zEvent, XMSEnv zEnv)
    {
        bReadFetchData = false; /* use read line mode */
        /* not necessary since we immediately resend message
         * but for clarity, we identify endpoint that sends message
         * - client retrieves msg from server
         */
        iSendsMsg = PASSENGER;
        rejectEOData(zEvent, zEnv);
        return;
    }

    private void rejectEOData(TCPChunkEvent zEvent, XMSEnv zEnv)
    {
        zLog.debug("end of data (reject message)");
        //zLog.debug("end of data (reject message): " + zEnv.getReadDataCt() + " bytes exceeds size limit, " + iMsgSzLimit + " bytes, " + zMsgDatas);

        ByteBuffer zErrLine;

        if (true == bReturnErr)
        {
            /* we will not ack EODATA with pseudo-server reply (OK)
             * - instead, we'll reject message with pseudo-server reply (NO)
             */
            zErrLine = ByteBuffer.allocate(READSZ);
            byte azTagId[] = zTagId.getBytes();
            ByteBuffer zTmp = ByteBuffer.wrap(azTagId);
            //zTmp.flip();
            zErrLine.put(zTmp);

            if (true == bUseAppendCmd)
            {
                zAppendTooLargeERR.rewind();
                zErrLine.put(zAppendTooLargeERR);
            }
            else
            {
                zFetchTooLargeERR.rewind();
                zErrLine.put(zFetchTooLargeERR);
            }

            /* we set limit since we didn't allocate buffer to exact size */
            zErrLine.limit(zErrLine.position());
        }
        else
        {
            zErrLine = null;

            /* we'll restore EODATA in resend
             * (stripEOD determines position of EODATA from message size)
             * - note that we get back everything that we need
             *   including all EOLINEs and anything else at end-of-data
             */
            zEODatas = stripEOD();

            /* we will not send cmd (containing message) yet
             * - we'll send cmd to passenger later
             *   (after we have scanned this message and
             *    are ready to resend this message to passenger)
             */
            zEnv.clearReadCLine();
        }

        rejectEOData(zEvent, zEnv, zErrLine, Constants.REJECT);
        /* we do not parse or process message
         * because we've already parsed message header and
         * we've already determined that we must reject message
         * - therefore, we immediately resend message
         */
        resend(zEnv); /* we'll setup again during resend */
        return;
    }

    private void unsetAppendData(XMSEnv zEnv)
    {
        zLog.debug("append all data");
        zStateMachine.reset(DNC_INT, DNC_INT);

        //zLog.debug("resend message: " + zMsg);
        zLog.debug("(EODATA): " + zEODatas);

        /* resend message data */
        zEnv.convertToPassenger(zMsgDatas);
        zEnv.sendToPassenger(zEODatas);

        zEnv.resetReadCLine();

        flushMsg(zEnv);
        setup(false); /* since we're sending data, we'll setup again */
        return;
    }
}
