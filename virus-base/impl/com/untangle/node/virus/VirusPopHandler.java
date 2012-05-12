/**
 * $Id$
 */
package com.untangle.node.virus;

import java.io.File;
import java.io.IOException;

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
import com.untangle.uvm.vnet.NodeTCPSession;

public class VirusPopHandler extends PopStateMachine
{
    private static final String MOD_SUB_TEMPLATE =
        "[VIRUS] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Virus Blocker.\r\n";

    private final VirusNodeImpl node;
    private final VirusScanner scanner;
    private final String vendorName;

    private final WrappedMessageGenerator generator;
    private final String popAction;
    private final boolean scan;

    // constructors -----------------------------------------------------------

    VirusPopHandler(NodeTCPSession session, VirusNodeImpl node, MailExport zMExport)
    {
        super(session);

        this.node = node;
        scanner = node.getScanner();
        vendorName = scanner.getVendorName();

        MailNodeSettings zMTSettings = zMExport.getExportSettings();

        scan = node.getSettings().getScanPop();
        popAction = node.getSettings().getPopAction();
        generator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE, MOD_BODY_TEMPLATE);
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        MIMEPart azMPart[];

        if (true == scan &&
            MIMEUtil.EMPTY_MIME_PARTS != (azMPart = MIMEUtil.getCandidateParts(zMMessage))) {
            node.incrementScanCount();

            TempFileFactory zTFFactory = new TempFileFactory(getPipeline());
            VirusScannerResult zFirstResult = null;

            VirusScannerResult zCurResult;
            File zMPFile;

            for (MIMEPart zMPart : azMPart) {
                if (true == MIMEUtil.shouldScan(zMPart)) {
                    try {
                        zMPFile = zMPart.getContentAsFile(zTFFactory, true);
                    } catch (IOException exn) {
                        /* we'll reuse original message */
                        throw new TokenException("cannot get message/mime part file: " + exn);
                    }

                    if (null != (zCurResult = scanFile(zMPFile)) && "remove".equals(popAction)) {
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
                node.incrementRemoveCount();

                /* wrap infected message and rebuild message token */
                MIMEMessage zWMMessage = generator.wrap(zMMessage, zFirstResult);
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
                node.incrementPassCount();
            }
        } //else {
        //logger.debug("scan is not enabled or message contains no MIME parts");
        //}

        return new TokenResult(new Token[] { zMMessageT }, null);
    }

    private VirusScannerResult scanFile(File zFile) throws TokenException
    {
        try {
            VirusScannerResult zScanResult = scanner.scanFile(zFile);
            VirusMailEvent event = new VirusMailEvent(zMsgInfo,
                                                      zScanResult,
                                                      zScanResult.isClean() ? "pass" : popAction,
                                                      vendorName);
            node.logEvent(event);

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
