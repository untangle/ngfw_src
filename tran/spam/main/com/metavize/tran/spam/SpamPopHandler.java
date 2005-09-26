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

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailTransformSettings;
import com.metavize.tran.mail.papi.MIMEMessageT;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import com.metavize.tran.mail.papi.pop.PopStateMachine;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEUtil;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.util.FileFactory;
import com.metavize.tran.util.TempFileFactory;
import com.metavize.tran.mime.LCString;
import org.apache.log4j.Logger;

public class SpamPopHandler extends PopStateMachine
{
    private final static Logger logger = Logger.getLogger(SpamPopHandler.class);
    private final static Logger eventLogger = MvvmContextFactory.context().eventLogger();

    /* no block counter */
    private final static int SCAN_COUNTER = Transform.GENERIC_0_COUNTER;
    private final static int PASS_COUNTER = Transform.GENERIC_2_COUNTER;
    private final static int MARK_COUNTER = Transform.GENERIC_3_COUNTER;

    private final SpamImpl zTransform;
    private final SpamScanner zScanner;
    private final String zVendorName;

    private final SpamMessageAction zMsgAction;
    private final SpamPOPConfig zConfig;
    private final boolean bScan;
    private final int strength; 
    private final int giveUpSize;

    private WrappedMessageGenerator zWMsgGenerator;

    // constructors -----------------------------------------------------------

    protected SpamPopHandler(TCPSession session, SpamImpl transform, MailExport zMExport)
    {
        super(session);

        zTransform = transform;
        zScanner = transform.getScanner();
        zVendorName = zScanner.getVendorName();

        MailTransformSettings zMTSettings = zMExport.getExportSettings();

        WrappedMessageGenerator zWMGenerator;

        if (!session.isInbound()) {
            zConfig = transform.getSpamSettings().getPOPInbound();
            zWMGenerator = zConfig.getMessageGenerator();
            lTimeout = zMTSettings.getPopInboundTimeout();
        } else {
            zConfig = transform.getSpamSettings().getPOPOutbound();
            zWMGenerator = zWMGenerator = zConfig.getMessageGenerator();
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
                    zMsgFile = zWMMessage.toFile(new TempFileFactory(getPipeline()));

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
            SpamLogEvent event = new SpamLogEvent(zMsgInfo,
                                                  zReport.getScore(),
                                                  zReport.isSpam(),
                                                  zReport.isSpam() ? zMsgAction : SpamMessageAction.PASS,
                                                  zVendorName);
            eventLogger.info(event);

            try {
                zMMessage.getMMHeaders().removeHeaderFields(new LCString(zConfig.getHeaderName()));
                zMMessage.getMMHeaders().addHeaderField(zConfig.getHeaderName(),
                  zConfig.getHeaderValue(zReport.isSpam()));
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
