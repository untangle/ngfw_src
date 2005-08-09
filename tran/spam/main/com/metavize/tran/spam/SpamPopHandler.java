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

package com.metavize.tran.spam;

import java.io.File;
import java.io.IOException;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailTransformSettings;
import com.metavize.tran.mail.papi.MIMEMessageT;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import com.metavize.tran.mail.papi.pop.PopStateMachine;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.LCString;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEUtil;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.util.FileFactory;
import org.apache.log4j.Logger;

public class SpamPopHandler extends PopStateMachine
{
    private final static Logger logger = Logger.getLogger(SpamPopHandler.class);
    private final static Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private final static String SPAM_HDR_NAME = "X-Spam-Flag";
    private final static LCString SPAM_HDR_NAME_LC = new LCString(SPAM_HDR_NAME);
    private final static String IS_SPAM_HDR_VALUE = "YES";
    private final static String IS_HAM_HDR_VALUE = "NO";

    /* no block counter */
    private final static int SCAN_COUNTER = Transform.GENERIC_0_COUNTER;
    private final static int PASS_COUNTER = Transform.GENERIC_2_COUNTER;
    private final static int MARK_COUNTER = Transform.GENERIC_3_COUNTER;

    private final SpamImpl zTransform;
    private final SpamScanner zScanner;
    private final String zVendorName;

    private final WrappedMessageGenerator zWMsgGenerator;
    private final SpamMessageAction zMsgAction;
    private final boolean bScan;
    private final int strength; 
    private final int giveUpSize;

    // constructors -----------------------------------------------------------

    SpamPopHandler(TCPSession session, SpamImpl transform, MailExport zMExport)
    {
        super(session);

        zTransform = transform;
        zScanner = transform.getScanner();
        zVendorName = zScanner.getVendorName();

        MailTransformSettings zMTSettings = zMExport.getExportSettings();

        SpamPOPConfig zConfig;
        WrappedMessageGenerator zWMGenerator;

        if (IntfConverter.INSIDE == session.clientIntf()) {
            zConfig = transform.getSpamSettings().getPOPInbound();
            zWMGenerator = new WrappedMessageGenerator(SpamSettings.IN_MOD_SUB_TEMPLATE, SpamSettings.IN_MOD_BODY_TEMPLATE);
            lTimeout = zMTSettings.getPopInboundTimeout();
        } else {
            zConfig = transform.getSpamSettings().getPOPOutbound();
            zWMGenerator = new WrappedMessageGenerator(SpamSettings.OUT_MOD_SUB_TEMPLATE, SpamSettings.OUT_MOD_BODY_TEMPLATE);
            lTimeout = zMTSettings.getPopOutboundTimeout();
        }
        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
        strength = zConfig.getStrength();
        giveUpSize = zConfig.getMsgSizeLimit();
        zWMsgGenerator = zWMGenerator;
        //logger.debug("scan: " + bScan + ", message action: " + zMsgAction + ", timeout: " + lTimeout);
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        if (true == bScan &&
            giveUpSize >= zMsgFile.length()) {
            zTransform.incrementCount(SCAN_COUNTER);

            SpamReport zReport;

            if (null != (zReport = scanFile(zMsgFile)) &&
                SpamMessageAction.MARK == zMsgAction) {
                zTransform.incrementCount(MARK_COUNTER);

                /* wrap spam message and rebuild message token */
                MIMEMessage zWMMessage = zWMsgGenerator.wrap(zMMessage, zReport);
                try {
                    zMsgFile = zWMMessage.toFile(new FileFactory() {
                        public File createFile(String name) throws IOException {
                            return createFile();
                        }

                        public File createFile() throws IOException {
                            return getPipeline().mktemp();
                        }
                    } );

                    zMMessageT = new MIMEMessageT(zMsgFile);
                    zMMessageT.setMIMEMessage(zWMMessage);

                    /* do not dispose original message
                     * (wrapped message references original message)
                     */
                } catch (IOException exn) {
                    /* we'll reuse original message */
                    //XXXX - need to dispose wrapped message?
                    throw new TokenException("cannot create wrapped message file after marking message as spam: " + exn);
                }
            } else {
                zTransform.incrementCount(PASS_COUNTER);
            }
        } //else {
            //logger.debug("scan is not enabled");
        //}

        return new TokenResult(new Token[] { zMMessageT }, null);
    }

    private SpamReport scanFile(File zFile) throws TokenException
    {
        try {
            SpamReport zReport = zScanner.scanFile(zFile, strength/10.0f);
            eventLogger.info(new SpamLogEvent(zMsgInfo, zReport.getScore(), zReport.isSpam(), zMsgAction, zVendorName));

            try {
                zMMessage.getMMHeaders().removeHeaderFields(SPAM_HDR_NAME_LC);
                zMMessage.getMMHeaders().addHeaderField(SPAM_HDR_NAME, true == zReport.isSpam() ? IS_SPAM_HDR_VALUE : IS_HAM_HDR_VALUE);
            }
            catch (HeaderParseException exn) {
                /* we'll reuse original message */
                throw new TokenException("cannot add spam report header to scanned message/mime part: " + exn);
            }

            if (true == zReport.isSpam()) {
                return zReport;
            }
            /* else not spam - do nothing */

            return null;
        } catch (Exception exn) { // Should never happen
            /* we'll reuse original message */
            throw new TokenException("cannot scan message/mime part file: " + exn);
        }
    }
}
