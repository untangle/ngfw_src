
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

package com.metavize.tran.virus;

import java.io.File;
import java.io.IOException;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.pop.PopStateMachine;
import com.metavize.tran.mime.MIMEPart;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.util.TempFileFactory;
import org.apache.log4j.Logger;

public class VirusPopHandler extends PopStateMachine
{
    private final static Logger logger = Logger.getLogger(VirusPopHandler.class);
    private final static Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private final VirusScanner zScanner;
    private final String zVendorName;

    private final VirusMessageAction zMsgAction;
    private final boolean bScan;

    // constructors -----------------------------------------------------------

    VirusPopHandler(TCPSession session, VirusTransformImpl transform)
    {
        super(session);

        zScanner = transform.getScanner();
        zVendorName = zScanner.getVendorName();

        VirusPOPConfig zConfig;
        if (IntfConverter.INSIDE == session.clientIntf()) {
            zConfig = transform.getVirusSettings().getPOPInbound();
        } else {
            zConfig = transform.getVirusSettings().getPOPOutbound();
        }
        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
        //logger.debug("scan: " + bScan + ", message action: " + zMsgAction);
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        if (true == zMMessage.isMultipart()) {
            MIMEPart azMPart[] = zMMessage.getLeafParts(true);
            TempFileFactory zTFFactory = new TempFileFactory();

            File zMPFile;

            for (MIMEPart zMPart : azMPart) {
                if (false == zMPart.isMultipart()) {
                    try {
                        zMPFile = zMPart.getContentAsFile(zTFFactory, true);
                    } catch (IOException exn) {
                        throw new TokenException("cannot get message/mime part file: ", exn);
                    }

                    scanFile(zMPFile);
                }
            }
        }
        //else {
            //logger.debug("message contains no MIME parts");
        //}

        return new TokenResult(new Token[] { zMMessageT }, null);
    }

    private void scanFile(File zFile) throws TokenException
    {
        if (false == bScan) {
            return;
        }

        try {
            VirusScannerResult zScanResult = zScanner.scanFile(zFile.getPath());
            eventLogger.info(new VirusMailEvent(zMsgInfo, zScanResult, zMsgAction, zVendorName));

            if (false == zScanResult.isClean() &&
                VirusMessageAction.REMOVE == zMsgAction) {
                //XXXX remove virus/wrap message
            }
            /* else PASS - do nothing */

            return;
        }
        catch (IOException exn) {
            throw new TokenException("cannot scan message/mime part file: ", exn);
        }
        catch (InterruptedException exn) { // XXX deal with this in scanner
            throw new TokenException("scan interrupted: ", exn);
        }
    }
}
