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

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.imap.BufferingImapTokenStreamHandler;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.mime.HeaderParseException;
import com.untangle.node.mime.LCString;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.util.TempFileFactory;
import org.apache.log4j.Logger;

public class SpamImapHandler
    extends BufferingImapTokenStreamHandler {

    private final Logger m_logger =
        Logger.getLogger(SpamImapHandler.class);

    private final SpamImpl m_spamImpl;
    private final SpamIMAPConfig m_config;
    private final TempFileFactory m_fileFactory;
    private final SafelistNodeView m_safelist;

    public SpamImapHandler(TCPSession session,
                           long maxClientWait,
                           long maxSvrWait,
                           SpamImpl impl,
                           SpamIMAPConfig config,
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
        m_spamImpl.incrementScanCounter();

        //Scan the message
        File f = messageToFile(msg);
        if(f == null) {
            m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            m_spamImpl.incrementPassCounter();
            return HandleMailResult.forPassMessage();
        }

        if(f.length() > m_config.getMsgSizeLimit()) {
            m_logger.debug("Message larger than " + m_config.getMsgSizeLimit() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.OVERSIZE);
            m_spamImpl.incrementPassCounter();
            return HandleMailResult.forPassMessage();
        }

        if(m_safelist.isSafelisted(null, msg.getMMHeaders().getFrom(), null)) {
            m_logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.SAFELIST);
            m_spamImpl.incrementPassCounter();
            return HandleMailResult.forPassMessage();
        }

        SpamMessageAction action = m_config.getMsgAction();
        SpamReport report = scanFile(f);
        //Handle error case
        if(report == null) {
            m_logger.error("Error scanning message.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            m_spamImpl.incrementPassCounter();
            return HandleMailResult.forPassMessage();
        }

        postSpamEvent(msgInfo, report, action);

        //Mark headers regardless of other actions
        try {
            msg.getMMHeaders().removeHeaderFields(new LCString(m_config.getHeaderName()));
            msg.getMMHeaders().addHeaderField(m_config.getHeaderName(),
                                              m_config.getHeaderValue(report.isSpam()));
        }
        catch(HeaderParseException shouldNotHappen) {
            m_logger.error(shouldNotHappen);
        }

        if(report.isSpam()) {//BEGIN SPAM
            m_logger.debug("Spam found");

            if(action == SpamMessageAction.PASS) {
                m_logger.debug("Although SPAM detected, pass message as-per policy");
                m_spamImpl.incrementPassCounter();
                return HandleMailResult.forPassMessage();
            }
            else {
                m_logger.debug("Marking message as-per policy");
                m_spamImpl.incrementMarkCounter();
                MIMEMessage wrappedMsg = m_config.getMessageGenerator().wrap(msg, report);
                return HandleMailResult.forReplaceMessage(wrappedMsg);
            }
        }//ENDOF SPAM
        else {//BEGIN HAM
            m_logger.debug("Not spam");
            m_spamImpl.incrementPassCounter();
            return HandleMailResult.forPassMessage();
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
            SpamReport ret = m_spamImpl.getScanner().scanFile(f, m_config.getStrength()/10.0f);
            if(ret == null) {
                m_logger.error("Received ERROR SpamReport");
                return null;
            }
            return ret;
        }
        catch(Exception ex) {
            m_logger.error("Exception scanning message", ex);
            return null;
        }
    }
}
