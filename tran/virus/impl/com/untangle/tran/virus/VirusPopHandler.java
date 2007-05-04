/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.virus;

import java.io.File;
import java.io.IOException;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.mail.papi.MIMEMessageT;
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.mail.papi.MailTransformSettings;
import com.untangle.tran.mail.papi.WrappedMessageGenerator;
import com.untangle.tran.mail.papi.pop.PopStateMachine;
import com.untangle.tran.mime.HeaderParseException;
import com.untangle.tran.mime.MIMEMessage;
import com.untangle.tran.mime.MIMEPart;
import com.untangle.tran.mime.MIMEUtil;
import com.untangle.tran.token.Token;
import com.untangle.tran.token.TokenException;
import com.untangle.tran.token.TokenResult;
import com.untangle.tran.util.TempFileFactory;
import org.apache.log4j.Logger;

public class VirusPopHandler extends PopStateMachine
{
    private final Logger logger = Logger.getLogger(getClass());

    /* no block counter */
    private final static int SCAN_COUNTER = Transform.GENERIC_0_COUNTER;
    private final static int PASS_COUNTER = Transform.GENERIC_2_COUNTER;
    private final static int REMOVE_COUNTER = Transform.GENERIC_3_COUNTER;

    private final VirusTransformImpl zTransform;
    private final VirusScanner zScanner;
    private final String zVendorName;

    private final WrappedMessageGenerator zWMsgGenerator;
    private final VirusMessageAction zMsgAction;
    private final boolean bScan;

    // constructors -----------------------------------------------------------

    VirusPopHandler(TCPSession session, VirusTransformImpl transform, MailExport zMExport)
    {
        super(session);

        zTransform = transform;
        zScanner = transform.getScanner();
        zVendorName = zScanner.getVendorName();

        MailTransformSettings zMTSettings = zMExport.getExportSettings();

        VirusPOPConfig zConfig;
        WrappedMessageGenerator zWMGenerator;

        if (!session.isInbound()) {
            zConfig = transform.getVirusSettings().getPOPInbound();
            zWMGenerator = zConfig.getMessageGenerator();
            lTimeout = zMTSettings.getPopInboundTimeout();
        } else {
            zConfig = transform.getVirusSettings().getPOPOutbound();
            zWMGenerator = zConfig.getMessageGenerator();
            lTimeout = zMTSettings.getPopOutboundTimeout();
        }

        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
        zWMsgGenerator = zWMGenerator;
        //logger.debug("scan: " + bScan + ", message action: " + zMsgAction + ", timeout: " + lTimeout);
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        MIMEPart azMPart[];

        if (true == bScan &&
            MIMEUtil.EMPTY_MIME_PARTS != (azMPart = MIMEUtil.getCandidateParts(zMMessage))) {
            zTransform.incrementCount(SCAN_COUNTER);

            TempFileFactory zTFFactory = new TempFileFactory(getPipeline());
            VirusScannerResult zFirstResult = null;

            VirusScannerResult zCurResult;
            File zMPFile;
            boolean bWrap;

            for (MIMEPart zMPart : azMPart) {
                if (true == MIMEUtil.shouldScan(zMPart)) {
                    try {
                        zMPFile = zMPart.getContentAsFile(zTFFactory, true);
                    } catch (IOException exn) {
                        /* we'll reuse original message */
                        throw new TokenException("cannot get message/mime part file: " + exn);
                    }

                    if (null != (zCurResult = scanFile(zMPFile)) &&
                        VirusMessageAction.REMOVE == zMsgAction) {
                        try {
                            MIMEUtil.removeChild(zMPart);
                        } catch (HeaderParseException exn) {
                            /* we'll reuse original message */
                            throw new TokenException("cannot remove message/mime part containing virus: " + exn);
                        }

                        if (null == zFirstResult) {
                            /* use 1st scan result to wrap message */
                            zFirstResult = zCurResult;
                        }
                    }
                }
            }

            if (null != zFirstResult) {
                zTransform.incrementCount(REMOVE_COUNTER);

                /* wrap infected message and rebuild message token */
                MIMEMessage zWMMessage = zWMsgGenerator.wrap(zMMessage, zFirstResult);
                try {
                    zMsgFile = zWMMessage.toFile(zTFFactory);

                    zMMessageT = new MIMEMessageT(zMsgFile);
                    zMMessageT.setMIMEMessage(zWMMessage);

                    /* do not dispose original message
                     * (wrapped message references original message)
                     */
                } catch (IOException exn) {
                    //XXXX - need to dispose wrapped message?
                    /* we'll reuse original message */
                    throw new TokenException("cannot create wrapped message file after removing virus: " + exn);
                }
            } else {
                zTransform.incrementCount(PASS_COUNTER);
            }
        } //else {
        //logger.debug("scan is not enabled or message contains no MIME parts");
        //}

        return new TokenResult(new Token[] { zMMessageT }, null);
    }

    private VirusScannerResult scanFile(File zFile) throws TokenException
    {
        try {
            VirusScannerResult zScanResult = zScanner.scanFile(zFile);
            VirusMailEvent event = new VirusMailEvent(zMsgInfo,
                                                      zScanResult,
                                                      zScanResult.isClean() ? VirusMessageAction.PASS : zMsgAction,
                                                      zVendorName);
            zTransform.log(event);

            if (false == zScanResult.isClean()) {
                return zScanResult;
            }
            /* else not infected - discard scan result */

            return null;
        } catch (Exception exn) {
            // Should never happen
            throw new TokenException("cannot scan message/mime part file: ", exn);
        }
    }
}
