/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
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

/* POP3 - RFC1939
 *
 * octet = 8-bit byte
 */
public class POPHandler extends MLHandler
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(POPHandler.class.getName());

    private final static String DIGVAL = "(\\p{Digit})++";
    private final static String SZVAL = DIGVAL + " octets";
    private final static Pattern DIGVALP = Pattern.compile(DIGVAL);
    private final static Pattern SZVALP = Pattern.compile(SZVAL);

    private final static String OK = "+OK ";
    private final static String OKREPLY = "^\\" + OK;

    /* (client) POP commands - ignore other commands */
    private final static String USER = "^USER ";
    private final static String PASSWD = "^PASS ";
    private final static String APOP = "^APOP ";
    private final static String RETR = "^RETR ";
    private final static String NOOP = "^NOOP" + Constants.PEOLINE;
    private final static String QUIT = "^QUIT" + Constants.PEOLINE;
    private final static Pattern USERP = Pattern.compile(USER, Pattern.CASE_INSENSITIVE);
    private final static Pattern PASSWDP = Pattern.compile(PASSWD, Pattern.CASE_INSENSITIVE);
    private final static Pattern APOPP = Pattern.compile(APOP, Pattern.CASE_INSENSITIVE);
    private final static Pattern RETRP = Pattern.compile(RETR, Pattern.CASE_INSENSITIVE);
    private final static Pattern NOOPP = Pattern.compile(NOOP, Pattern.CASE_INSENSITIVE);
    private final static Pattern QUITP = Pattern.compile(QUIT, Pattern.CASE_INSENSITIVE);

    /* (server) reply codes */
    private final static String SERVICEOPEN = OKREPLY;
    private final static String SERVICECLOSE = OKREPLY;
    /* DATAOK = retrieve ok; msg follows */
    private final static String DATAOK = OKREPLY + SZVAL + "[^" + Constants.PEOLINE + "]*" + Constants.PEOLINE;
    /* EODATA = retrieve cmd ack; msg sent */
    private final static String EODATA = "(^|" + Constants.PEOLINE + ")\\." + Constants.PEOLINE;
    protected final static Pattern SERVICEOPENP = Pattern.compile(SERVICEOPEN, Pattern.CASE_INSENSITIVE);
    private final static Pattern SERVICECLOSEP = Pattern.compile(SERVICECLOSE, Pattern.CASE_INSENSITIVE);
    private final static Pattern DATAOKP = Pattern.compile(DATAOK, Pattern.CASE_INSENSITIVE);
    private final static Pattern EODATAP = Pattern.compile(EODATA);

    /* +OK <msg sz> octets */
    private final static String OCTETS = " octets" + Constants.PCRLF;

    /* (pseudo-server) reply codes */
    /* +OK */
    private final static byte CMDOKBA[] = { '+', 'O', 'K', ' ', 13, 10 };
    /* -ERR */
    private final static byte DATAERRBA[] = { '-', 'E', 'R', 'R', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e', 'n', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', 13, 10 };
    private final static byte TOOLARGEERRBA[] = { '-', 'E', 'R', 'R', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'e', 'x', 'c', 'e', 'e', 'd', 's', ' ', 'm', 'a', 'x', 'i', 'm', 'u', 'm', ' ', 's', 'i', 'z', 'e', ' ', 'l', 'i', 'm', 'i', 't', 13, 10 };

    private final static String LWSPEOL = "(" + Constants.LWSP + "|" + Constants.PEOLINE + ")";
    private final static Pattern LWSPEOLP = Pattern.compile(LWSPEOL);

    /* cmds */
    private final static int RETR_VAL = 0;
    private final static int NOOP_VAL = 1;
    private final static int USER_VAL = 2;
    private final static int APOP_VAL = 3;
    private final static int QUIT_VAL = 4;

    private final static Integer RETR_INT = new Integer(RETR_VAL);
    private final static Integer NOOP_INT = new Integer(NOOP_VAL);
    private final static Integer USER_INT = new Integer(USER_VAL);
    private final static Integer APOP_INT = new Integer(APOP_VAL);
    private final static Integer QUIT_INT = new Integer(QUIT_VAL);

    /* replies */
    private final static int DATAOK_VAL = 0;

    private final static Integer DATAOK_INT = new Integer(DATAOK_VAL);

    /* do not care cmd or reply */
    private final static int DNC_VAL = -1;

    private final static Integer DNC_INT = new Integer(DNC_VAL);

    /* class variables */

    /* instance variables */
    private ByteBuffer zCmdOK;
    private ByteBuffer zDataERR;
    private ByteBuffer zTooLargeERR;

    private CBufferWrapper zDataOK;
    private ByteBuffer zEOData;
    private boolean bReadData;

    /* constructors */
    public POPHandler()
    {
        super();

        zCmdOK = ByteBuffer.wrap(CMDOKBA, CMDOKBA.length, 0);
        zDataERR = ByteBuffer.wrap(DATAERRBA, DATAERRBA.length, 0);
        zTooLargeERR = ByteBuffer.wrap(TOOLARGEERRBA, TOOLARGEERRBA.length, 0);

        setup(true);
    }

    /* public methods */
    public void setOptions(XMailScannerCache zXMSCache)
    {
        zPostmaster = zXMSCache.getPOP3Postmaster();
        iMsgSzLimit = zXMSCache.getMsgSzLimit();
        bReturnErr = zXMSCache.getReturnErrOnPOP3Block();
        return;
    }

    /* for POP3,
     * each message transaction involves one cmd/reply sequence
     * - we intercept retrieve message
     *   (during checkReply, we intercept reply from passenger),
     *   filter message, and
     *   resend message
     *   (during checkReply, we resend reply to driver)
     */
    /* POP3 client */
    public void checkCmd(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException
    {
        if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
        {
            /* line is not complete - get rest of line */
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
            return;
        }
        /* else line is now complete */

        CBufferWrapper zCLine = zEnv.getReadCLine();
        ArrayList zStatePairs = zStateMachine.get();
        zLog.debug("check cmd: " + zCLine + ", state machine: " + zStatePairs.size() + " states");

        StatePair zStatePair;

        for (Iterator zIter = zStatePairs.iterator(); true == zIter.hasNext(); )
        {
            zStatePair = (StatePair) zIter.next();
            switch(zStatePair.getCmd().intValue())
            {
            case RETR_VAL:
                zLog.debug("retrieve cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, RETRP))
                {
                    setRetrieve(zEnv, zCLine);
                    return;
                }

                break;

            case NOOP_VAL:
                zLog.debug("noop cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, NOOPP))
                {
                    ackNoop(zEnv);
                    return;
                }

                break;

            case QUIT_VAL:
                zLog.debug("quit cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, QUITP))
                {
                    ackQuit(zEnv);
                    return;
                }

                break;

            case USER_VAL:
            case APOP_VAL:
                zLog.debug("username or apop cmd?: " + zStatePair.getCmd());
                /* USER and APOP cmds include username - strip out username */
                if (true == checkUserName(zEnv, zCLine, USERP) ||
                    true == checkUserName(zEnv, zCLine, APOPP))
                {
                    return;
                }

                break;

            default:
            case DNC_VAL:
                zLog.debug("do not care cmd: " + zStatePair.getCmd());
                zEnv.setFixedResult(Constants.PASS_THROUGH);
                return;
            }
        }

        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    /* POP3 server */
    public void checkReply(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException, ParseException
    {
        if (false == bReadData)
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
                    return; /* data is not complete - get rest of data */
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
                return; /* data is not complete - get rest of data */
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
                /* forcibly recalculate message size
                 * - we may have modified message
                 *   (e.g., fragmented long lines and
                 *    appended EOL at end of fragments)
                 */
                //zLog.debug("msg size (org): " + zMsg.getSize());
                zMsg.clearSize();
                //zLog.debug("msg size (new): " + zMsg.getSize());
                zMsg.getSize();

                setEOData(zEnv);
            }
            else
            {
                rejectEOData(zEvent, zEnv); /* reject remaining data */
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
            case DATAOK_VAL:
                zLog.debug("data ok reply?: " + zStatePair.getReply());
                if (true == isMatch(zCLine, DATAOKP))
                {
                    setData(zEnv, zCLine);
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
        block(zEnv, zDataERR, bCopy, iType);
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

        /* we add EOLINE in case zMsgDatas doesn't end with EOLINE
         * - we need EOLINE to correctly define zEOData termination
         * - EOLINE may have been present at start of end-of-data but
         *   if SpamAssassin significantly modifies message,
         *   SpamAssassin does not insert EOLINE
         *   at end of each line that it adds
         */
        ByteBuffer zEOLine = ByteBuffer.wrap(Constants.EOLINEBA, Constants.EOLINEBA.length, 0);

        /* create start of data reply */
        int iMsgSz = zMsg.getSize(); /* retrieve actual message size */
        /* forcibly recalculate message size - we may have modified message */
        zMsg.clearSize();
        /* we will be adding EOLINE so take its size into account */
        int iNewMsgSz = zMsg.getSize() + zEOLine.limit();

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
                zLine.position(zLine.limit()); /* set position to indicate that
ByteBuffer contains data */
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
        //zLog.debug("message: " + zMsg);
        zLog.debug("(EODATA): " + zEOData);

        /* resend message data */
        zEnv.sendToDriver(zLine);
        zEnv.convertToDriver(zMsgDatas);
        zEnv.sendToDriver(zEOLine);
        zEnv.sendToDriver(zEOData);
        zEnv.resetReadCLine();

        flushMsg(zEnv); /* flush after we've resent message data */
        setup(false); /* since we're sending EODATA, we'll setup again */
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
        zEOData = null;
        bReadData = false; /* use read line mode */

        /* in order of most likely to least likely to occur
         * - we are either intercepting or passing these cmds through
         *   so we don't care about replies yet
         */
        zStateMachine.reset(RETR_INT, DNC_INT);
        zStateMachine.set(QUIT_INT, DNC_INT);

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
            zStateMachine.set(USER_INT, DNC_INT);
            zStateMachine.set(APOP_INT, DNC_INT);
            zStateMachine.set(DNC_INT, DNC_INT);
        }

        return;
    }

    /* RFC 1939:
     * RETR msg
     *    Arguments:
     *        a message-number (required) which may NOT refer to a
     *        message marked as deleted
     *    Restrictions:
     *        may only be given in the TRANSACTION state
     *    Discussion:
     *        If the POP3 server issues a positive response, then the
     *        response given is multi-line.  After the initial +OK, the
     *        POP3 server sends the message corresponding to the given
     *        message-number, being careful to byte-stuff the termination
     *        character (as with all multi-line responses).
     */
    private void setRetrieve(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("retrieve");
        /* prepare for reply (containing message) from passenger */
        zStateMachine.reset(DNC_INT, DATAOK_INT);

        zEnv.setFixedResult(Constants.PASS_THROUGH);
        return;
    }

    /* RFC 1939:
     * USER name
     *    Arguments:
     *        a string identifying a mailbox (required), which is of
     *        significance ONLY to the server
     *    Restrictions:
     *        may only be given in the AUTHORIZATION state after the POP3
     *        greeting or after an unsuccessful USER or PASS command
     *    Discussion:
     *        To authenticate using the USER and PASS command
     *        combination, the client must first issue the USER
     *        command.  If the POP3 server responds with a positive
     *        status indicator ("+OK"), then the client may issue
     *        either the PASS command to complete the authentication,
     *        or the QUIT command to terminate the POP3 session.  If
     *        the POP3 server responds with a negative status indicator
     *        ("-ERR") to the USER command, then the client may either
     *        issue a new authentication command or may issue the QUIT
     *        command.
     *        The server may return a positive response even though no
     *        such mailbox exists.  The server may return a negative
     *        response if mailbox exists, but does not permit plaintext
     *        password authentication.
     *
     * PASS string
     *    Arguments:
     *        a server/mailbox-specific password (required)
     *    Restrictions:
     *        may only be given in the AUTHORIZATION state immediately
     *        after a successful USER command
     *    Discussion:
     *        When the client issues the PASS command, the POP3 server
     *        uses the argument pair from the USER and PASS commands to
     *        determine if the client should be given access to the
     *        appropriate maildrop.
     *        Since the PASS command has exactly one argument, a POP3
     *        server may treat spaces in the argument as part of the
     *        password, instead of as argument separators.
     *
     * APOP name digest
     *    Arguments:
     *        a string identifying a mailbox and a MD5 digest string
     *        (both required)
     *    Restrictions:
     *        may only be given in the AUTHORIZATION state after the POP3
     *        greeting or after an unsuccessful USER or PASS command
     *    Discussion:
     *        Normally, each POP3 session starts with a USER/PASS
     *        exchange.  This results in a server/user-id specific
     *        password being sent in the clear on the network.  For
     *        intermittent use of POP3, this may not introduce a sizable
     *        risk.  However, many POP3 client implementations connect to
     *        the POP3 server on a regular basis -- to check for new
     *        mail.  Further the interval of session initiation may be on
     *        the order of five minutes.  Hence, the risk of password
     *        capture is greatly enhanced.
     *        An alternate method of authentication is required which
     *        provides for both origin authentication and replay
     *        protection, but which does not involve sending a password
     *        in the clear over the network.  The APOP command provides
     *        this functionality.
     *        When the POP3 server receives the APOP command, it verifies
     *        the digest provided.  If the digest is correct, the POP3
     *        server issues a positive response, and the POP3 session
     *        enters the TRANSACTION state.  Otherwise, a negative
     *        response is issued and the POP3 session remains in the
     *        AUTHORIZATION state.
     *
     * RFC 1734:
     * AUTH mechanism
     *   Arguments:
     *       a string identifying an IMAP4 authentication mechanism,
     *       such as defined by [IMAP4-AUTH].  Any use of the string
     *       "imap" used in a server authentication identity in the
     *       definition of an authentication mechanism is replaced with
     *       the string "pop".
     *   Restrictions:
     *       may only be given in the AUTHORIZATION state
     *   Discussion:
     *       The AUTH command indicates an authentication mechanism to
     *       the server.  If the server supports the requested
     *       authentication mechanism, it performs an authentication
     *       protocol exchange to authenticate and identify the user.
     *       Optionally, it also negotiates a protection mechanism for
     *       subsequent protocol interactions.  If the requested
     *       authentication mechanism is not supported, the server
     *       should reject the AUTH command by sending a negative
     *       response.
     *       The authentication protocol exchange consists of a series
     *       of server challenges and client answers that are specific
     *       to the authentication mechanism.  A server challenge,
     *       otherwise known as a ready response, is a line consisting
     *       of a "+" character followed by a single space and a BASE64
     *       encoded string.  The client answer consists of a line
     *       containing a BASE64 encoded string.  If the client wishes
     *       to cancel an authentication exchange, it should issue a
     *       line with a single "*".  If the server receives such an
     *       answer, it must reject the AUTH command by sending a
     *       negative response.
     *       The server is not required to support any particular
     *       authentication mechanism, nor are authentication mechanisms
     *       required to support any protection mechanisms.  If an AUTH
     *       command fails with a negative response, the session remains
     *       in the AUTHORIZATION state and client may try another
     *       authentication mechanism by issuing another AUTH command,
     *       or may attempt to authenticate by using the USER/PASS or
     *       APOP commands.  In other words, the client may request
     *       authentication types in decreasing order of preference,
     *       with the USER/PASS or APOP command as a last resort.
     *       Should the client successfully complete the authentication
     *       exchange, the POP3 server issues a positive response and
     *       the POP3 session enters the TRANSACTION state.
     */
    private boolean checkUserName(XMSEnv zEnv, CBufferWrapper zCLine, Pattern zPattern)
    {
        Matcher zMatcher = zPattern.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (false == saveUserName(zCLine, zMatcher))
            {
                return false;
            }

            zEnv.setFixedResult(Constants.PASS_THROUGH);

            setup(false); /* we got user name info so setup again */
            return true;
        }

        return false;
    }

    private boolean saveUserName(CBufferWrapper zCLine, Matcher zMatcher)
    {
        int iStart = zMatcher.end();
        zMatcher = LWSPEOLP.matcher(zCLine);
        if (false == zMatcher.find(iStart))
        {
            return false;
        }

        zUserName = (CBufferWrapper) zCLine.subSequence(iStart, zMatcher.start());
        zHdlInfo.setUserName(zUserName);
        zLog.debug("user name is " + zUserName);
        return true;
    }

    private void ackNoop(XMSEnv zEnv)
    {
        zLog.debug("noop");
        /* since passenger will "ignore" anyway, passenger does not need NOOP
         * - intercept NOOP
         */

        zEnv.sendToDriver(zCmdOK); /* ack with pseudo-server reply (+OK) */
        zEnv.resetReadCLine();
        return;
    }

    /* RFC 1939:
     * QUIT
     *    Arguments: none
     *    Restrictions: none
     *    Discussion:
     *        The POP3 server removes all messages marked as deleted
     *        from the maildrop and replies as to the status of this
     *        operation.  If there is an error, such as a resource
     *        shortage, encountered while removing messages, the
     *        maildrop may result in having some or none of the messages
     *        marked as deleted be removed.  In no case may the server
     *        remove any messages not marked as deleted.
     *        Whether the removal was successful or not, the server
     *        then releases any exclusive-access lock on the maildrop
     *        and closes the TCP connection.
     */
    private void ackQuit(XMSEnv zEnv)
    {
        zLog.debug("quit");
        zEnv.setFixedResult(Constants.PASS_THROUGH);

        if (null != zMsg)
        {
            flushMsg(zEnv);
            zMsg = null;
        }
        setup(false); /* not really necessary to setup again but ... */
        return;
    }

    private void setData(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("data");
        if (null == zMsg)
        {
            zMsg = new MLMessage();
        }
        /* else recycle original MLMessage */

        /* in read data mode, we will not receive any more cmds from driver
         * until we have received data and EODATA from passenger
         */
        bReadData = true; /* use read data mode */

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
         *   (POP3 retrieve cmd reply has fixed syntax)
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
            zLog.error("Unable to locate fragment specifying size of message in retrieve ok reply: " + zDataOK);
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
            zLog.error("Unable to locate size value of message in retrieve ok reply: " + zDataOK);
            return;
        }

        String zDigVal = zMatcher.group();
        zMsg.setSize(zDigVal);
        zReadDataSz = new Integer(zMsg.getSize());
        return;
    }

    private void setEOData(XMSEnv zEnv) throws ParseException
    {
        zLog.debug("end of data");
        /* we're buffering message; driver does not need data yet */
        bReadData = false;

        if (true == zMsgDatas.isEmpty())
        {
            zLog.warn("retrieved empty message");
            zEnv.resetReadCLine();
            setup(false);
            return;
        }

        /* we'll restore EOData in resend
         * - note that we may not get back everything that we need
         *   (specifically, we may loose 1st EOLine at end of zMsgDatas)
         *   so we'll automatically add extra EOLine to end of zMsgDatas
         */
        zEOData = stripEOD((CBufferWrapper) zMsgDatas.get(zMsgDatas.size() - 1), EODATAP);

        /* we will not send reply (containing message) yet
         * - we'll send reply to driver later
         *   (after we have scanned this message and
         *    are ready to resend this message to driver)
         */
        zEnv.clearReadCLine();

        zMsg.parse(true);
        zMsgInfo = new MLMessageInfo(zHdlInfo, zEnv, zMsg);
        //zLog.debug("message: " + zMsg);
        return;
    }

    private void rejectEOData(TCPChunkEvent zEvent, XMSEnv zEnv)
    {
        zLog.debug("end of data (reject message)");
        //zLog.debug("end of data (reject message): " + zEnv.getReadDataCt() + " bytes exceeds size limit, " + iMsgSzLimit + " bytes, " + zMsgDatas);

        /* we'll restore EOData in resend
         * - note that we may not get back everything that we need
         *   (specifically, we may loose 1st EOLine at end of zMsgDatas)
         *   so we'll automatically add extra EOLine to end of zMsgDatas
         */
        zEOData = stripEOD((CBufferWrapper) zMsgDatas.get(zMsgDatas.size() - 1), EODATAP);

        rejectEOData(zEvent, zEnv, zTooLargeERR, Constants.REJECT);
        resend(zEnv); /* we'll setup again during resend */
        return;
    }
}
