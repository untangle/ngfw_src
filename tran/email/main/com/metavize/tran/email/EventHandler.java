/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EventHandler.java,v 1.22 2005/03/25 23:22:39 jdi Exp $
 */
package com.metavize.tran.email;

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import org.apache.log4j.Logger;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.fprot.FProtScanner;
import com.metavize.tran.hauri.HauriScanner;
import com.metavize.tran.sophos.SophosScanner;
import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;
import com.metavize.tran.util.MatchAction;

public class EventHandler extends AbstractEventHandler
{
    class XMailScannerSession
    {
        /* during its limited lifetime,
         * each session only uses its cache and env references
         */
        private XMailScannerCache zCache;
        private XMSEnv zEnv;

        private MailSender zMailSender;
        private int iDriver; /* driver is client or server */
        private int iSpamFlags; /* signed, 32-bit value */
        private int iVirusFlags; /* signed, 32-bit value */

        public XMailScannerSession(XMailScannerCache zXMSCache)
        {
            zCache = zXMSCache;
            zEnv = new XMSEnv();
            zMailSender = MvvmContextFactory.context().mailSender();

            iDriver = Constants.CLIENT;
            iSpamFlags = 0;
            iVirusFlags = 0;
        }

        public void setDriver(int iDriver)
        {
            this.iDriver = iDriver;
            return;
        }

        public void setSpamFlags(boolean bScan, boolean bBlock, boolean bCopyOnBlock, boolean bNotifySender, boolean bNotifyReceiver)
        {
            iSpamFlags = setFlags(iSpamFlags, bScan, bBlock, bCopyOnBlock, false, bNotifySender, bNotifyReceiver);
            return;
        }

        public void setVirusFlags(boolean bScan, boolean bBlock, boolean bCopyOnBlock, boolean bReplace, boolean bNotifySender, boolean bNotifyReceiver)
        {
            iVirusFlags = setFlags(iVirusFlags, bScan, bBlock, bCopyOnBlock, bReplace, bNotifySender, bNotifyReceiver);
            return;
        }

        public XMailScannerCache getCache()
        {
            return zCache;
        }

        public XMSEnv getEnv()
        {
            return zEnv;
        }

        public MailSender getMailSender()
        {
            return zMailSender;
        }

        public int getDriver()
        {
            return iDriver;
        }

        public int getSpamNotify()
        {
            return getNotifyFlag(iSpamFlags);
        }

        public int getVirusNotify()
        {
            return getNotifyFlag(iVirusFlags);
        }

        public boolean getSpamOption(int iType)
        {
            return getOption(iSpamFlags, iType);
        }

        public boolean getVirusOption(int iType)
        {
            return getOption(iVirusFlags, iType);
        }

        private int setFlags(int iFlags, boolean bScan, boolean bBlock, boolean bCopyOnBlock, boolean bReplace, boolean bNotifySender, boolean bNotifyReceiver)
        {
            if (true == bScan)
            {
                iFlags |= Constants.SCAN_TYPE;
            }
            if (true == bBlock)
            {
                iFlags |= Constants.BLOCK_TYPE;
            }
            if (true == bReplace)
            {
                iFlags |= Constants.REPLACE_TYPE;
            }
            if (true == bCopyOnBlock)
            {
                iFlags |= Constants.CPONBLOCK_TYPE;
            }
            if (true == bNotifySender)
            {
                iFlags |= Constants.NTFYSENDR_TYPE;
            }
            if (true == bNotifyReceiver)
            {
                iFlags |= Constants.NTFYRECVR_TYPE;
            }
            return iFlags;
        }

        private int getNotifyFlag(int iFlags)
        {
            return ((iFlags & Constants.NTFYSENDR_TYPE) |
                    (iFlags & Constants.NTFYRECVR_TYPE));
        }

        private boolean getOption(int iFlags, int iType)
        {
            return (iType == (iFlags & iType)) ? true : false;
        }
    }

    private static final Logger zLog = Logger.getLogger(EventHandler.class.getName());
    private static final Logger zUserLog = MvvmContextFactory.context().eventLogger();

    private static final int SPAM = 0;
    private static final int VIRUS = 1;

    private static final int SCAN_COUNTER  = Transform.GENERIC_0_COUNTER;
    private static final int CUSTOMBLK_COUNTER = Transform.GENERIC_1_COUNTER;
    private static final int SPAMBLK_COUNTER = Transform.GENERIC_2_COUNTER;
    private static final int VIRUSBLK_COUNTER = Transform.GENERIC_3_COUNTER;

    private static final String SPAMSUBJNOTIFY = "WARNING: Message contains spam.";
    private static final String SPAMBODYNOTIFY = "The anti-spam scanner has reported that the attached message contains spam. Please review and proceed according to company policy.\n";
    private static final String VIRUSSUBJNOTIFY = "WARNING: Message contains virus.";
    private static final String VIRUSBODYNOTIFY = "The anti-virus scanner has reported that the attached message contains one or more viruses. Please review and proceed according to company policy.\n";

    /* we copy this cache once during every request and
     * never directly read it while handling session
     * - we save it with each session whenever new request arrives
     * - session only uses its private reference to cache
     */
    private XMailScannerCache zXMSCache;
    private SpamAssassin zInboundSpam;
    private SpamAssassin zOutboundSpam;
    private VirusScanner zVirusScanner;
    private boolean bCopyOnException;

    public EventHandler(XMailScannerCache zXMSCache)
    {
        this.zXMSCache = zXMSCache;
        zInboundSpam = new SpamAssassin( EmailTransformImpl.getSpamInboundConfigFile());
        zOutboundSpam = new SpamAssassin( EmailTransformImpl.getSpamOutboundConfigFile());
        setVirusScanner(zXMSCache);
        bCopyOnException = zXMSCache.getCopyOnException();
    }

    public void renew(XMailScannerCache zXMSCache)
    {
        /* all currently active sessions will continue to use original cache
         * but all new sessions will use this new cache
         */
        this.zXMSCache = zXMSCache;
        zInboundSpam = new SpamAssassin( EmailTransformImpl.getSpamInboundConfigFile());
        zOutboundSpam = new SpamAssassin( EmailTransformImpl.getSpamOutboundConfigFile());
        setVirusScanner(zXMSCache);
        bCopyOnException = zXMSCache.getCopyOnException();
        return;
    }

    /* handle request for new session */
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        TCPNewSessionRequest sessReq = event.sessionRequest();
        zLog.debug("TCP session requested: " +
          sessReq.clientAddr() + ":" + sessReq.clientPort() + " -> " +
          sessReq.serverAddr() + ":" + sessReq.serverPort());

        return;
    }

    /* handle new session */
    public void handleTCPNewSession(TCPSessionEvent event)
    {
        TCPSession session = event.session();
        zLog.debug("TCP session started: " +
          session.clientAddr() + ":" + session.clientPort() + " -> " +
          session.serverAddr() + ":" + session.serverPort());

        /* create read-write TCPSession and use it with this session */
        session.clientLineBuffering(true);
        session.serverLineBuffering(true);

        /* attach XMailScannerSession to TCPSession;
         * we need to access MLHandler (inside XMailScannerSession) later
         * to handle e-mail
         */
        XMailScannerSession zXMSession = new XMailScannerSession(zXMSCache);
        session.attach(zXMSession);

        session.clientReadLimit(MLHandler.READSZ);
        session.serverReadLimit(MLHandler.READSZ);

        return;
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
    {
        TCPSession session = event.session();
        XMailScannerSession zXMSession = (XMailScannerSession) session.attachment();
        XMSEnv zEnv = zXMSession.getEnv();
        int iDriver = zXMSession.getDriver();

        MLHandler zHandler = zEnv.getHandler();
        if (null == zHandler)
        {
            zHandler = MLHandler.identify(event, zEnv);
            if (null != zHandler)
            {
                zEnv.setHandler(zHandler);
                iDriver = Constants.SERVER;
                zXMSession.setDriver(iDriver);
                setOptions(session, zXMSession, zHandler);

                zLog.debug("client got handler: " + zHandler + ", client is driver: " + iDriver);
                return zHandler.write(event, zEnv, iDriver);
            }
            else /* no handler - this is not e-mail data */
            {
                zLog.debug("TCP client session released: " +
                  session.clientAddr() + ":" + session.clientPort() + " -> " +
                  session.serverAddr() + ":" + session.serverPort());

                session.release(); /* release this session */
                return Constants.PASS_THROUGH;
            }
        }

        int iEndpoint = MLHandler.DRIVER;

        switch(iDriver)
        {
        default:
        case Constants.CLIENT:
            //zLog.debug("client is driver: " + zHandler);
            try
            {
                zHandler.checkCmd(event, zEnv);
            }
            catch (ReadException e)
            {
                zLog.error("Unable to read cmd (e-mail client-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ProtoException e)
            {
                zLog.error("Unable to retrieve or transmit e-mail (e-mail client-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ParseException e)
            {
                zLog.error("Unable to parse message because message contains incorrectly formatted data (e-mail client-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            break;

        case Constants.SERVER:
            //zLog.debug("client is passenger: " + zHandler);
            try
            {
                zHandler.checkReply(event, zEnv);
            }
            catch (ReadException e)
            {
                zLog.error("Unable to read reply (e-mail client-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ProtoException e)
            {
                zLog.error("Unable to retrieve or transmit e-mail (e-mail client-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ParseException e)
            {
                zLog.error("Unable to parse message because message contains incorrectly formatted data (e-mail client-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }

            iEndpoint = MLHandler.PASSENGER;
            break;
        }

        if (true == zHandler.isMsgReady(iEndpoint))
        {
            filter(session, zXMSession, zEnv, zHandler);
        }

        //IPDataResult zResult = zHandler.write(event, zEnv, iDriver);
        //zLog.debug("client send result: " + zResult);
        //return zResult;
        return zHandler.write(event, zEnv, iDriver);
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
    {
        TCPSession session = event.session();
        XMailScannerSession zXMSession = (XMailScannerSession) session.attachment();
        XMSEnv zEnv = zXMSession.getEnv();
        int iDriver = zXMSession.getDriver();

        MLHandler zHandler = zEnv.getHandler();
        if (null == zHandler)
        {
            zHandler = MLHandler.identify(event, zEnv);
            if (null != zHandler)
            {
                zEnv.setHandler(zHandler);
                iDriver = Constants.CLIENT;
                zXMSession.setDriver(iDriver);
                setOptions(session, zXMSession, zHandler);

                zLog.debug("server got handler: " + zHandler + ", server is driver: " + iDriver);
                return zHandler.write(event, zEnv, iDriver);
            }
            else /* no handler - this is not e-mail data */
            {
                zLog.debug("TCP server session released: " +
                  session.clientAddr() + ":" + session.clientPort() + " -> " +
                  session.serverAddr() + ":" + session.serverPort());

                session.release(); /* release this session */
                return Constants.PASS_THROUGH;
            }
        }

        int iEndpoint = MLHandler.PASSENGER;

        switch(iDriver)
        {
        default:
        case Constants.CLIENT:
            //zLog.debug("server is passenger: " + zHandler);
            try
            {
                zHandler.checkReply(event, zEnv);
            }
            catch (ReadException e)
            {
                zLog.error("Unable to read reply (e-mail server-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ProtoException e)
            {
                zLog.error("Unable to retrieve or transmit e-mail (e-mail server-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ParseException e)
            {
                zLog.error("Unable to parse message because message contains incorrectly formatted data (e-mail server-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            break;

        case Constants.SERVER:
            //zLog.debug("server is driver: " + zHandler);
            try
            {
                zHandler.checkCmd(event, zEnv);
            }
            catch (ReadException e)
            {
                zLog.error("Unable to read cmd (e-mail server-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ProtoException e)
            {
                zLog.error("Unable to retrieve or transmit e-mail (e-mail server-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }
            catch (ParseException e)
            {
                zLog.error("Unable to parse message because message contains incorrectly formatted data (e-mail server-side): " + e);
                handleException(zEnv, zHandler, e);
                break;
            }

            iEndpoint = MLHandler.DRIVER;
            break;
        }

        if (true == zHandler.isMsgReady(iEndpoint))
        {
            filter(session, zXMSession, zEnv, zHandler);
        }

        //IPDataResult zResult = zHandler.write(event, zEnv, iDriver);
        //zLog.debug("server send result: " + zResult);
        //return zResult;
        return zHandler.write(event, zEnv, iDriver);
    }

    public void handleTCPFinalized(TCPSessionEvent event) throws MPipeException
    {
        TCPSession session = event.session();
        XMailScannerSession zXMSession = (XMailScannerSession) session.attachment();
        XMSEnv zEnv = zXMSession.getEnv();

        zLog.debug("TCP session finalized: " +
          session.clientAddr() + ":" + session.clientPort() + " -> " +
          session.serverAddr() + ":" + session.serverPort());

        session.attach(null);

        super.handleTCPFinalized(event);
        return;
    }

    private void setOptions(TCPSession session, XMailScannerSession zXMSession, MLHandler zHandler)
    {
        boolean bSessionIsInbound = (session.direction() == IPSessionDesc.INBOUND) ? true : false;
        boolean bMsgIsUploaded = zHandler.isMsgUploaded(bSessionIsInbound);
        zLog.debug("session is inbound: " + bSessionIsInbound + ", msg is uploaded: " + bMsgIsUploaded);

        XMailScannerCache zCache = zXMSession.getCache();
        boolean bScan = getSpamOption(zCache, bMsgIsUploaded, Constants.SCAN_TYPE);
        boolean bBlock = getSpamOption(zCache, bMsgIsUploaded, Constants.BLOCK_TYPE);
        boolean bCopyOnBlock = getSpamOption(zCache, bMsgIsUploaded, Constants.CPONBLOCK_TYPE);
        boolean bNotifySender = getSpamOption(zCache, bMsgIsUploaded, Constants.NTFYSENDR_TYPE);
        boolean bNotifyReceiver = getSpamOption(zCache, bMsgIsUploaded, Constants.NTFYRECVR_TYPE);

        zXMSession.setSpamFlags(bScan, bBlock, bCopyOnBlock, bNotifySender, bNotifyReceiver);
        zLog.debug("spam scan: " + zXMSession.getSpamOption(Constants.SCAN_TYPE) + ", block: " + zXMSession.getSpamOption(Constants.BLOCK_TYPE) + ", copy on block: " + zXMSession.getSpamOption(Constants.CPONBLOCK_TYPE) + ", notify sender: " + zXMSession.getSpamOption(Constants.NTFYSENDR_TYPE) + ", notify receiver: " + zXMSession.getSpamOption(Constants.NTFYRECVR_TYPE));

        bScan = getVirusOption(zCache, bMsgIsUploaded, Constants.SCAN_TYPE);
        bBlock = getVirusOption(zCache, bMsgIsUploaded, Constants.BLOCK_TYPE);
        bCopyOnBlock = getVirusOption(zCache, bMsgIsUploaded, Constants.CPONBLOCK_TYPE);
        boolean bReplace = getVirusOption(zCache, bMsgIsUploaded, Constants.REPLACE_TYPE);
        bNotifySender = getVirusOption(zCache, bMsgIsUploaded, Constants.NTFYSENDR_TYPE);
        bNotifyReceiver = getVirusOption(zCache, bMsgIsUploaded, Constants.NTFYRECVR_TYPE);

        zXMSession.setVirusFlags(bScan, bBlock, bCopyOnBlock, bReplace, bNotifySender, bNotifyReceiver);
        zLog.debug("virus scan: " + zXMSession.getVirusOption(Constants.SCAN_TYPE) + ", block: " + zXMSession.getVirusOption(Constants.BLOCK_TYPE) + ", copy on block: " + zXMSession.getVirusOption(Constants.CPONBLOCK_TYPE) + ", remove: " + zXMSession.getVirusOption(Constants.REPLACE_TYPE) + ", notify sender: " + zXMSession.getVirusOption(Constants.NTFYSENDR_TYPE) + ", notify receiver: " + zXMSession.getVirusOption(Constants.NTFYRECVR_TYPE));

        zHandler.setOptions(zCache);
        return;
    }

    private boolean getSpamOption(XMailScannerCache zCache, boolean bMsgIsUploaded, int iType)
    {
        return ((true == bMsgIsUploaded && true == zCache.getSpamOutboundOption(iType)) ||
                (false == bMsgIsUploaded && true == zCache.getSpamInboundOption(iType))) ? true : false;
    }

    private boolean getVirusOption(XMailScannerCache zCache, boolean bMsgIsUploaded, int iType)
    {
        return ((true == bMsgIsUploaded && true == zCache.getVirusOutboundOption(iType)) ||
                (false == bMsgIsUploaded && true == zCache.getVirusInboundOption(iType))) ? true : false;
    }

    private void filter(TCPSession zSession, XMailScannerSession zXMSession, XMSEnv zEnv, MLHandler zHandler)
    {
        zLog.debug("filter: scan");
        if (false == scan(zSession, zXMSession, zEnv, zHandler))
        {
            /* we've already handled any exception */
            return;
        }

        zLog.debug("filter: resend");
        if (false == zHandler.resend(zEnv))
        {
            /* if unable to resend message, save copy of it */
            zLog.error("Unable to resend message; attempting to save copy of message");
            handleException(zEnv, zHandler, null);
        }

        return;
    }

    /* scan message
     * - returns true if scan successfully completes or no cache exists
     *   (regardless of result if there is one - e.g., spam scan) or
     *   no cache exists or
     *   returns false if scan fails (no result)
     */
    private boolean scan(TCPSession zSession, XMailScannerSession zXMSession, XMSEnv zEnv, MLHandler zHandler)
    {
        long lCntr = incrementCount(SCAN_COUNTER);
        //zLog.debug("cntr 0: " + lCntr);

        XMailScannerCache zCache = zXMSession.getCache();
        if (null == zCache)
        {
            zLog.debug("No matching email rule; passing through message");
            return true;
        }

        MLMessage zMsg = zHandler.getMsg();
        int iMsgSz = zMsg.getSize();
        int iMsgSzLimit = zCache.getSpamMsgSzLimit();

        Action zAction;
        int iNotify;

        if ((Constants.NO_MSGSZ_LIMIT == iMsgSzLimit ||
             iMsgSzLimit >= iMsgSz) &&
            true == zXMSession.getSpamOption(Constants.SCAN_TYPE))
        {
            zLog.debug("Matched spam email rule: scanning message for spam");

            Object zScanResult;

            switch(zCache.getASScanner())
            {
            default:
            case Constants.SPAMAS_ID:
                if ( zSession.direction() == IPSessionDesc.INBOUND ) {
                    zScanResult = scan(zEnv, zHandler, zInboundSpam );
                } else {
                    zScanResult = scan(zEnv, zHandler, zOutboundSpam );
                }
                break;
            }

            if (null == zScanResult)
            {
                zUserLog.info(new SpamRuleEvent(zHandler.getMsgInfo(), Action.PASS, zMsg, false));
                return false; /* have already handled exception */
            }

            /* since we only handle SpamAssassin,
             * we will assume for now that zScanResult is Boolean
             */
            if (true == ((Boolean)zScanResult).booleanValue())
            {
                iNotify = zXMSession.getSpamNotify();
                if (0 != iNotify)
                {
                    zAction = notify(zXMSession, zMsg, iNotify, SPAM);
                    zUserLog.info(new SpamRuleEvent(zHandler.getMsgInfo(), zAction, zMsg, true));
                    lCntr = incrementCount(SPAMBLK_COUNTER);
                    //zLog.debug("cntr 2: " + lCntr);

                    /* we'll automatically block if we notify */
                    zHandler.block(zEnv, zXMSession.getSpamOption(Constants.CPONBLOCK_TYPE), Constants.ASBLOCK);
                    return true; /* skip other rules */
                }
                else if (true == zXMSession.getSpamOption(Constants.BLOCK_TYPE))
                {
                    zUserLog.info(new SpamRuleEvent(zHandler.getMsgInfo(), Action.BLOCK, zMsg, true));
                    lCntr = incrementCount(SPAMBLK_COUNTER);
                    //zLog.debug("cntr 2: " + lCntr);

                    zHandler.block(zEnv, zXMSession.getSpamOption(Constants.CPONBLOCK_TYPE), Constants.ASBLOCK);
                    return true; /* skip other rules */
                }
                else
                {
                    zUserLog.info(new SpamRuleEvent(zHandler.getMsgInfo(), Action.PASS, zMsg, true));
                    /* fall through and continue with other rules */
                }
            }
            else /* not spam */
            {
                zUserLog.info(new SpamRuleEvent(zHandler.getMsgInfo(), Action.PASS, zMsg, false));
                /* fall through and continue with other rules */
            }
        }

        iMsgSzLimit = zCache.getVirusMsgSzLimit();

        if ((Constants.NO_MSGSZ_LIMIT == iMsgSzLimit ||
             iMsgSzLimit >= iMsgSz) &&
            true == zXMSession.getVirusOption(Constants.SCAN_TYPE))
        {
            zLog.debug("Matched virus email rule: scanning message for viruses");

            VirusScannerResult zScanResult;

            if (true == zXMSession.getVirusOption(Constants.REPLACE_TYPE))
            {
                switch(zCache.getAVScanner())
                {
                case Constants.FPROTAV_ID:
                case Constants.SOPHOSAV_ID:
                case Constants.HAURIAV_ID:
                case Constants.CLAMAV_ID:
                    zScanResult = scan(zEnv, zCache, zHandler, zVirusScanner, true);
                    break;

                default:
                    /* if virus scanner is unavailable, assume clean */
                    zScanResult = VirusScannerResult.CLEAN;
                    break;
                }
            }
            else /* true == zXMSession.getVirusOption(Constants.BLOCK_TYPE) */
            {
                switch(zCache.getAVScanner())
                {
                case Constants.FPROTAV_ID:
                    zScanResult = scan(zEnv, zHandler, zVirusScanner);
                    break;

                case Constants.SOPHOSAV_ID:
                case Constants.CLAMAV_ID:
                case Constants.HAURIAV_ID:
                    /* these virus scanners do not process RFC822 messages
                     * so we must parse these messages and
                     * decode file attachments ourselves
                     */
                    zScanResult = scan(zEnv, zCache, zHandler, zVirusScanner, false);
                    break;

                default:
                    /* if virus scanner is unavailable, assume clean */
                    zScanResult = VirusScannerResult.CLEAN;
                    break;
                }
            }

            if (null == zScanResult)
            {
                zUserLog.info(new VirusRuleEvent(zHandler.getMsgInfo(), Action.PASS, zScanResult));
                return false; /* have already handled exception */
            }

            if (false == zScanResult.isClean())
            {
                iNotify = zXMSession.getVirusNotify();
                if (0 != iNotify)
                {
                    zAction = notify(zXMSession, zMsg, iNotify, VIRUS);
                    zUserLog.info(new VirusRuleEvent(zHandler.getMsgInfo(), zAction, zScanResult));
                    lCntr = incrementCount(VIRUSBLK_COUNTER);
                    //zLog.debug("cntr 3: " + lCntr);

                    /* we'll automatically block if we notify */
                    zHandler.block(zEnv, zXMSession.getVirusOption(Constants.CPONBLOCK_TYPE), Constants.AVBLOCK);
                    return true; /* skip other rules */
                }
                else if (true == zXMSession.getVirusOption(Constants.BLOCK_TYPE))
                {
                    zUserLog.info(new VirusRuleEvent(zHandler.getMsgInfo(), Action.BLOCK, zScanResult));
                    lCntr = incrementCount(VIRUSBLK_COUNTER);
                    //zLog.debug("cntr 3: " + lCntr);

                    zHandler.block(zEnv, zXMSession.getVirusOption(Constants.CPONBLOCK_TYPE), Constants.AVBLOCK);
                    return true; /* skip other rules */
                }
                else if (true == zXMSession.getVirusOption(Constants.REPLACE_TYPE))
                {
                    zUserLog.info(new VirusRuleEvent(zHandler.getMsgInfo(), Action.REPLACE, zScanResult));
                    lCntr = incrementCount(VIRUSBLK_COUNTER);
                    //zLog.debug("cntr 3: " + lCntr);
                    return true; /* skip other rules */
                }
                else
                {
                    zUserLog.info(new VirusRuleEvent(zHandler.getMsgInfo(), Action.PASS, zScanResult));
                    /* fall through and continue with other rules */
                }
            }
            else /* not virus */
            {
                zUserLog.info(new VirusRuleEvent(zHandler.getMsgInfo(), Action.PASS, zScanResult));
                /* fall through and continue with other rules */
            }
        }

        MatchAction zMatchAction = zCache.getAction(zMsg.getPTO(), XMailScannerCache.FIRST2FIRST, XMailScannerCache.DEFNONE);

        zUserLog.info(new CustomRuleEvent(zHandler.getMsgInfo(), zMatchAction));

        int iAction = zMatchAction.getPatternAction().getAction();
        switch(iAction)
        {
        case Constants.BLOCK_ACTION:
        case Constants.COPYONBLOCK_ACTION:
            lCntr = incrementCount(CUSTOMBLK_COUNTER);
            //zLog.debug("cntr 1: " + lCntr);

            if (Constants.BLOCK_ACTION == iAction)
            {
                zLog.debug("Matched custom email rule [" + zMatchAction.getFirstTrueDbg() + "]: blocking message");
                zHandler.block(zEnv, false, Constants.CSBLOCK);
            }
            else
            {
                zLog.debug("Matched custom email rule [" + zMatchAction.getFirstTrueDbg() + "]: copying and blocking message");
                zHandler.block(zEnv, true, Constants.CSBLOCK);
            }

            break;

        case Constants.EXCHANGE_ACTION:
            /* gui ignores exchange actions for activity log */
            zLog.debug("Matched custom email rule [" + zMatchAction.getFirstTrueDbg() + "]: replacing text in message");

            try
            {
                zHandler.modify(zCache, zMatchAction);
            }
            catch (ModifyException e)
            {
                zLog.error("Unable to modify e-mail message: " + e);
                handleException(zEnv, zHandler, e);
                return false;
            }
            /* now that we've made an exchange, event may pass through */
            break;

        case Constants.PASS_ACTION:
            zLog.debug("Matched custom email rule [" + zMatchAction.getFirstTrueDbg() + "]: passing through message without change");
            break;

        default:
            zLog.debug("No matching custom email rule; passing through message");
            break;
        }

        return true;
    }

    /* scan message,
     * retrieve updated message after scan, and
     * check updated message to determine results of scan
     */
    private Boolean scan(XMSEnv zEnv, MLHandler zHandler, SpamAssassin zScanner)
    {
        MLMessage zMsg = zHandler.getMsg();
        ArrayList zDatas = zMsg.getDataBuffers();
        if (null == zDatas)
        {
            return Boolean.FALSE; /* message is empty - should never occur */
        }

        ArrayList zNewDatas;

        try
        {
            zLog.debug("scan is starting");
            zNewDatas = zScanner.scanEmail(zDatas);
            zLog.debug("scan is complete");
            zDatas.clear();
        }
        catch (IOException e)
        {
            /* let message pass through as is */
            zLog.error("Unable to send/retrieve e-mail message to/from scanner (please check that this scanner has been installed and configured (e.g., with correct permissions): " + e);
            handleException(zEnv, zHandler, zDatas, e);
            return null;
        }
        catch (InterruptedException e)
        {
            /* let message pass through as is */
            zLog.error("Unable to scan e-mail message due to interruption: " + e);
            handleException(zEnv, zHandler, zDatas, e);
            return null;
        }

        try
        {
            zMsg.putDataBuffers(zNewDatas, true);
        }
        catch (ParseException e)
        {
            zLog.error("Unable to parse e-mail message after scanning it: " + e);
            handleException(zEnv, zHandler, zNewDatas, e);
            return null;
        }

        if (true == zMsg.isSpam())
        {
            zLog.debug("e-mail message contains spam");
            return Boolean.TRUE;
        }
        else
        {
            zLog.debug("e-mail message does not contain spam");
            return Boolean.FALSE;
        }
    }

    /* scan message and if requested, replace parts that contain virus */
    private VirusScannerResult scan(XMSEnv zEnv, XMailScannerCache zCache, MLHandler zHandler, VirusScanner zScanner, boolean bReplace)
    {
        MLMessage zMsg = zHandler.getMsg();

        VirusScannerResult zScanResult;

        try
        {
            if (null == zMsg.getMIMEBody())
            {
                /* message does not contain MIME body so
                 * it does not contain any virus
                 */
                zScanResult = null;
            }
            else
            {
                zLog.debug("scan is starting");
                zScanResult = zHandler.scan(zCache, zScanner, bReplace);
                zLog.debug("scan is complete");
            }
        }
        catch (ModifyException e)
        {
            zLog.error("Unable to remove virus from e-mail message: " + e);
            handleException(zEnv, zHandler, e);
            return null;
        }
        catch (IOException e)
        {
            /* let message pass through as is */
            zLog.error("Unable to send e-mail message to scanner (please check that this scanner has been installed and configured (e.g., with correct permissions): " + e);
            handleException(zEnv, zHandler, e);
            return null;
        }
        catch (InterruptedException e)
        {
            /* let message pass through as is */
            zLog.error("Unable to scan e-mail message due to interruption: " + e);
            handleException(zEnv, zHandler, e);
            return null;
        }

        if (null == zScanResult)
        {
            zLog.debug("e-mail message was not scanned for virus (replace: " + bReplace + ")");
            return VirusScannerResult.CLEAN;
        }

        if (false == zScanResult.isClean())
        {
            zLog.debug("e-mail message contains virus (replace: " + bReplace + ")");
        }
        else
        {
            zLog.debug("e-mail message does not contain virus (replace: " + bReplace + ")");
        }

        return zScanResult;
    }

    /* scan message only and retrieve results of scan */
    private VirusScannerResult scan(XMSEnv zEnv, MLHandler zHandler, VirusScanner zScanner)
    {
        MLMessage zMsg = zHandler.getMsg();
        ArrayList zDatas = zMsg.getDataBuffers();
        if (null == zDatas)
        {
            /* message is empty - should never occur */
            return VirusScannerResult.CLEAN;
        }

        VirusScannerResult zScanResult;

        try
        {
            if (null == zMsg.getMIMEBody())
            {
                /* message does not contain MIME body so
                 * it does not contain any virus
                 */
                zScanResult = null;
            }
            else
            {
                zLog.debug("scan is starting");
                zScanResult = zScanner.scanBufs(zDatas);
                zLog.debug("scan is complete");
            }
        }
        catch (IOException e)
        {
            /* let message pass through as is */
            zLog.error("Unable to send/retrieve e-mail message to/from scanner (please check that this scanner has been installed and configured (e.g., with correct permissions): " + e);
            handleException(zEnv, zHandler, zDatas, e);
            return null;
        }
        catch (InterruptedException e)
        {
            /* let message pass through as is */
            zLog.error("Unable to scan e-mail message due to interruption: " + e);
            handleException(zEnv, zHandler, zDatas, e);
            return null;
        }

        try
        {
            zMsg.putDataBuffers(zDatas, true);
        }
        catch (ParseException e)
        {
            zLog.error("Unable to parse e-mail message after scanning it: " + e);
            handleException(zEnv, zHandler, zDatas, e);
            return null;
        }

        if (null == zScanResult)
        {
            zLog.debug("e-mail message was not scanned for virus (scan only)");
            return VirusScannerResult.CLEAN;
        }

        if (false == zScanResult.isClean())
        {
            zLog.debug("e-mail message contains virus (scan only)");
        }
        else
        {
            zLog.debug("e-mail message does not contain virus (scan only)");
        }

        return zScanResult;
    }

    /* note that since we send notification from machine where MPipe runs and
     * MPipe doesn't capture traffic that
     * this machine directly originates or receives,
     * we don't have to worry about recursive looping
     * (capturing notification and resending notification ...)
     */
    private Action notify(XMailScannerSession zXMSession, MLMessage zMsg, int iNotify, int iType)
    {
        ArrayList zList = new ArrayList();

        ArrayList zTmp;
        boolean bNotifySender;

        if (Constants.NTFYSENDR_TYPE == (iNotify & Constants.NTFYSENDR_TYPE))
        {
            /* sender should be same as from so if we have sender, skip from */
            CBufferWrapper zCLine = zMsg.getSender();
            if (null != zCLine)
            {
                zList.add(zCLine);
                zLog.debug("notify sender: " + zCLine);
            }
            else
            {
                zTmp = zMsg.getFrom();
                if (null != zTmp &&
                    false == zTmp.isEmpty())
                {
                    zList.addAll(zTmp);
                    zLog.debug("notify from: " + zTmp);
                }
            }

            bNotifySender = true;
        }
        else
        {
            bNotifySender = false;
        }

        boolean bNotifyReceiver;

        if (Constants.NTFYRECVR_TYPE == (iNotify & Constants.NTFYRECVR_TYPE))
        {
            /* recipient should be same as tolist, cclist, and bcclist so
             * if we have recipient, skip other lists
             */
            zTmp = zMsg.getRcpt();
            if (null != zTmp &&
                false == zTmp.isEmpty())
            {
                zList.addAll(zTmp);
                zLog.debug("using recipient list");
            }
            else
            {
                zTmp = zMsg.getToList();
                if (null != zTmp &&
                    false == zTmp.isEmpty())
                {
                    zList.addAll(zTmp);
                    zLog.debug("using to list");
                }

                zTmp = zMsg.getCcList();
                if (null != zTmp &&
                    false == zTmp.isEmpty())
                {
                    zList.addAll(zTmp);
                    zLog.debug("using cc list");
                }

                zTmp = zMsg.getBccList();
                if (null != zTmp &&
                    false == zTmp.isEmpty())
                {
                    zList.addAll(zTmp);
                    zLog.debug("using bcc list");
                }
            }

            bNotifyReceiver = true;
        }
        else
        {
            bNotifyReceiver = false;
        }

        if (true == zList.isEmpty())
        {
            zLog.error("Sender or receiver recipients not specified; ignoring");
            return Action.PASS;
        }

        String azRecipients[] = buildRcpt(zList);
        if (null == azRecipients)
        {
            zLog.error("Unable to create recipient list for notification");
            return Action.PASS;
        }

        zList = zMsg.getData();
        ArrayList zNewList = new ArrayList(zList.size());

        CBufferWrapper zCLine;
        ByteBuffer zLine;

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zLine = zCLine.get();
            zLine.rewind();

            zNewList.add(zLine);
        }

        String zSubject;
        String zBody;

        switch(iType)
        {
        case SPAM:
            zSubject = SPAMSUBJNOTIFY;
            zBody = SPAMBODYNOTIFY;
            zLog.debug("Sending spam notify message");
            break;

        case VIRUS:
            zSubject = VIRUSSUBJNOTIFY;
            zBody = VIRUSBODYNOTIFY;
            zLog.debug("Sending virus notify message");
            break;

        default:
            zLog.error("Spam or virus notification not specified; ignoring");
            zNewList.clear();
            azRecipients = null;
            return Action.PASS;
        }

        zXMSession.getMailSender().sendMessageWithAttachment(azRecipients, zSubject, zBody, zNewList);

        if (true == bNotifySender &&
            true == bNotifyReceiver)
        {
            return Action.BLOCK_AND_WARN_BOTH;
        }
        else if (true == bNotifySender)
        {
            return Action.BLOCK_AND_WARN_SENDER;
        }
        else
        {
            return Action.BLOCK_AND_WARN_RECEIVER;
        }
    }

    private String[] buildRcpt(ArrayList zList)
    {
        CBufferWrapper zCLine;
        ByteBuffer zLine;
        Matcher zMatcher;
        int iPosition;
        int iLimit;

        CharsetDecoder zDecoder = Constants.CHARSET.newDecoder();
        String azSLines[] = new String[zList.size()];
        int iIdx = 0;

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext();)
        {
            zCLine = (CBufferWrapper) zIter.next();

            zMatcher = Constants.ANYCMDP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                zLine = zCLine.get();

                /* create snapshot of ByteBuffer state */
                iPosition = zLine.position();
                iLimit = zLine.limit();

                /* we found field command so skip over it */
                zLine.position(zMatcher.end());
                zLine.limit(iPosition);

                try
                {
                    azSLines[iIdx] = (zDecoder.decode(zLine).toString()).trim();
                }
                catch (CharacterCodingException e)
                {
                    zLog.error("Unable to decode notification recipient: " + zCLine + ": " + e);

                    /* restore ByteBuffer state */
                    zLine.limit(iLimit);
                    zLine.position(iPosition);
                    return null;
                }

                /* restore ByteBuffer state */
                zLine.limit(iLimit);
                zLine.position(iPosition);
            }
            else
            {
                /* this text is part of folded field
                 * it only contains values (and has no field command)
                 */
                azSLines[iIdx] = (zCLine.toString()).trim();
            }

            zLog.debug("notify recipient[" + iIdx + "]: " + azSLines[iIdx]);
            iIdx++;
        }

        return azSLines;
    }

    private void handleException(XMSEnv zEnv, MLHandler zHandler, ArrayList zDatas, Exception zException)
    {
        /* we stripped backing data from message to scan data
         * - we'll restore original backing data now
         * - we will not bother to re-parse message
         */
        MLMessage zMsg = zHandler.getMsg();
        zMsg.putDataBuffers(zDatas);
        handleException(zEnv, zHandler, zException);
        return;
    }

    /* after we've handled any exception,
     * we setup handler to handle next message transaction
     */
    private void handleException(XMSEnv zEnv, MLHandler zHandler, Exception zException)
    {
        if (true == zEnv.getException())
        {
            return; /* don't continue if already handling an exception */
        }

        zEnv.setException(true);

        MLMessage zMsg = zHandler.getMsg();
        if (null == zMsg ||
            true == zMsg.isEmpty())
        {
            zEnv.setException(false);
            return;
        }

        int iDir;
        boolean bResend;

        if (true == (zException instanceof ReadException))
        {
            iDir = Constants.RDEXCDIR_VAL;
            bResend = false;
        }
        else if (true == (zException instanceof ParseException))
        {
            iDir = Constants.PRSEXCDIR_VAL;
            bResend = true;
        }
        else if (true == (zException instanceof ProtoException))
        {
            iDir = Constants.PROEXCDIR_VAL;
            bResend = false;
        }
        else if (true == (zException instanceof IOException))
        {
            iDir = Constants.IOEXCDIR_VAL;
            bResend = true;
        }
        else if (true == (zException instanceof ModifyException))
        {
            iDir = Constants.MDFEXCDIR_VAL;
            bResend = true;
        }
        else
        {
            /* new/unidentified exception
             * (someone handled new exception but didn't add it here yet
             *  - should it here now)
             */
            iDir = Constants.DEFAULTDIR_VAL;
            bResend = true;
        }

        if (true == bCopyOnException)
        {
            zMsg.toFile(iDir);
        }

        if (true == bResend)
        {
            zLog.debug("handleException: resend");
            zHandler.resend(zEnv);
        }
        else
        {
            zLog.debug("handleException: setup (drop current message)");
            zHandler.setup();
        }

        zEnv.setException(false);
        return;
    }

    private void setVirusScannerNotUsed(XMailScannerCache zCache)
    {
        switch(zCache.getAVScanner())
        {
        case Constants.FPROTAV_ID:
            zVirusScanner = new FProtScanner();
            zLog.debug("using F-Prot f-prot");
            break;

        case Constants.SOPHOSAV_ID:
            zVirusScanner = new SophosScanner();
            zLog.debug("using Sophos sweep");
            break;

        case Constants.HAURIAV_ID:
            zVirusScanner = new HauriScanner();
            zLog.debug("using Hauri virobot");
            break;

        case Constants.CLAMAV_ID:
            zVirusScanner = new ClamScanner();
            zLog.debug("using Clam clamscan");
            break;

        default:
            zVirusScanner = null;
            zLog.debug("no virus scanner");
            break;
        }

        return;
    }

    /* TEMPORARY - ignore cached settings
     * - if FProtAV is installed, then use FProtAV
     * - if FProtAV is not installed but SophosAV is installed,
     *   then use SophosAV
     * - if FProtAV and SophosAV are not installed but HauriAV is installed,
     *   then use HauriAV
     * - if FProtAV, SophosAV, and HauriAV are not installed but ClamAV is installed,
     *   then use ClamAV
     * - if FProtAV, SophosAV, HauriAV, and ClamAV are not installed,
     *   then do not scan for virus
     */
    private void setVirusScanner(XMailScannerCache zCache)
    {
        File zFile = new File("/usr/bin/f-prot");
        if (true == zFile.exists())
        {
            zVirusScanner = new FProtScanner();
            zCache.setAVScanner(Constants.FPROTAV_ID);
            zLog.debug("F-Prot f-prot found");
        }
        else
        {
            zFile = new File("/usr/bin/sweep");
            if (true == zFile.exists())
            {
                zVirusScanner = new SophosScanner();
                zCache.setAVScanner(Constants.SOPHOSAV_ID);
                zLog.debug("Sophos sweep found");
            }
            else
            {
                zFile = new File("/usr/bin/virobot");
                if (true == zFile.exists())
                {
                    zVirusScanner = new HauriScanner();
                    zCache.setAVScanner(Constants.HAURIAV_ID);
                    zLog.debug("Hauri virobot found");
                }
                else
                {
                    zFile = new File("/usr/bin/clamscan");
                    if (true == zFile.exists())
                    {
                        zVirusScanner = new ClamScanner();
                        zCache.setAVScanner(Constants.CLAMAV_ID);
                        zLog.debug("Clam clamscan found");
                    }
                    else
                    {
                        zVirusScanner = null;
                        zCache.setAVScanner(Constants.NOAV_ID);
                        zLog.debug("virus scanner not found");
                    }
                }
            }
        }

        return;
    }
}
