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

package com.untangle.node.spam;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MIMEMessageT;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mail.papi.pop.PopStateMachine;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.mime.HeaderParseException;
import com.untangle.node.mime.LCString;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.vnet.TCPSession;

public class SpamPopHandler extends PopStateMachine
{
    private final Logger logger = Logger.getLogger(getClass());

    private final SpamNodeImpl zNode;
    private final SpamScanner zScanner;
    private final String zVendorName;

    private static final String MOD_SUB_TEMPLATE =
        "[SPAM] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);

    private final SafelistNodeView zSLNodeView;
    private final SpamPopConfig zConfig;
    private final SpamMessageAction zMsgAction;
    private final boolean bScan;
    private final int strength;
    private final int giveUpSize;

    protected SpamPopHandler(TCPSession session, SpamNodeImpl node,
                             MailExport zMExport)
    {
        super(session);

        zNode = node;
        zScanner = node.getScanner();
        zVendorName = zScanner.getVendorName();

        zSLNodeView = zMExport.getSafelistNodeView();

        MailNodeSettings zMTSettings = zMExport.getExportSettings();

        zConfig = node.getSettings().getPopConfig();
        lTimeout = zMTSettings.getPopTimeout();
        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
        strength = zConfig.getStrength();
        giveUpSize = zConfig.getMsgSizeLimit();
        if (logger.isDebugEnabled()) {
            logger.debug("scan: " + bScan + ", message action: " + zMsgAction + ", timeout: " + lTimeout);
        }
    }

    protected TokenResult scanMessage() throws TokenException
    {
        logger.debug("SCAN MESSAGE!");
        if (giveUpSize < zMsgFile.length()) {
            postSpamEvent(zMsgInfo, cleanReport(), SpamMessageAction.OVERSIZE);
            zNode.incrementPassCount();
        } else if (true == zSLNodeView.isSafelisted(null, zMMessage.getMMHeaders().getFrom(), null)) {
            postSpamEvent(zMsgInfo, cleanReport(), SpamMessageAction.SAFELIST);
            zNode.incrementPassCount();
        } else if (true == bScan) {
            SpamReport zReport;

            if (null != (zReport = scanFile(zMsgFile)) &&
                SpamMessageAction.MARK == zMsgAction) {
                zNode.incrementMarkCount();

                /* wrap spam message and rebuild message token */
                MIMEMessage zWMMessage = this.getMsgGenerator().wrap(zMMessage, zReport);
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
                zNode.incrementPassCount();
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("scan is not enabled");
            }
        }

        return new TokenResult(new Token[] { zMMessageT }, null);
    }

    private SpamReport scanFile(File zFile) throws TokenException
    {
        try {
            SpamReport zReport = zScanner.scanFile(zFile, strength / 10.0f);
            postSpamEvent(zMsgInfo, zReport, zMsgAction);

            if (zConfig.getAddSpamHeaders()) {
                try {
                    zMMessage.getMMHeaders().removeHeaderFields(new LCString(zConfig.getHeaderName()));
                    zMMessage.getMMHeaders().addHeaderField(zConfig.getHeaderName(),(zReport.isSpam() ? "YES" : "NO"));
                }
                catch (HeaderParseException exn) {
                    /* we'll reuse original message */
                    throw new TokenException("cannot add spam report header to scanned message/mime part: " + exn);
                }
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

    private SpamReport cleanReport() {
        return new SpamReport(new LinkedList<ReportItem>(), 0.0f, strength/10.0f);
    }

    private void postSpamEvent(MessageInfo msgInfo, SpamReport report, SpamMessageAction action) {
        SpamLogEvent event = new SpamLogEvent(msgInfo,
                                              report.getScore(),
                                              report.isSpam(),
                                              report.isSpam() ? action : SpamMessageAction.PASS,
                                              zVendorName);
        zNode.log(event);
    }

    /**
     * Method for returning the generator used to mark messages
     */
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return SpamPopHandler.msgGenerator;
    }

}
