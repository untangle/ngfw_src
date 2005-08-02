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
import com.metavize.tran.mail.papi.pop.PopStateMachine;
import com.metavize.tran.mail.papi.MIMEMessageT;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;
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

    private final static float SPAM_SCORE = 5;

    private final SpamScanner zScanner;
    private final String zVendorName;

    private final WrappedMessageGenerator zWMsgGenerator;
    private final SpamMessageAction zMsgAction;
    private final boolean bScan;

    // constructors -----------------------------------------------------------

    SpamPopHandler(TCPSession session, SpamImpl transform)
    {
        super(session);

        zScanner = transform.getScanner();
        zVendorName = zScanner.getVendorName();

        SpamPOPConfig zConfig;
        WrappedMessageGenerator zWMGenerator;
        if (IntfConverter.INSIDE == session.clientIntf()) {
            zConfig = transform.getSpamSettings().getPOPInbound();
            zWMGenerator = new WrappedMessageGenerator(SpamSettings.IN_MOD_SUB_TEMPLATE, SpamSettings.IN_MOD_BODY_TEMPLATE);
        } else {
            zConfig = transform.getSpamSettings().getPOPOutbound();
            zWMGenerator = new WrappedMessageGenerator(SpamSettings.OUT_MOD_SUB_TEMPLATE, SpamSettings.OUT_MOD_BODY_TEMPLATE);
        }
        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
        zWMsgGenerator = zWMGenerator;
        //logger.debug("scan: " + bScan + ", message action: " + zMsgAction);
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        if (true == bScan) {
            SpamReport zReport;

            if (null != (zReport = scanFile(zMsgFile)) &&
                SpamMessageAction.MARK == zMsgAction) {
                /* wrap spam message and rebuild message token */
                MIMEMessage zWMMessage = zWMsgGenerator.wrap(zMMessage, zReport);
                try {
                    zMsgFile = zWMMessage.toFile(new FileFactory() {
                        public File createFile(String name) throws IOException {                          return createFile();
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
                } catch (IOException exn2) {
                    throw new TokenException("cannot wrap original message/mime part: ", exn2);
                }
            }
        }
        //else {
            //logger.debug("scan is not enabled");
        //}

        return new TokenResult(new Token[] { zMMessageT }, null);
    }

    private SpamReport scanFile(File zFile) throws TokenException
    {
        try {
            SpamReport zReport = zScanner.scanFile(zFile, SPAM_SCORE);
            eventLogger.info(new SpamLogEvent(zMsgInfo, zReport.getScore(), zReport.isSpam(), zMsgAction, zVendorName));

            try {
                zMMessage.getMMHeaders().removeHeaderFields(SPAM_HDR_NAME_LC);
                zMMessage.getMMHeaders().addHeaderField(SPAM_HDR_NAME, true == zReport.isSpam() ? IS_SPAM_HDR_VALUE : IS_HAM_HDR_VALUE);
            }
            catch (HeaderParseException exn) {
                logger.error(exn);
            }

            if (true == zReport.isSpam()) {
                return zReport;
            }
            /* else PASS - do nothing */

            return null;
        }
        catch (IOException exn) {
            throw new TokenException("cannot scan message/mime part file: ", exn);
        } catch (InterruptedException exn) { // XXX deal with this in scanner
            throw new TokenException("scan interrupted: ", exn);
        }
    }
}
