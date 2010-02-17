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
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mail.papi.imap.BufferingImapTokenStreamHandler;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.mime.HeaderParseException;
import com.untangle.node.mime.LCString;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.vnet.TCPSession;

public class SpamImapHandler extends BufferingImapTokenStreamHandler
{
    private final Logger m_logger = Logger.getLogger(SpamImapHandler.class);

    private final SpamNodeImpl m_spamImpl;
    private final SpamImapConfig m_config;
    private final TempFileFactory m_fileFactory;
    private final SafelistNodeView m_safelist;

    private static final String MOD_SUB_TEMPLATE =
        "[SPAM] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);
    
    public SpamImapHandler(TCPSession session,
                           long maxClientWait,
                           long maxSvrWait,
                           SpamNodeImpl impl,
                           SpamImapConfig config,
                           SafelistNodeView safelist) {

        super(maxClientWait, maxSvrWait, config.getMsgSizeLimit());

        m_spamImpl = impl;
        m_safelist = safelist;
        m_config = config;
        m_fileFactory = new TempFileFactory(LocalUvmContextFactory.context().
                                            pipelineFoundry().getPipeline(session.id()));
    }

    @Override
    public HandleMailResult handleMessage(MIMEMessage msg,
                                          MessageInfo msgInfo) {
        m_logger.debug("[handleMessage]");

        //I'm incrementing the count, even if the message is too big
        //or cannot be converted to file
        //m_spamImpl.incrementScanCount(); node can only have 4 metrics at this time - KenH, 8/15/08

        //Scan the message
        File f = messageToFile(msg);
        if(f == null) {
            m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            m_spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        if(f.length() > m_config.getMsgSizeLimit()) {
            m_logger.debug("Message larger than " + m_config.getMsgSizeLimit() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.OVERSIZE);
            m_spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        if(m_safelist.isSafelisted(null, msg.getMMHeaders().getFrom(), null)) {
            m_logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.SAFELIST);
            m_spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        SpamMessageAction action = m_config.getMsgAction();
        SpamReport report = scanFile(f);
        //Handle error case
        if(report == null) {
            m_logger.warn("Error scanning message.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            m_spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        boolean addSpamHeaders = m_config.getAddSpamHeaders();
        if (addSpamHeaders) {
            report.addHeaders(msg);
        }

        postSpamEvent(msgInfo, report, action);

        //Mark headers regardless of other actions
        if (m_config.getAddSpamHeaders()) {
            try {
                msg.getMMHeaders().removeHeaderFields(new LCString(m_config.getHeaderName()));
                msg.getMMHeaders().addHeaderField(m_config.getHeaderName(),(report.isSpam() ? "YES" : "NO"));
            } catch(HeaderParseException shouldNotHappen) {
                m_logger.error(shouldNotHappen);
            }
        }

        if(report.isSpam()) {//BEGIN SPAM
            m_logger.debug("Spam found");

            if(action == SpamMessageAction.PASS) {
                m_logger.debug("Although SPAM detected, pass message as-per policy");
                m_spamImpl.incrementPassCount();
                return HandleMailResult.forReplaceMessage(msg);
            }
            else {
                m_logger.debug("Marking message as-per policy");
                m_spamImpl.incrementMarkCount();
                MIMEMessage wrappedMsg = this.getMsgGenerator().wrap(msg, report);
                return HandleMailResult.forReplaceMessage(wrappedMsg);
            }
        }//ENDOF SPAM
        else {//BEGIN HAM
            m_logger.debug("Not spam");
            m_spamImpl.incrementPassCount();
            report.addHeaders(msg);
            return HandleMailResult.forReplaceMessage(msg);
        }//ENDOF HAM
    }

    private SpamReport cleanReport() {
        return new SpamReport(new LinkedList<ReportItem>(), 0.0f, m_config.getStrength()/10.0f);
    }

    /**
     * ...name says it all...
     */
    private void postSpamEvent(MessageInfo msgInfo,
                               SpamReport report,
                               SpamMessageAction action) {

        //Create an event for the reports
        SpamLogEvent spamEvent = new SpamLogEvent(
                                                  msgInfo,
                                                  report.getScore(),
                                                  report.isSpam(),
                                                  report.isSpam() ? action : SpamMessageAction.PASS,
                                                  m_spamImpl.getScanner().getVendorName());
        m_spamImpl.log(spamEvent);
    }

    /**
     * Wrapper that handles exceptions, and returns
     * null if there is a problem
     */
    private File messageToFile(MIMEMessage msg) {
        //Get the part as a file
        try {
            return msg.toFile(m_fileFactory);
        }
        catch(Exception ex) {
            m_logger.error("Exception writing MIME Message to file", ex);
            return null;
        }
    }

    /**
     * Wrapper method around the real scanner, which
     * swallows exceptions and simply returns null
     */
    private SpamReport scanFile(File f) {
        //Attempt scan
        try {
            SpamReport ret = m_spamImpl.getScanner()
                .scanFile(f, m_config.getStrength() / 10.0f);
            return ret;
        }
        catch(Exception ex) {
            m_logger.error("Exception scanning message", ex);
            return null;
        }
    }

    /**
     * Method for returning the generator used to mark messages
     */
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return this.msgGenerator;
    }

}
