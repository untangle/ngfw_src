/*
 * $Id$
 */
package com.untangle.node.spam;

import static com.untangle.node.util.Ascii.CRLF;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

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
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Protocol Handler which is called-back as scannable messages are encountered.
 */
public class SpamSmtpHandler extends BufferingSessionHandler
{

    private final Logger logger = Logger.getLogger(SpamSmtpHandler.class);
    private final TempFileFactory fileFactory;

    private static final String MOD_SUB_TEMPLATE = "[SPAM] $MIMEMessage:SUBJECT$";
    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);

    private final SpamNodeImpl spamImpl;
    private final SpamSmtpConfig config;
    private final QuarantineNodeView quarantine;
    private final SafelistNodeView safelist;
    private final TCPSession session;

    private String receivedBy; // Now we also keep the salutation to help SpamAssassin evaluate.

    public SpamSmtpHandler(TCPSession session, long maxClientWait, long maxSvrWait, SpamNodeImpl impl, SpamSmtpConfig config, QuarantineNodeView quarantine, SafelistNodeView safelist)
    {
        super(config.getMsgSizeLimit(), maxClientWait, maxSvrWait, false);

        this.spamImpl = impl;
        this.quarantine = quarantine;
        this.safelist = safelist;
        this.config = config;
        this.session = session;
        this.fileFactory = new TempFileFactory(UvmContextFactory.context().pipelineFoundry().getPipeline(session.id()));
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
        return msgGenerator;
    }
    
    @Override
    public BPMEvaluationResult blockPassOrModify(MIMEMessage msg, SmtpTransaction tx, MessageInfo msgInfo)
    {
        logger.debug("[handleMessageCanBlock]");

        // I'm incrementing the count, even if the message is too big
        // or cannot be converted to file
        // spamImpl.incrementScanCount();

        // Scan the message
        File f = messageToFile(msg, tx);
        if (f == null) {
            logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
            spamImpl.incrementPassCount();
            return PASS_MESSAGE;
        }

        if (f.length() > getGiveupSz()) {
            logger.debug("Message larger than " + getGiveupSz() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.OVERSIZE);
            spamImpl.incrementPassCount();
            return PASS_MESSAGE;
        }

        if (safelist.isSafelisted(tx.getFrom(), msg.getMMHeaders().getFrom(), tx.getRecipients(false))) {
            logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
            spamImpl.incrementPassCount();
            return PASS_MESSAGE;
        }

        try {
            boolean isWan = UvmContextFactory.context().networkManager().getNetworkConfiguration().findById(session.serverIntf()).isWAN();
            if (!config.getScanWanMail() && isWan) {
                logger.debug("Ignoring WAN-bound SMTP mail");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.OUTBOUND);
                spamImpl.incrementPassCount();
                return PASS_MESSAGE;
            }
        }
        catch (Exception e) {
            logger.warn("Unable to lookup destination interface", e);
        }
        
        SpamReport report = scanFile(f);

        if (report == null) { // Handle error case
            if (config.getFailClosed()) {
                logger.warn("Error scanning message. Failing closed");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.BLOCK);
                spamImpl.incrementBlockCount();
                return TEMPORARILY_REJECT;
            } else {
                logger.warn("Error scanning message. Failing open");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
                spamImpl.incrementPassCount();
                return PASS_MESSAGE;
            }
        }

        if (config.getAddSpamHeaders()) {
            report.addHeaders(msg);
        }

        SmtpSpamMessageAction action = config.getMsgAction();

        if (config.getBlockSuperSpam()
            && config.getSuperSpamStrength() / 10.0f <= report.getScore()) {
            action = SmtpSpamMessageAction.BLOCK;
        }

        if (report.isSpam()) {// BEGIN SPAM
            logger.debug("Spam found");

            if (action == SmtpSpamMessageAction.PASS) {
                logger.debug("Although SPAM detected, pass message as-per policy");
                markHeaders(msg, report);
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                spamImpl.incrementPassCount();
                return new BPMEvaluationResult(msg);
            } else if (action == SmtpSpamMessageAction.MARK) {
                logger.debug("Marking message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.MARK);
                markHeaders(msg, report);
                spamImpl.incrementMarkCount();
                MIMEMessage wrappedMsg = this.getMsgGenerator().wrap(msg, tx, report);
                return new BPMEvaluationResult(wrappedMsg);
            } else if (action == SmtpSpamMessageAction.QUARANTINE) {
                logger.debug("Attempt to quarantine mail as-per policy");
                if (quarantineMail(msg, tx, report, f)) {
                    spamImpl.incrementQuarantineCount();
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.QUARANTINE);
                    return BLOCK_MESSAGE;
                } else {
                    logger.debug("Quarantine failed.  Fall back to mark");
                    spamImpl.incrementMarkCount();
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.MARK);
                    markHeaders(msg, report);
                    MIMEMessage wrappedMsg = this.getMsgGenerator().wrap(msg, tx, report);
                    return new BPMEvaluationResult(wrappedMsg);
                }
            } else {
                logger.debug("Blocking SPAM message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.BLOCK);
                spamImpl.incrementBlockCount();
                return BLOCK_MESSAGE;
            }
        } else {
            markHeaders(msg, report);
            postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
            logger.debug("Not spam");
            spamImpl.incrementPassCount();

            return new BPMEvaluationResult(msg);
        }
    }

    @Override
    public void handleOpeningResponse(Response resp, com.untangle.node.mail.papi.smtp.sapi.Session.SmtpResponseActions actions)
    {
        // Note the receivedBy
        String[] rargs = resp.getArgs();
        if (rargs == null || rargs.length < 1) {
            receivedBy = null;
        } else {
            receivedBy = rargs[0];
        }
        super.handleOpeningResponse(resp, actions);
    }

    @Override
    public BlockOrPassResult blockOrPass(MIMEMessage msg, SmtpTransaction tx, MessageInfo msgInfo)
    {

        logger.debug("[handleMessageCanNotBlock]");

        // spamImpl.incrementScanCount();

        // Scan the message
        File f = messageToFile(msg, tx);
        if (f == null) {
            logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
            spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        if (f.length() > getGiveupSz()) {
            logger.debug("Message larger than " + getGiveupSz() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.OVERSIZE);
            spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        if (safelist.isSafelisted(tx.getFrom(), msg.getMMHeaders().getFrom(), tx.getRecipients(false))) {
            logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
            spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        try {
            boolean isWan = UvmContextFactory.context().networkManager().getNetworkConfiguration().findById(session.serverIntf()).isWAN();
            if (!config.getScanWanMail() && isWan) {
                logger.debug("Ignoring WAN-bound SMTP mail");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.SAFELIST);
                spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            }
        }
        catch (Exception e) {
            logger.warn("Unable to lookup destination interface", e);
        }
        
        SpamReport report = scanFile(f);

        if (report == null) { // Handle error case
            if (config.getFailClosed()) {
                logger.warn("Error scanning message. Failing closed");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.BLOCK);
                spamImpl.incrementBlockCount();
                return BlockOrPassResult.TEMPORARILY_REJECT;
            } else {
                logger.warn("Error scanning message. Failing open");
                postSpamEvent(msgInfo, cleanReport(), SmtpSpamMessageAction.PASS);
                spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            }
        }

        SmtpSpamMessageAction action = config.getMsgAction();

        // Anything going out External MARK instead of QUARANTINE
        boolean isWan = UvmContextFactory.context().networkManager().getNetworkConfiguration().findById(session.serverIntf()).isWAN();
        if (action == SmtpSpamMessageAction.QUARANTINE && isWan) {
            // Change action now, as it'll make the event logs
            // more accurate
            logger.debug("Implicitly converting policy from \"QUARANTINE\"" + " to \"MARK\" as we have a message going out WAN");
            action = SmtpSpamMessageAction.MARK;
        }

        // Check for the impossible-to-satisfy action of "REMOVE"
        if (action == SmtpSpamMessageAction.MARK) {
            // Change action now, as it'll make the event logs
            // more accurate
            logger.debug("Implicitly converting policy from \"MARK\"" +
                           " to \"PASS\" as we have already begun to trickle");
            action = SmtpSpamMessageAction.PASS;
        }

        if (config.getBlockSuperSpam()
            && config.getSuperSpamStrength() / 10.0f <= report.getScore()) {
            action = SmtpSpamMessageAction.BLOCK;
        }

        if (report.isSpam()) {
            logger.debug("Spam");

            if (action == SmtpSpamMessageAction.PASS) {
                logger.debug("Although SPAM detected, pass message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            } else if (action == SmtpSpamMessageAction.MARK) {
                logger.debug("Cannot mark at this time.  Simply pass");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                spamImpl.incrementPassCount();
                return BlockOrPassResult.PASS;
            } else if (action == SmtpSpamMessageAction.QUARANTINE) {
                logger.debug("Attempt to quarantine mail as-per policy");
                if (quarantineMail(msg, tx, report, f)) {
                    logger.debug("Mail quarantined");
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.QUARANTINE);
                    spamImpl.incrementQuarantineCount();
                    return BlockOrPassResult.BLOCK;
                } else {
                    logger.debug("Quarantine failed.  Fall back to pass");
                    postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
                    spamImpl.incrementPassCount();
                    return BlockOrPassResult.PASS;
                }
            } else {
                logger.debug("Blocking SPAM message as-per policy");
                postSpamEvent(msgInfo, report, SmtpSpamMessageAction.BLOCK);
                spamImpl.incrementBlockCount();
                return BlockOrPassResult.BLOCK;
            }
        } else {
            logger.debug("Not Spam");
            postSpamEvent(msgInfo, report, SmtpSpamMessageAction.PASS);
            spamImpl.incrementPassCount();
            return BlockOrPassResult.PASS;
        }
    }

    private void markHeaders(MIMEMessage msg, SpamReport report)
    {
        if (config.getAddSpamHeaders()) {
            try {
                msg.getMMHeaders().removeHeaderFields(new LCString(config.getHeaderName()));
                msg.getMMHeaders().addHeaderField(config.getHeaderName(),(report.isSpam() ? "YES" : "NO"));
            } catch (HeaderParseException shouldNotHappen) {
                logger.error(shouldNotHappen);
            }
        }
    }

    private SpamReport cleanReport()
    {
        return new SpamReport(new LinkedList<ReportItem>(), 0.0f, config.getStrength()/10.0f);
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
                                                    spamImpl.getScanner().getVendorName());
        spamImpl.logEvent(spamEvent);
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
        sb.append("\tby ").append(receivedBy == null ? "untangle" : receivedBy).append("; ").append(MIMEUtil.getRFC822Date());


        File ret = null;
        FileOutputStream fOut = null;
        try {
            ret = fileFactory.createFile("spamc_ut");
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
            logger.error("Exception writing MIME Message to file", ex);
            return null;
        }
    }

    /**
     * Wrapper method around the real scanner, which
     * swallows exceptions and simply returns null
     */
    private SpamReport scanFile(File f)
    {
        try {
            SpamReport ret = spamImpl.getScanner().scanFile(f, config.getStrength()/10.0f);
            return ret;
        } catch (Exception ex) {
            logger.error("Exception scanning message", ex);
            return null;
        }
    }

    private boolean quarantineMail(MIMEMessage msg, SmtpTransaction tx, SpamReport report, File file)
    {
        List<EmailAddress> addrList = tx.getRecipients(true);
        EmailAddress[] addresses = addrList.toArray(new EmailAddress[addrList.size()]);

        return quarantine.quarantineMail(file, new MailSummary(msg.getMMHeaders().getFrom()==null?
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
