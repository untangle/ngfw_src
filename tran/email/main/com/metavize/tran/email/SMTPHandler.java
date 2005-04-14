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

import java.lang.IndexOutOfBoundsException;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.util.*;

/* RFC 821:
   4.5.2.  TRANSPARENCY
      Without some provision for data transparency the character
      sequence "<CRLF>.<CRLF>" ends the mail text and cannot be sent
      by the user.  In general, users are not aware of such
      "forbidden" sequences.  To allow all user composed text to be
      transmitted transparently the following procedures are used.

         1. Before sending a line of mail text the sender-SMTP checks
         the first character of the line.  If it is a period, one
         additional period is inserted at the beginning of the line.
         2. When a line of mail text is received by the receiver-SMTP
         it checks the line.  If the line is composed of a single
         period it is the end of mail.  If the first character is a
         period and there are other characters on the line, the first
         character is deleted.

      The mail data may contain any of the 128 ASCII characters.  All
      characters are to be delivered to the recipient's mailbox
      including format effectors and other control characters.  If
      the transmission channel provides an 8-bit byte (octets) data
      stream, the 7-bit ASCII codes are transmitted right justified
      in the octets with the high order bits cleared to zero.

         In some systems it may be necessary to transform the data as
         it is received and stored.  This may be necessary for hosts
         that use a different character set than ASCII as their local
         character set, or that store data in records rather than
         strings.  If such transforms are necessary, they must be
         reversible -- especially if such transforms are applied to
         mail being relayed.
*/
/* SMTP - RFC 821
 * ARPA Internet text message format - RFC 822
 * SMTP Service Extensions - RFC 1869
 *
 * SMTP client = driver = sender of commands (recipient of replies)
 * SMTP server = passenger = recipient of commands (sender of replies)
 */
public class SMTPHandler extends MLHandler
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(SMTPHandler.class.getName());

    /* (client) commands - ignore other commands */
    private final static String SEND = "^(MAIL|SEND|SOML|SAML) FROM:";
    private final static String RCPT = "^RCPT TO:";
    private final static String DATA = "^DATA" + Constants.PEOLINE;
    private final static String EODATA = "(^|" + Constants.PEOLINE + ")\\." + Constants.PEOLINE;
    private final static String NOOP = "^NOOP" + Constants.PEOLINE;
    private final static String RSET = "^RSET" + Constants.PEOLINE;
    private final static String QUIT = "^QUIT" + Constants.PEOLINE;
    private final static Pattern SENDP = Pattern.compile(SEND, Pattern.CASE_INSENSITIVE);
    private final static Pattern RCPTP = Pattern.compile(RCPT, Pattern.CASE_INSENSITIVE);
    private final static Pattern DATAP = Pattern.compile(DATA, Pattern.CASE_INSENSITIVE);
    private final static Pattern EODATAP = Pattern.compile(EODATA);
    private final static Pattern NOOPP = Pattern.compile(NOOP, Pattern.CASE_INSENSITIVE);
    private final static Pattern RSETP = Pattern.compile(RSET, Pattern.CASE_INSENSITIVE);
    private final static Pattern QUITP = Pattern.compile(QUIT, Pattern.CASE_INSENSITIVE);

    private final static byte DATABA[] = { 'D', 'A', 'T', 'A', 13, 10 };

    /* (pseudo-client) cmds */

    /* (server) reply codes (ignore multiline replies) */
    private final static String SERVICEOPEN = "^220 ";
    private final static String SERVICECLOSE = "^221 ";
    private final static String COMMANDOK = "^25[01] "; /* 250 or 251 */
    private final static String DATAOK = "^354 ";
    protected final static Pattern SERVICEOPENP = Pattern.compile(SERVICEOPEN);
    private final static Pattern SERVICECLOSEP = Pattern.compile(SERVICECLOSE);
    private final static Pattern COMMANDOKP = Pattern.compile(COMMANDOK);
    private final static Pattern DATAOKP = Pattern.compile(DATAOK);

    /* (server) reply codes */
    private final static String PIPELINING = "250.PIPELINING";
    private final static Pattern PIPELININGP = Pattern.compile(PIPELINING, Pattern.CASE_INSENSITIVE);

    /* (pseudo-server) reply codes */
    /* 250 OK
     * - only 250 code is necessary but add OK for sensitive SMTP clients
     * (sensitive STMP clients check text after reply code for keywords)
     */
    private final static byte CMDOKBA[] = { '2', '5', '0', ' ', 'O', 'K', 13, 10 };
    /* 354 Start mail; end with "." on a line by itself
     * - only 354 code is necessary but add rest for sensitive SMTP clients
     * (sensitive STMP clients check text after reply code for keywords)
     */
    private final static byte DATAOKBA[] = { '3', '5', '4', ' ', 'S', 't', 'a', 'r', 't', ' ', 'm', 'a', 'i', 'l', ';', ' ', 'e', 'n', 'd', ' ', 'w', 'i', 't', 'h', ' ', '\"', '.', '\"', ' ', 'o', 'n', ' ', 'a', ' ', 'l', 'i', 'n', 'e', ' ', 'b', 'y', ' ', 'i', 't', 's', 'e', 'l', 'f', 13, 10 };
    /* 554 Transaction failed: message has been blocked
     * - 554 code refers to "Transaction failed";
     *   we add ": Message has been blocked" as fyi
     */
    private final static byte DATAERRBA[] = { '5', '5', '4', ' ', 'T', 'r', 'a', 'n', 's', 'a', 'c', 't', 'i', 'o', 'n', ' ', 'f', 'a', 'i', 'l', 'e', 'd', ':', ' ', 'M', 'e', 's', 's', 'a', 'g', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e', 'n', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', 13, 10 };
    /* 554 Transaction failed: message size exceeds maximum limit
     * - 554 code refers to "Transaction failed";
     *   we add ": Message exceeds maximum size limit" as fyi
     */
    private final static byte TOOLARGEERRBA[] = { '5', '5', '4', ' ', 'T', 'r', 'a', 'n', 's', 'a', 'c', 't', 'i', 'o', 'n', ' ', 'f', 'a', 'i', 'l', 'e', 'd', ':', ' ', 'M', 'e', 's', 's', 'a', 'g', 'e', ' ', 'e', 'x', 'c', 'e', 'e', 'd', 's', ' ', 'm', 'a', 'x', 'i', 'm', 'u', 'm', ' ', 's', 'i', 'z', 'e', ' ', 'l', 'i', 'm', 'i', 't', 13, 10 };

    /* cmds */
    private final static int SEND_VAL = 0;
    private final static int RCPT_VAL = 1;
    private final static int DATA_VAL = 2;
    private final static int RSET_VAL = 3;
    private final static int NOOP_VAL = 4;
    private final static int QUIT_VAL = 5;

    private final static Integer SEND_INT = new Integer(SEND_VAL);
    private final static Integer RCPT_INT = new Integer(RCPT_VAL);
    private final static Integer DATA_INT = new Integer(DATA_VAL);
    private final static Integer RSET_INT = new Integer(RSET_VAL);
    private final static Integer NOOP_INT = new Integer(NOOP_VAL);
    private final static Integer QUIT_INT = new Integer(QUIT_VAL);

    /* replies */
    private final static int COMMANDOK_VAL = 0;
    private final static int DATAOK_VAL = 1;
    private final static int PIPELINING_VAL = 2;

    private final static Integer COMMANDOK_INT = new Integer(COMMANDOK_VAL);
    private final static Integer DATAOK_INT = new Integer(DATAOK_VAL);
    private final static Integer PIPELINING_INT = new Integer(PIPELINING_VAL);

    private final static int DNC_VAL = -1; /* do not care */
    private final static Integer DNC_INT = new Integer(DNC_VAL);

    /* class variables */

    /* instance variables */
    private ByteBuffer zData; /* reference copy of DATA cmd */
    private ByteBuffer zCmdOK; /* reference copy of COMMANDOK reply */
    private ByteBuffer zDataOK; /* reference copy of DATAOK reply */
    private ByteBuffer zDataERR; /* reference copy of Transaction failed reply */
    private ByteBuffer zTooLargeERR; /* reference copy of Transaction failed reply */

    private ByteBuffer zEOData;
    private ArrayList zToPassengers; /* list of resend-to-passenger lines */
    private boolean bStartMsg;
    private boolean bReadData;

    /* constructors */
    public SMTPHandler()
    {
        super();

        zData = ByteBuffer.wrap(DATABA, DATABA.length, 0);

        zCmdOK = ByteBuffer.wrap(CMDOKBA, CMDOKBA.length, 0);
        zDataOK = ByteBuffer.wrap(DATAOKBA, DATAOKBA.length, 0);
        zDataERR = ByteBuffer.wrap(DATAERRBA, DATAERRBA.length, 0);
        zTooLargeERR = ByteBuffer.wrap(TOOLARGEERRBA, TOOLARGEERRBA.length, 0);

        zToPassengers = new ArrayList();

        setup();
    }

    /* public methods */
    public void setOptions(XMailScannerCache zXMSCache)
    {
        zPostmaster = null;
        iMsgSzLimit = zXMSCache.getMsgSzLimit();
        bReturnErr = zXMSCache.getReturnErrOnSMTPBlock();
        return;
    }

    /* for SMTP,
     * each message transaction involves several cmd/reply sequences
     * - we detect message transaction, prompt for, and
     *   intercept each piece of message
     *   (during checkCmd, we intercept cmds from driver and
     *    send pseudo-server replies to driver),
     *   filter message, and
     *   resend each piece of message
     *   (during checkReply, we resend cmds to passenger and
     *    intercept replies from passenger)
     */
    /* SMTP client */
    public void checkCmd(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException, ParseException
    {
        if (false == bReadData)
        {
            if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
            {
                zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
                return; /* line is not complete - get rest of line */
            }
            /* else line is now complete */
        }
        else
        {
            if (false == readData(zEvent, zEnv, zMsgDatas, Constants.EOLINEFEEDP, EODATAP))
            {
                if (zEnv.getReadDataCt() < iMsgSzLimit)
                {
                    //zLog.debug("read more (cmd): " + zEnv.getReadDataCt() + ", " + iReadDataLimit);
                    return; /* data is not complete - get rest of data */
                }

                if (false == bRejectData)
                {
                    /* setup warning in order to generate SizeLimitLogEvent;
                     * we will not issue warning
                     */
                    setupWarning(zEnv);
                    bRejectData = true; /* reject message data */
                }
                rejectData();
                //zLog.debug("read more reject (cmd): " + zEnv.getReadDataCt() + ", " + iReadDataLimit);
                return; /* data is not complete - get rest of data */
            }
            /* else data is now complete */

            if (false == bRejectData &&
                zEnv.getReadDataCt() >= iMsgSzLimit)
            {
                /* message had been under size limit but
                 * now that data is complete,
                 * message exceeds size limit
                 * - setup warning in order to generate SizeLimitLogEvent;
                 *   we will not issue warning
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
        zLog.debug("check cmd: " + zCLine + ", state machine: " + zStatePairs.size() + " states");

        StatePair zStatePair;

        for (Iterator zIter = zStatePairs.iterator(); true == zIter.hasNext(); )
        {
            zStatePair = (StatePair) zIter.next();
            switch(zStatePair.getCmd().intValue())
            {
            case SEND_VAL:
                zLog.debug("sender cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, SENDP))
                {
                    setSender(zEnv, zCLine);
                    return;
                }

                break;

            case RCPT_VAL:
                zLog.debug("recipient cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, RCPTP))
                {
                    setRecipient(zEnv, zCLine);
                    return;
                }

                break;

            case DATA_VAL:
                zLog.debug("data cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, DATAP))
                {
                    setData(zEnv);
                    return;
                }

                break;

            case RSET_VAL:
                zLog.debug("reset cmd?: " + zStatePair.getCmd());
                if (true == isMatch(zCLine, RSETP))
                {
                    ackReset(zEnv);
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
                    ackQuit(zEnv, zCLine);
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

        if (false == bStartMsg)
        {
            return;
        }
        else
        {
            throw new ProtoException("Unexpected command received during e-mail message transaction: " + zCLine);
        }
    }

    /* SMTP server */
    public void checkReply(TCPChunkEvent zEvent, XMSEnv zEnv) throws ProtoException, ReadException
    {
        if (false == readLine(zEvent, zEnv, Constants.PEOLINEP))
        {
            /* line is not complete - get rest of line */
            zEnv.setFixedResult(Constants.READ_MORE_NO_WRITE);
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
            case COMMANDOK_VAL:
                zLog.debug("command ok reply?: " + zStatePair.getReply());
                if (true == isMatch(zCLine, COMMANDOKP))
                {
                    unsetRecipient(zEnv);
                    return;
                }

                break;

            case DATAOK_VAL:
                zLog.debug("data ok reply?: " + zStatePair.getReply());
                if (true == isMatch(zCLine, DATAOKP))
                {
                    unsetEOData(zEnv);
                    return;
                }

                break;

            case PIPELINING_VAL:
                zLog.debug("ESMTP pipelining reply?: " + zStatePair.getReply());
                if (true == isMatch(zCLine, PIPELININGP))
                {
                    zLog.debug("suppressed ESMTP pipelining reply: " + zCLine);
                    zEnv.setFixedResult(Constants.DO_NOT_PASS);
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

        if (false == bStartMsg)
        {
            return;
        }
        else
        {
            throw new ProtoException("Unexpected command received during e-mail message transaction: " + zCLine);
        }
    }

    public void block(XMSEnv zEnv, boolean bCopy, int iType)
    {
        zLog.debug("block message");
        block(zEnv, zCmdOK, zDataERR, bCopy);
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
            setup();
            return true;
        }

        //zLog.debug("resend message: " + zMsg);
        ArrayList zRcpts = zMsg.getRcpt();

        ByteBuffer zLine;

        /* enqueue RECIPIENT cmds
         * - we send these after we've sent SENDER cmd
         */
        for (Iterator zIter = zRcpts.iterator(); true == zIter.hasNext(); )
        {
            zLine = ((CBufferWrapper) zIter.next()).get();
            zToPassengers.add(zLine);
        }

        zStateMachine.reset(DNC_INT, COMMANDOK_INT);

        zLine = zMsg.getSender().get();
        zEnv.sendToPassenger(zLine); /* resend original SENDER cmd */
        zEnv.resetReadCLine();
        return true;
    }

    public void setup()
    {
        zMsgInfo = null;
        zMsgDatas = null;
        iReadDataLimit = Constants.NO_MSGSZ_LIMIT;
        iRejectedCt = 0;
        bRejectData = false; /* accept message data */

        zEOData = null;
        bStartMsg = false; /* not ready to start new message transaction */
        bReadData = false; /* use read line mode */

        /* in order of most likely to least likely to occur
         * - we are either intercepting or passing these cmds through
         *   so we don't care about replies yet
         */
        zStateMachine.reset(SEND_INT, PIPELINING_INT);
        zStateMachine.set(QUIT_INT, DNC_INT);
        zStateMachine.set(DNC_INT, DNC_INT);
        return;
    }

    /* private methods */
    /* RFC 821:
     * MAIL (MAIL), SEND (SEND), SEND OR MAIL (SOML), SEND AND MAIL (SAML)
     *  This command is used to initiate a mail transaction in which
     *  the mail data is delivered to one or more mailboxes.  The
     *  argument field contains a reverse-path.
     */
    private void setSender(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("sender");
        bStartMsg = true; /* ready to start a new message transaction */

        if (null == zMsg)
        {
            zMsg = new MLMessage();
        }
        /* else recycle original MLMessage */

        zMsg.setSender(zCLine); /* save SENDER cmd */
        zMsg.setRcpt(); /* prepare for recipients (which must follow) */

        zStateMachine.reset(RCPT_INT, DNC_INT);
        zStateMachine.set(RSET_INT, DNC_INT);
        zStateMachine.set(NOOP_INT, DNC_INT);
        zStateMachine.set(DNC_INT, DNC_INT);

        zEnv.sendToDriver(zCmdOK); /* ack with pseudo-server reply (250) */
        zEnv.clearReadCLine();
        return;
    }

    /* RFC 821:
     * RECIPIENT (RCPT)
     *  This command is used to identify an individual recipient of
     *  the mail data; multiple recipients are specified by multiple
     *  use of this command.
     */
    private void setRecipient(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("recipient");
        zMsg.addRcpt(zCLine); /* save RECIPIENT cmd (one or more) */

        zStateMachine.reset(RCPT_INT, DNC_INT);
        zStateMachine.set(DATA_INT, DNC_INT);
        zStateMachine.set(RSET_INT, DNC_INT);
        zStateMachine.set(NOOP_INT, DNC_INT);
        zStateMachine.set(DNC_INT, DNC_INT);

        zEnv.sendToDriver(zCmdOK); /* ack with pseudo-server reply (250) */
        zEnv.clearReadCLine();
        return;
    }

    /* RFC 821:
     * DATA (DATA)
     *  The receiver treats the lines following the command as mail
     *  data from the sender.  This command causes the mail data
     *  from this command to be appended to the mail data buffer.
     *  The mail data may contain any of the 128 ASCII character
     *  codes.
     *  The mail data is terminated by a line containing only a
     *  period, that is the character sequence "<CRLF>.<CRLF>" (see
     *  Section 4.5.2 on Transparency).  This is the end of mail
     *  data indication.
     */
    private void setData(XMSEnv zEnv)
    {
        zLog.debug("data");
        /* we're buffering message; passenger does not need DATA yet
         * - intercept DATA but don't save it (we'll restore it in unsetData)
         */

        /* in read data mode, we will not receive any more cmds from driver
         * until we have received and sent EODATA from driver
         */
        bReadData = true; /* use read data mode */

        zStateMachine.reset(DNC_INT, DNC_INT);

        zMsgDatas = zMsg.getData();

        zEnv.sendToDriver(zDataOK); /* ack with pseudo-server reply (354) */
        zEnv.clearReadCLine();
        return;
    }

    private void setEOData(XMSEnv zEnv) throws ParseException
    {
        zLog.debug("end of data");

        /* we're buffering message; passenger does not need data yet */

        if (true == zMsgDatas.isEmpty())
        {
            zLog.warn("retrieved empty message");
            zEnv.resetReadCLine();
            setup();
            return;
        }

        /* we'll restore EOData in resend->...->unsetEOData
         * - note that we may not get back everything that we need
         *   (specifically, we may loose 1st EOLINE at end of zMsgDatas)
         *   so we'll automatically add extra EOLine to end of zMsgDatas
         */
        zEOData = stripEOD((CBufferWrapper) zMsgDatas.get(zMsgDatas.size() - 1), EODATAP);

        /* we will not ack EODATA with pseudo-server reply (250) yet
         * - we'll send pseudo-server reply to driver later
         *   (after we have scanned this message and
         *    are ready to resend this message to passenger)
         */
        zEnv.clearReadCLine();

        zMsg.parse(true);
        zMsgInfo = new MLMessageInfo(zHdlInfo, zEnv, zMsg);
        //zLog.debug("read message: " + zMsg);
        return;
    }

    private void rejectEOData(TCPChunkEvent zEvent, XMSEnv zEnv)
    {
        zLog.debug("end of data (reject message)");
        //zLog.debug("end of data (reject message): " + zEnv.getReadDataCt() + " bytes exceeds size limit, " + iMsgSzLimit + " bytes, " + zMsgDatas);
        rejectEOData(zEvent, zEnv, zCmdOK, zTooLargeERR);
        /* we'll setup again
         * since we've rejected message (and did not resend it)
         */
        setup();
        return;
    }

    /* RFC 821:
     * RESET (RSET)
     *  This command specifies that the current mail transaction is
     *  to be aborted.  Any stored sender, recipients, and mail data
     *  must be discarded, and all buffers and state tables cleared.
     *  The receiver must send an OK reply.
     */
    private void ackReset(XMSEnv zEnv)
    {
        zLog.debug("reset");
        zEnv.sendToDriver(zCmdOK); /* ack with pseudo-server reply (250) */
        zEnv.resetReadCLine();

        /* since we've buffered message, passenger does not need RSET
         * - intercept RSET,
         *   delete contents of current message (recycle message), and
         *   setup again
         */
        if (null != zMsg)
        {
            flushMsg(zEnv);
        }
        setup();
        return;
    }

    private void ackNoop(XMSEnv zEnv)
    {
        zLog.debug("noop");
        /* since passenger will "ignore" anyway, passenger does not need NOOP
         * - intercept NOOP
         */

        zEnv.sendToDriver(zCmdOK); /* ack with pseudo-server reply (250) */
        zEnv.resetReadCLine();
        return;
    }

    /* RFC 821:
     * QUIT (QUIT)
     *  This command specifies that the receiver must send an OK
     *  reply, and then close the transmission channel.
     *  The receiver should not close the transmission channel until
     *  it receives and replies to a QUIT command (even if there was
     *  an error).  The sender should not close the transmission
     *  channel until it send a QUIT command and receives the reply
     *  (even if there was an error response to a previous command).
     *  If the connection is closed prematurely the receiver should
     *  act as if a RSET command had been received (canceling any
     *  pending transaction, but not undoing any previously
     *  completed transaction), the sender should act as if the
     *  command or transaction in progress had received a temporary
     *  error (4xx).
     */
    private void ackQuit(XMSEnv zEnv, CBufferWrapper zCLine)
    {
        zLog.debug("quit");
        zEnv.setFixedResult(Constants.PASS_THROUGH);

        if (null != zMsg)
        {
            flushMsg(zEnv);
            zMsg = null;
        }
        setup(); /* not really necessary to setup again but ... */
        return;
    }

    private void unsetRecipient(XMSEnv zEnv)
    {
        ByteBuffer zLine;

        try
        {
            zLine = (ByteBuffer) zToPassengers.remove(0);
            if (null == zLine)
            {
                unsetData(zEnv);
                return;
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            unsetData(zEnv);
            return;
        }

        zEnv.sendToPassenger(zLine); /* resend original RCPT cmd */
        zEnv.resetReadCLine();
        return;
    }

    private void unsetData(XMSEnv zEnv)
    {
        zLog.debug("resend data");
        zLog.debug("(SODATA): " + zData);

        zStateMachine.reset(DNC_INT, DATAOK_INT);

        zEnv.sendToPassenger(zData); /* resend original DATA cmd */
        zEnv.resetReadCLine();
        return;
    }

    private void unsetEOData(XMSEnv zEnv)
    {
        zLog.debug("resend all data");

        /* we add EOLINE in case zMsgDatas doesn't end with EOLINE
         * - we need EOLINE to correctly define zEOData termination
         * - EOLINE may have been present at start of end-of-data but
         *   if SpamAssassin significantly modifies message,
         *   SpamAssassin does not insert EOLINE
         *   at end of each line that it adds
         */
        ByteBuffer zEOLine = ByteBuffer.wrap(Constants.EOLINEBA, Constants.EOLINEBA.length, 0);

        //zLog.debug("message: " + zMsg);
        zLog.debug("(EODATA): " + zEOLine + ", " + zEOData);

        /* resend message data */
        zEnv.convertToPassenger(zMsgDatas);
        zEnv.sendToPassenger(zEOLine);
        zEnv.sendToPassenger(zEOData);
        zEnv.resetReadCLine();

        /* we did not ack message (250) yet
         * - passenger just sent reply and we read it
         *   so we'll pass reply through next (as DNC)
         */

        flushMsg(zEnv);
        setup(); /* since we're sending data, we'll setup again */
        return;
    }
}
