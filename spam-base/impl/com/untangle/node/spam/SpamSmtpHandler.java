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

import static com.untangle.node.util.Ascii.CRLF;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mail.papi.quarantine.MailSummary;
import com.untangle.node.mail.papi.quarantine.QuarantineNodeView;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.mail.papi.smtp.Response;
import com.untangle.node.mail.papi.smtp.SmtpTransaction;
import com.untangle.node.mail.papi.smtp.sapi.BufferingSessionHandler;
import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.HeaderParseException;
import com.untangle.node.mime.LCString;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEOutputStream;
import com.untangle.node.mime.MIMEUtil;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.localapi.LocalIntfManager;
import org.apache.log4j.Logger;

/**
 * Protocol Handler which is called-back as scannable messages are encountered.
 */
public class SpamSmtpHandler extends BufferingSessionHandler {

    private final Logger m_logger = Logger.getLogger(SpamSmtpHandler.class);
    private final TempFileFactory m_fileFactory;

    private static final String MOD_SUB_TEMPLATE =
        "[SPAM] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);

    private static final LocalIntfManager intfManager = LocalUvmContextFactory.context().localIntfManager();
    
    private final SpamNodeImpl m_spamImpl;
    private final SpamSmtpConfig m_config;
    private final QuarantineNodeView m_quarantine;
    private final SafelistNodeView m_safelist;
    private final TCPSession m_session;

    // Now we also keep the salutation to help SpamAssassin evaluate.
    private String m_receivedBy;

    public SpamSmtpHandler(TCPSession session,
                           long maxClientWait,
                           long maxSvrWait,
                           SpamNodeImpl impl,
                           SpamSmtpConfig config,
                           QuarantineNodeView quarantine,
                           SafelistNodeView safelist)
    {
        super(config.getMsgSizeLimit(), maxClientWait, maxSvrWait, false);

        m_spamImpl = impl;
        m_quarantine = quarantine;
        m_safelist = safelist;
        m_config = config;
        m_session = session;
        m_fileFactory = new TempFileFactory(LocalUvmContextFactory.context().
                                            pipelineFoundry().getPipeline(session.id()));
    }

    /**
     * Method for subclasses (i.e. clamphish) to
     * set the
     * {@link com.untangle.node.mail.papi.quarantine.MailSummary#getQuarantineCategory category}
     * for a Quarantine submission.
     */
    protected String getQuarantineCategory()
    {
        return "SPAM";
    }

    /**
     * Method for subclasses (i.e. clamphish) to
     * set the
     * {@link com.untangle.node.mail.papi.quarantine.MailSummary#getQuarantineDetail detail}
     * for a Quarantine submission.
     */
    protected String getQuarantineDetail(SpamReport report)
    {
        // Make a nicely printed string for the UI.
        return String.format("%03.1f", report.getScore());
    }

    /**
     * Method for returning the generator used to mark messages
     */
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return this.msgGenerator;
    }
    
    @Override
    public BPMEvaluationResult blockPassOrModify(MIMEMessage msg, SmtpTransaction tx, MessageInfo msgInfo)
    {
        m_logger.debug("[handleMessageCanBlock]");

        // I'm incrementing the count, even if the message is too big
        // or cannot be converted to file
        // m_spamImpl.incrementScanCount();

        // Scan the message
        File f = messageToFile(msg, tx);
        if (f == null) {
            m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
            m_spamImpl.incrementPassCount();
            return PASS_MESSAGE;
        }

        if (f.length() > getGiveupSz()) {
            m_logger.debug("Message larger than " + getGiveupSz() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.OVERSIZE);
            m_spamImpl.incrementPassCount();
            return PASS_MESSAGE;
        }

        if (m_safelist.isSafelisted(tx.getFrom(), msg.getMMHeaders().getFrom(), tx.getRecipients(false))) {
            m_logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
            m_spamImpl.incrementPassCount();
            return PASS_MESSAGE;
        }

        try {
            if (!m_config.getScanWanMail() && intfManager.getIntfByArgon(m_session.serverIntf()).isWanInterface()) {
                m_logger.debug("Ignoring WAN-bound SMTP mail");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
                m_spamImpl.incrementPassCount();
                return PASS_MESSAGE;
            }
        }
        catch (ArgonException e) {
            m_logger.warn("Unable to lookup destination interface", e);
        }
        
        SpamReport report = scanFile(f);

        if (report == null) { // Handle error case
            if (m_config.getFailClosed()) {
                m_logger.warn("Error scanning message. Failing closed");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.BLOCK);
                m_spamImpl.incrementBlockCount();
                return TEMPORARILY_REJECT;
            } else {
                m_logger.warn("Error scanning message. Failing open");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
                m_spamImpl.incrementPassCount();
                return PASS_MESSAGE;
            }
        }

        boolean addSpamHeaders = m_config.getAddSpamHeaders();
        if (addSpamHeaders) {
            report.addHeaders(msg);
        }

        SmtpSpamMessageAction action = m_config.getMsgAction();

        // XXX This has been deprecated by the scan_wan_mail setting
        // Anything going out External MARK instead of QUARANTINE
        //         if (action == SmtpSpamMessageAction.QUARANTINE
        //                 && m_session.serverIntf() == IntfConstants.EXTERNAL_INTF) {
        //             // Change action now, as it'll make the event logs
        //             // more accurate
        //             m_logger.debug("Implicitly converting policy from \"QUARANTINE\"" +
        //                            " to \"MARK\" as we have a message going out external");
        //             action = SmtpSpamMessageAction.MARK;
        //         }

        if (m_config.getBlockSuperSpam()
            && m_config.getSuperSpamStrength() / 10.0f <= report.getScore()) {
            action = SmtpSpamMessageAction.BLOCK;
        }

        if (report.isSpam()) {// BEGIN SPAM
            m_logger.debug("Spam found");

            if (action == SmtpSpamMessageAction.PASS) {
                m_logger.debug("Although SPAM detected, pass message as-per policy");
                markHeaders(msg, report);
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                m_spamImpl.incrementPassCount();
                return new BPMEvaluationResult(msg);
            } else if (action == SmtpSpamMessageAction.MARK) {
                m_logger.debug("Marking message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.MARK);
                markHeaders(msg, report);
                m_spamImpl.incrementMarkCount();
                MIMEMessage wrappedMsg = this.getMsgGenerator().wrap(msg, tx, report);
                return new BPMEvaluationResult(wrappedMsg);
            } else if (action == SmtpSpamMessageAction.QUARANTINE) {
                m_logger.debug("Attempt to quarantine mail as-per policy");
                if (quarantineMail(msg, tx, report, f)) {
                    m_spamImpl.incrementQuarantineCount();
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.QUARANTINE);
                    return BLOCK_MESSAGE;
                } else {
                    m_logger.debug("Quarantine failed.  Fall back to mark");
                    m_spamImpl.incrementMarkCount();
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.MARK);
                    markHeaders(msg, report);
                    MIMEMessage wrappedMsg = this.getMsgGenerator().wrap(msg, tx, report);
                    return new BPMEvaluationResult(wrappedMsg);
                }
            } else {
                m_logger.debug("Blocking SPAM message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.BLOCK);
                m_spamImpl.incrementBlockCount();
                return BLOCK_MESSAGE;
            }
        } else {
            markHeaders(msg, report);
            postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
            m_logger.debug("Not spam");
            m_spamImpl.incrementPassCount();

            return new BPMEvaluationResult(msg);
        }
    }

    @Override
    public void handleOpeningResponse(Response resp, com.untangle.node.mail.papi.smtp.sapi.Session.SmtpResponseActions actions)
    {
        // Note the receivedBy
        String[] rargs = resp.getArgs();
        if (rargs == null || rargs.length < 1) {
            m_receivedBy = null;
        } else {
            m_receivedBy = rargs[0];
        }
        super.handleOpeningResponse(resp, actions);
    }

    @Override
    public BlockOrPassResult blockOrPass(MIMEMessage msg, SmtpTransaction tx, MessageInfo msgInfo)
    {

        m_logger.debug("[handleMessageCanNotBlock]");

        // m_spamImpl.incrementScanCount();

        // Scan the message
        File f = messageToFile(msg, tx);
        if (f == null) {
            m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
            m_spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        if (f.length() > getGiveupSz()) {
            m_logger.debug("Message larger than " + getGiveupSz() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.OVERSIZE);
            m_spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        if (m_safelist.isSafelisted(tx.getFrom(), msg.getMMHeaders().getFrom(), tx.getRecipients(false))) {
            m_logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
            m_spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        try {
            if (!m_config.getScanWanMail() && intfManager.getIntfByArgon(m_session.serverIntf()).isWanInterface()) {
                m_logger.debug("Ignoring WAN-bound SMTP mail");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
                m_spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            }
        }
        catch (ArgonException e) {
            m_logger.warn("Unable to lookup destination interface", e);
        }
        
        SpamReport report = scanFile(f);

        if (report == null) { // Handle error case
            if (m_config.getFailClosed()) {
                m_logger.warn("Error scanning message. Failing closed");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.BLOCK);
                m_spamImpl.incrementBlockCount();
                return BlockOrPassResult.TEMPORARILY_REJECT;
            } else {
                m_logger.warn("Error scanning message. Failing open");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
                m_spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            }
        }

        SmtpSpamMessageAction action = m_config.getMsgAction();

        // Anything going out External MARK instead of QUARANTINE
        if (action == SmtpSpamMessageAction.QUARANTINE
                && m_session.serverIntf() == IntfConstants.EXTERNAL_INTF) {
            // Change action now, as it'll make the event logs
            // more accurate
            m_logger.debug("Implicitly converting policy from \"QUARANTINE\"" +
                           " to \"MARK\" as we have a message going out external");
            action = SmtpSpamMessageAction.MARK;
        }

        // Check for the impossible-to-satisfy action of "REMOVE"
        if (action == SmtpSpamMessageAction.MARK) {
            // Change action now, as it'll make the event logs
            // more accurate
            m_logger.debug("Implicitly converting policy from \"MARK\"" +
                           " to \"PASS\" as we have already begun to trickle");
            action = SmtpSpamMessageAction.PASS;
        }

        if (m_config.getBlockSuperSpam()
            && m_config.getSuperSpamStrength() / 10.0f <= report.getScore()) {
            action = SmtpSpamMessageAction.BLOCK;
        }

        if (report.isSpam()) {
            m_logger.debug("Spam");

            if (action == SmtpSpamMessageAction.PASS) {
                m_logger.debug("Although SPAM detected, pass message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                m_spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            } else if (action == SmtpSpamMessageAction.MARK) {
                m_logger.debug("Cannot mark at this time.  Simply pass");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                m_spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            } else if (action == SmtpSpamMessageAction.QUARANTINE) {
                m_logger.debug("Attempt to quarantine mail as-per policy");
                if (quarantineMail(msg, tx, report, f)) {
                    m_logger.debug("Mail quarantined");
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.QUARANTINE);
                    m_spamImpl.incrementQuarantineCount();
                    return BlockOrPassResult.BLOCK;
                } else {
                    m_logger.debug("Quarantine failed.  Fall back to pass");
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                    m_spamImpl.incrementPassCount();
                    return BlockOrPassResult.PASS;
                }
            } else {
                m_logger.debug("Blocking SPAM message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.BLOCK);
                m_spamImpl.incrementBlockCount();
                return BlockOrPassResult.BLOCK;
            }
        } else {
            m_logger.debug("Not Spam");
            postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
            m_spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }
    }

    private void markHeaders(MIMEMessage msg, SpamReport report)
    {
        boolean addSpamHeaders = m_config.getAddSpamHeaders();
        if (addSpamHeaders) {
            try {
                msg.getMMHeaders().removeHeaderFields(new LCString(m_config.getHeaderName()));
                msg.getMMHeaders().addHeaderField(m_config.getHeaderName(),(report.isSpam() ? "YES" : "NO"));
            } catch (HeaderParseException shouldNotHappen) {
                m_logger.error(shouldNotHappen);
            }
        }
    }

    private SpamReport cleanReport()
    {
        return new SpamReport(new LinkedList<ReportItem>(), 0.0f, m_config.getStrength()/10.0f);
    }

    /**
     * ...name says it all...
     */
    private void postSpamEvent(MessageInfo msgInfo, SpamReport report, SmtpSpamMessageAction action)
    {

        SpamSmtpEvent spamEvent = new SpamSmtpEvent(msgInfo,
                                                    report.getScore(),
                                                    report.isSpam(),
                                                    action,
                                                    m_spamImpl.getScanner().getVendorName());
        m_spamImpl.log(spamEvent);
    }

    /**
     * Wrapper that handles exceptions, and returns
     * null if there is a problem
     */
    private File messageToFile(MIMEMessage msg, SmtpTransaction tx) {

        // Build the "fake" received header for SpamAssassin
        InetAddress clientAddr = getSession().getClientAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("Received: ");
        sb.append("from ").append(getHELOEHLOName()).
            append(" (").append(clientAddr.getHostName()).
            append(" [").append(clientAddr.getHostAddress()).append("])").append(CRLF);
        EmailAddress envFrom = tx.getFrom();
        if (envFrom != null) {
            String smtpEnvFrom = envFrom.toSMTPString();
            sb.append("\t(envelope-from ").append(smtpEnvFrom).append(")").append(CRLF);
        }
        sb.append("\tby ").append(m_receivedBy == null ? "untangle" : m_receivedBy).append("; ").append(MIMEUtil.getRFC822Date());


        File ret = null;
        FileOutputStream fOut = null;
        try {
            ret = m_fileFactory.createFile("spamc_ut");
            fOut = new FileOutputStream(ret);
            BufferedOutputStream bOut = new BufferedOutputStream(fOut);
            MIMEOutputStream mimeOut = new MIMEOutputStream(bOut);
            mimeOut.writeLine(sb.toString());
            msg.writeTo(mimeOut);
            mimeOut.flush();
            bOut.flush();
            fOut.flush();
            fOut.close();
            return ret;
        } catch (Exception ex) {
            try { fOut.close(); } catch (Exception ignore) {}
            try { ret.delete(); } catch (Exception ignore) {}
            m_logger.error("Exception writing MIME Message to file", ex);
            return null;
        }
    }

    /**
     * Wrapper method around the real scanner, which
     * swallows exceptions and simply returns null
     */
    private SpamReport scanFile(File f)
    {
        // Attempt scan
        try {
            SpamReport ret = m_spamImpl.getScanner()
                .scanFile(f, m_config.getStrength()/10.0f);
            return ret;
        } catch (Exception ex) {
            m_logger.error("Exception scanning message", ex);
            return null;
        }
    }

    private boolean quarantineMail(MIMEMessage msg, SmtpTransaction tx, SpamReport report, File file)
    {

        List<EmailAddress> addrList =
            tx.getRecipients(true);

        EmailAddress[] addresses =
            (EmailAddress[]) addrList.toArray(new EmailAddress[addrList.size()]);

        return m_quarantine.quarantineMail(file,
                                           new MailSummary(msg.getMMHeaders().getFrom()==null?
                                                           (tx.getFrom()==null?
                                                            "<>":
                                                            tx.getFrom().getAddress()):
                                                           msg.getMMHeaders().getFrom().getAddress(),
                                                           msg.getMMHeaders().getSubject(),
                                                           getQuarantineCategory(),
                                                           getQuarantineDetail(report),
                                                           msg.getAttachmentCount(),
                                                           file.length()),
                                           addresses);
    }
}
