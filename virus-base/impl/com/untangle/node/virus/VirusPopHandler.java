/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.virus;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MIMEMessageT;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mail.papi.pop.PopStateMachine;
import com.untangle.node.mime.HeaderParseException;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEPart;
import com.untangle.node.mime.MIMEUtil;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.vnet.TCPSession;

public class VirusPopHandler extends PopStateMachine
{
    private final Logger logger = Logger.getLogger(getClass());

    private final VirusNodeImpl zNode;
    private final VirusScanner zScanner;
    private final String zVendorName;

    private final WrappedMessageGenerator zWMsgGenerator;
    private final VirusMessageAction zMsgAction;
    private final boolean bScan;

    // constructors -----------------------------------------------------------

    VirusPopHandler(TCPSession session, VirusNodeImpl node, MailExport zMExport)
    {
        super(session);

        zNode = node;
        zScanner = node.getScanner();
        zVendorName = zScanner.getVendorName();

        MailNodeSettings zMTSettings = zMExport.getExportSettings();

        VirusPOPConfig zConfig;
        WrappedMessageGenerator zWMGenerator;

        zConfig = node.getVirusSettings().getBaseSettings().getPopConfig();
        zWMGenerator = zConfig.getMessageGenerator();
        lTimeout = zMTSettings.getPopTimeout();

        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
        zWMsgGenerator = zWMGenerator;
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        MIMEPart azMPart[];

        if (true == bScan &&
            MIMEUtil.EMPTY_MIME_PARTS != (azMPart = MIMEUtil.getCandidateParts(zMMessage))) {
            zNode.incrementScanCount();

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
                zNode.incrementRemoveCount();

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
                zNode.incrementPassCount();
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
            zNode.log(event);

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
