/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import static com.untangle.uvm.util.Ascii.CRLF;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.MailExport;
import com.untangle.app.smtp.MailExportFactory;
import com.untangle.app.smtp.SmtpMessageEvent;
import com.untangle.app.smtp.Response;
import com.untangle.app.smtp.SmtpTransaction;
import com.untangle.app.smtp.TemplateTranslator;
import com.untangle.app.smtp.WrappedMessageGenerator;
import com.untangle.app.smtp.handler.ScannedMessageResult;
import com.untangle.app.smtp.handler.SmtpEventHandler;
import com.untangle.app.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;
import com.untangle.app.smtp.mime.MIMEOutputStream;
import com.untangle.app.smtp.mime.MIMEUtil;
import com.untangle.app.smtp.quarantine.MailSummary;
import com.untangle.app.smtp.quarantine.QuarantineAppView;
import com.untangle.app.smtp.safelist.SafelistAppView;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * Protocol Handler which is called-back as scannable messages are encountered.
 */
public class SpamSmtpHandler extends SmtpEventHandler implements TemplateTranslator
{
    private final Logger logger = Logger.getLogger(SpamSmtpHandler.class);

    private static final String MOD_SUB_TEMPLATE = "[SPAM] $MIMEMessage:SUBJECT$";

    private final WrappedMessageGenerator msgGenerator;

    private final SpamBlockerBaseApp app;
    private final QuarantineAppView quarantine;
    private final SafelistAppView safelist;

    private final long timeout;

    private String receivedBy; // Now we also keep the salutation to help SpamAssassin evaluate.

    /**
     * Constructor
     * 
     * @param app
     *        The base application
     */
    public SpamSmtpHandler(SpamBlockerBaseApp app)
    {
        super();

        this.app = app;

        MailExport mailExport = MailExportFactory.factory().getExport();
        this.quarantine = mailExport.getQuarantineAppView();
        this.safelist = mailExport.getSafelistAppView();
        this.timeout = mailExport.getExportSettings().getSmtpTimeout();

        msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE, getTranslatedBodyTemplate(), this);
    }

    /**
     * Handle new TCP sessions requests
     * 
     * @param sessionRequest
     *        The session request
     */
    @Override
    public final void handleTCPNewSessionRequest(TCPNewSessionRequest sessionRequest)
    {
        SpamSettings spamSettings = app.getSettings();
        SpamSmtpConfig spamConfig = spamSettings.getSmtpConfig();

        if (!spamConfig.getScan()) {
            logger.debug("handleTCPNewSessionRequest() release");
            sessionRequest.release();
            return;
        }

        int activeCount = app.getScanner().getActiveScanCount();
        if (SpamLoadChecker.reject(activeCount, logger, spamConfig.getScanLimit(), spamConfig.getLoadLimit())) {
            logger.warn("Load too high, rejecting connection from: " + sessionRequest.getOrigClientAddr());
            sessionRequest.rejectReturnRst();
        }

        logger.debug("handleTCPNewSessionRequest()");
        super.handleTCPNewSessionRequest(sessionRequest);
    }

    /**
     * Get translated body template
     * 
     * @return The translated body template
     */
    @Override
    public String getTranslatedBodyTemplate()
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String bodyTemplate = i18nUtil.tr("The attached message from") + " $MIMEMessage:FROM$\r\n" + i18nUtil.tr("was determined by the Spam Blocker to be spam based on a score of") + " $SPAMReport:SCORE$\r\n" + i18nUtil.tr("where anything above") + " $SPAMReport:THRESHOLD$ " + i18nUtil.tr("is spam.") + "\r\n";
        return bodyTemplate;
    }

    /**
     * Get translated subject template
     * 
     * @return The translated subject template
     */
    @Override
    public String getTranslatedSubjectTemplate()
    {
        return MOD_SUB_TEMPLATE;
    }

    /**
     * Method for subclasses (i.e. clamphish) to set the
     * {@link com.untangle.app.smtp.quarantine.MailSummary#getQuarantineCategory
     * category} for a Quarantine submission.
     * 
     * @return The quarantine category
     */
    protected String getQuarantineCategory()
    {
        return "SPAM";
    }

    /**
     * 
     * Method for subclasses (i.e. clamphish) to set the
     * {@link com.untangle.app.smtp.quarantine.MailSummary#getQuarantineDetail
     * detail} for a Quarantine submission.
     * 
     * @param report
     *        The spam report
     * @return The quarantine detail
     */
    protected String getQuarantineDetail(SpamReport report)
    {
        // Make a nicely printed string for the UI.
        return String.format("%03.1f", report.getScore());
    }

    /**
     * Method for returning the generator used to mark messages
     * 
     * @return The generator
     */
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return msgGenerator;
    }

    /**
     * Get scanning enabled state
     * 
     * @param session
     *        The session
     * @return The scan enabled state
     */
    @Override
    public boolean getScanningEnabled(AppTCPSession session)
    {
        return app.getSettings().getSmtpConfig().getScan();
    }

    /**
     * Get the maximum server wait time
     * 
     * @param session
     *        The session
     * @return The maximum server wait time
     */
    @Override
    public long getMaxServerWait(AppTCPSession session)
    {
        return this.timeout;
    }

    /**
     * Get the maximum client wait time
     * 
     * @param session
     *        The session
     * @return The maximum client wait time
     */
    @Override
    public long getMaxClientWait(AppTCPSession session)
    {
        return this.timeout;
    }

    /**
     * get the give up size
     * 
     * @param session
     *        The session
     * @return The give up size
     */
    @Override
    public int getGiveUpSz(AppTCPSession session)
    {
        return app.getSettings().getSmtpConfig().getMsgSizeLimit();
    }

    /**
     * Get the scanned message result
     * 
     * @param session
     *        The session
     * @param msg
     *        The message
     * @param tx
     *        The transaction
     * @param msgInfo
     *        The message info
     * @return The result
     */
    @Override
    public ScannedMessageResult blockPassOrModify(AppTCPSession session, MimeMessage msg, SmtpTransaction tx, SmtpMessageEvent msgInfo)
    {
        logger.debug("blockPassOrModify()");

        InterfaceSettings iset = UvmContextFactory.context().networkManager().findInterfaceId(session.getServerIntf());
        boolean isWanBound = false;
        if (iset != null) isWanBound = iset.getIsWan();

        if (safelist.isSafelisted(tx.getFrom(), getFromNoEx(msg), tx.getRecipients(false))) {
            logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.SAFELIST);
            app.incrementPassCount();
            return new ScannedMessageResult(BlockOrPassResult.PASS);
        }

        // If greylist is enable, check greylist
        if (app.getSettings().getSmtpConfig().getGreylist() && !isWanBound) {
            InetAddress client = session.getClientAddr();
            String from = msgInfo.getEnvelopeFromAddress();
            String to = msgInfo.getEnvelopeToAddress();

            GreyListKey key = new GreyListKey(client, from, to);
            logger.debug("greylist: check message " + key);

            Boolean found = SpamBlockerBaseApp.getGreylist().get(key);
            if (found == null) {
                logger.info("greylist: missed. adding new key: " + key);

                SpamBlockerBaseApp.getGreylist().put(key, Boolean.TRUE);

                postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.GREYLIST);
                app.incrementBlockCount();
                return new ScannedMessageResult(BlockOrPassResult.TEMPORARILY_REJECT);
            } else {
                logger.warn("greylist: hit. " + key);
            }
        }

        // Scan the message
        File f = null;
        try {
            f = messageToFile(session, msg, tx);
            if (f == null) {
                logger.error("Error writing to file.  Unable to scan.  Assume pass");
                postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
                app.incrementPassCount();
                return new ScannedMessageResult(BlockOrPassResult.PASS);
            }

            if (f.length() > getGiveUpSz(session)) {
                logger.debug("Message larger than " + getGiveUpSz(session) + ".  Don't bother to scan");
                postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.OVERSIZE);
                app.incrementPassCount();
                return new ScannedMessageResult(BlockOrPassResult.PASS);
            }

            try {
                if (!app.getSettings().getSmtpConfig().getScanWanMail() && isWanBound) {
                    logger.debug("Ignoring WAN-bound SMTP mail");
                    postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.OUTBOUND);
                    app.incrementPassCount();
                    return new ScannedMessageResult(BlockOrPassResult.PASS);
                }
            } catch (Exception e) {
                logger.warn("Unable to lookup destination interface", e);
            }

            SpamReport report = scanFile(f);

            if (report == null) { // Handle error case
                if (app.getSettings().getSmtpConfig().getFailClosed()) {
                    logger.warn("Error scanning message. Failing closed");
                    postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.FAILED_BLOCKED);
                    app.incrementBlockCount();
                    return new ScannedMessageResult(BlockOrPassResult.TEMPORARILY_REJECT);
                } else {
                    logger.warn("Error scanning message. Failing open");
                    postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.FAILED_PASSED);
                    app.incrementPassCount();
                    return new ScannedMessageResult(BlockOrPassResult.PASS);
                }
            }

            if (app.getSettings().getSmtpConfig().getAddSpamHeaders()) {
                report.addHeaders(msg);
            }

            SpamMessageAction action = app.getSettings().getSmtpConfig().getMsgAction();

            if (app.getSettings().getSmtpConfig().getBlockSuperSpam() && app.getSettings().getSmtpConfig().getSuperSpamStrength() / 10.0f <= report.getScore()) {
                action = SpamMessageAction.DROP;
            }

            if (report.isSpam()) {// BEGIN SPAM
                logger.debug("Spam found");

                if (action == SpamMessageAction.PASS) {
                    logger.debug("Although spam detected, pass message as-per policy");
                    markHeaders(msg, report);
                    postSpamEvent(msgInfo, report, SpamMessageAction.PASS);
                    app.incrementPassCount();
                    return new ScannedMessageResult(msg);
                } else if (action == SpamMessageAction.MARK) {
                    logger.debug("Marking message as-per policy");
                    postSpamEvent(msgInfo, report, SpamMessageAction.MARK);
                    markHeaders(msg, report);
                    app.incrementMarkCount();
                    MimeMessage wrappedMsg = this.getMsgGenerator().wrap(msg, tx, report);
                    return new ScannedMessageResult(wrappedMsg);
                } else if (action == SpamMessageAction.QUARANTINE) {
                    logger.debug("Attempt to quarantine mail as-per policy");
                    if (quarantineMail(msg, tx, report, f)) {
                        app.incrementQuarantineCount();
                        postSpamEvent(msgInfo, report, SpamMessageAction.QUARANTINE);
                        return new ScannedMessageResult(BlockOrPassResult.DROP);
                    } else {
                        logger.debug("Quarantine failed.  Fall back to mark");
                        app.incrementMarkCount();
                        postSpamEvent(msgInfo, report, SpamMessageAction.MARK);
                        markHeaders(msg, report);
                        MimeMessage wrappedMsg = this.getMsgGenerator().wrap(msg, tx, report);
                        return new ScannedMessageResult(wrappedMsg);
                    }
                } else {
                    logger.debug("Blocking spam message as-per policy");
                    postSpamEvent(msgInfo, report, SpamMessageAction.DROP);
                    app.incrementBlockCount();
                    return new ScannedMessageResult(BlockOrPassResult.DROP);
                }
            } else {
                markHeaders(msg, report);
                postSpamEvent(msgInfo, report, SpamMessageAction.PASS);
                logger.debug("Not spam");
                app.incrementPassCount();

                return new ScannedMessageResult(msg);
            }
        } finally {
            try {
                if (f != null) f.delete();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Handle the opening response
     * 
     * @param session
     *        The session
     * @param resp
     *        The response
     */
    @Override
    public void handleOpeningResponse(AppTCPSession session, Response resp)
    {
        // Note the receivedBy
        String[] rargs = resp.getArgs();
        if (rargs == null || rargs.length < 1) {
            receivedBy = null;
        } else {
            receivedBy = rargs[0];
        }
        super.handleOpeningResponse(session, resp);
    }

    /**
     * Get the block or pass result
     * 
     * @param session
     *        The session
     * @param msg
     *        The message
     * @param tx
     *        The transaction
     * @param msgInfo
     *        The message info
     * @return The block or pass result
     */
    @Override
    public BlockOrPassResult blockOrPass(AppTCPSession session, MimeMessage msg, SmtpTransaction tx, SmtpMessageEvent msgInfo)
    {

        logger.debug("[handleMessageCanNotBlock]");

        // app.incrementScanCount();

        // Scan the message
        File f = messageToFile(session, msg, tx);
        if (f == null) {
            logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            app.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        if (f.length() > getGiveUpSz(session)) {
            logger.debug("Message larger than " + getGiveUpSz(session) + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.OVERSIZE);
            app.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        if (safelist.isSafelisted(tx.getFrom(), getFromNoEx(msg), tx.getRecipients(false))) {
            logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.SAFELIST);
            app.incrementPassCount();
            return BlockOrPassResult.PASS;
        }

        // Anything going out External MARK instead of QUARANTINE
        boolean isWanBound = false;
        try {
            InterfaceSettings iset = UvmContextFactory.context().networkManager().findInterfaceId(session.getServerIntf());
            if (iset != null) isWanBound = iset.getIsWan();
        } catch (Exception e) {
            logger.warn("Unable to lookup destination interface", e);
        }

        try {
            if (!app.getSettings().getSmtpConfig().getScanWanMail() && isWanBound) {
                logger.debug("Ignoring WAN-bound SMTP mail");
                postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.SAFELIST);
                app.incrementPassCount();
                return BlockOrPassResult.PASS;
            }
        } catch (Exception e) {
            logger.warn("Unable to lookup destination interface", e);
        }

        SpamReport report = scanFile(f);

        if (report == null) { // Handle error case
            if (app.getSettings().getSmtpConfig().getFailClosed()) {
                logger.warn("Error scanning message. Failing closed");
                postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.DROP);
                app.incrementBlockCount();
                return BlockOrPassResult.TEMPORARILY_REJECT;
            } else {
                logger.warn("Error scanning message. Failing open");
                postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
                app.incrementPassCount();
                return BlockOrPassResult.PASS;
            }
        }

        SpamMessageAction action = app.getSettings().getSmtpConfig().getMsgAction();

        if (action == SpamMessageAction.QUARANTINE && isWanBound) {
            // Change action now, as it'll make the event logs
            // more accurate
            logger.debug("Implicitly converting policy from \"QUARANTINE\"" + " to \"MARK\" as we have a message going out WAN");
            action = SpamMessageAction.MARK;
        }

        // Check for the impossible-to-satisfy action of "REMOVE"
        if (action == SpamMessageAction.MARK) {
            // Change action now, as it'll make the event logs
            // more accurate
            logger.debug("Implicitly converting policy from \"MARK\"" + " to \"PASS\" as we have already begun to trickle");
            action = SpamMessageAction.PASS;
        }

        if (app.getSettings().getSmtpConfig().getBlockSuperSpam() && app.getSettings().getSmtpConfig().getSuperSpamStrength() / 10.0f <= report.getScore()) {
            action = SpamMessageAction.DROP;
        }

        if (report.isSpam()) {
            logger.debug("Spam");

            if (action == SpamMessageAction.PASS) {
                logger.debug("Although spam detected, pass message as-per policy");
                postSpamEvent(msgInfo, report, SpamMessageAction.PASS);
                app.incrementPassCount();
                return BlockOrPassResult.PASS;
            } else if (action == SpamMessageAction.MARK) {
                logger.debug("Cannot mark at this time.  Simply pass");
                postSpamEvent(msgInfo, report, SpamMessageAction.PASS);
                app.incrementPassCount();
                return BlockOrPassResult.PASS;
            } else if (action == SpamMessageAction.QUARANTINE) {
                logger.debug("Attempt to quarantine mail as-per policy");
                if (quarantineMail(msg, tx, report, f)) {
                    logger.debug("Mail quarantined");
                    postSpamEvent(msgInfo, report, SpamMessageAction.QUARANTINE);
                    app.incrementQuarantineCount();
                    return BlockOrPassResult.DROP;
                } else {
                    logger.debug("Quarantine failed.  Fall back to pass");
                    postSpamEvent(msgInfo, report, SpamMessageAction.PASS);
                    app.incrementPassCount();
                    return BlockOrPassResult.PASS;
                }
            } else {
                logger.debug("Blocking spam message as-per policy");
                postSpamEvent(msgInfo, report, SpamMessageAction.DROP);
                app.incrementBlockCount();
                return BlockOrPassResult.DROP;
            }
        } else {
            logger.debug("Not Spam");
            postSpamEvent(msgInfo, report, SpamMessageAction.PASS);
            app.incrementPassCount();
            return BlockOrPassResult.PASS;
        }
    }

    /**
     * See if an extension is allowed
     * 
     * @param extension
     *        The extension
     * @param session
     *        The session
     * @return True if allowed, otherwise false
     */
    @Override
    protected boolean isAllowedExtension(String extension, AppTCPSession session)
    {
        // Thread safety
        String str = extension.toUpperCase();
        if ("STARTTLS".equals(str)) {
            // if the SSL inspector is active we always allow STARTTLS
            if (session.globalAttachment(AppSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) return (true);
            return app.getSettings().getSmtpConfig().getAllowTls();
        } else {
            return super.isAllowedExtension(extension, session);
        }
    }

    /**
     * See if a command is allowed
     * 
     * @param command
     *        The command
     * @param session
     *        The session
     * @return True if allowed, otherwise false
     */
    @Override
    protected boolean isAllowedCommand(String command, AppTCPSession session)
    {
        String str = command.toUpperCase();
        if ("STARTTLS".equals(str)) {
            // if the SSL inspector is active we always allow STARTTLS
            if (session.globalAttachment(AppSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) return (true);
            return app.getSettings().getSmtpConfig().getAllowTls();
        } else {
            return super.isAllowedCommand(command, session);
        }
    }

    /**
     * Mark the message headers
     * 
     * @param msg
     *        The message
     * @param report
     *        The spam report
     */
    private void markHeaders(MimeMessage msg, SpamReport report)
    {
        if (app.getSettings().getSmtpConfig().getAddSpamHeaders()) {
            try {
                msg.setHeader(app.getSettings().getSmtpConfig().getHeaderName(), (report.isSpam() ? "YES" : "NO"));
            } catch (MessagingException shouldNotHappen) {
                logger.error(shouldNotHappen);
            }
        }
    }

    /**
     * Get a clean spam report
     * 
     * @return The spam report
     */
    private SpamReport cleanReport()
    {
        return new SpamReport(new LinkedList<ReportItem>(), 0.0f, app.getSettings().getSmtpConfig().getStrength() / 10.0f);
    }

    /**
     * Log a spam event to the database
     * 
     * @param msgInfo
     *        The message info
     * @param report
     *        The spam report
     * @param action
     *        The message action
     */
    private void postSpamEvent(SmtpMessageEvent msgInfo, SpamReport report, SpamMessageAction action)
    {
        String testsString = "";
        boolean first = true;
        for (ReportItem ri : report.getItems()) {
            if (!first) testsString += " ";
            else first = false;
            testsString += ri.getCategory() + "[" + ri.getScore() + "]";
        }

        SpamLogEvent spamEvent = new SpamLogEvent(msgInfo, report.getScore(), report.isSpam(), action, app.getScanner().getVendorName(), testsString);
        app.logEvent(spamEvent);
    }

    /**
     * Write a message to a file
     * 
     * @param session
     *        The session
     * @param msg
     *        The message
     * @param tx
     *        The SMTP transaction
     * @return The message file, or null if there is a problem
     */
    private File messageToFile(AppTCPSession session, MimeMessage msg, SmtpTransaction tx)
    {
        // Build the "fake" received header for SpamAssassin
        InetAddress clientAddr = session.getClientAddr();
        StringBuilder sb = new StringBuilder();
        sb.append("Received: ");
        sb.append("from ").append(getHeloName(session)).append(" (").append(clientAddr.getHostName()).append(" [").append(clientAddr.getHostAddress()).append("])").append(CRLF);
        InternetAddress envFrom = tx.getFrom();
        if (envFrom != null) {
            String smtpEnvFrom = MIMEUtil.toSMTPString(envFrom);
            sb.append("\t(envelope-from ").append(smtpEnvFrom).append(")").append(CRLF);
        }
        sb.append("\tby ").append(receivedBy == null ? "untangle" : receivedBy).append("; ").append(MIMEUtil.getRFC822Date());

        File ret = null;
        try{
            File.createTempFile("SpamSmtpHandler-", null);
            if (ret != null) session.attachTempFile(ret.getAbsolutePath());
        }catch(Exception e){
            logger.warn("Unable to create temp file:", e);
        }
        if(ret != null){
            try (
                FileOutputStream fOut = new FileOutputStream(ret);
                BufferedOutputStream bOut = new BufferedOutputStream(fOut);
                MIMEOutputStream mimeOut = new MIMEOutputStream(bOut);
            ) {
                mimeOut.writeLine(sb.toString());
                msg.writeTo(mimeOut);
                mimeOut.flush();
                bOut.flush();
                fOut.flush();
            } catch (Exception ex) {
                if(ret != null){
                    try {
                        ret.delete();
                    } catch (Exception ignore) {
                    }
                    ret = null;
                }
                logger.error("Exception writing MIME Message to file", ex);
            }
        }
        return ret;
    }

    /**
     * Wrapper method around the real scanner, which swallows exceptions and
     * simply returns null
     * 
     * @param f
     *        The file
     * @return The spam report or null on failure
     */
    private SpamReport scanFile(File f)
    {
        try {
            SpamReport ret = app.getScanner().scanFile(f, app.getSettings().getSmtpConfig().getStrength() / 10.0f);
            return ret;
        } catch (Exception ex) {
            logger.error("Exception scanning message", ex);
            return null;
        }
    }

    /**
     * Get the from address
     * 
     * @param msg
     *        The message
     * @return The from address
     */
    private InternetAddress getFromNoEx(MimeMessage msg)
    {
        try {
            return (InternetAddress) (msg.getFrom()[0]);
        } catch (Exception e) {
            return new InternetAddress();
        }
    }

    /**
     * Get the subject
     * 
     * @param msg
     *        The message
     * @return The subject
     */
    private String getSubjectNoEx(MimeMessage msg)
    {
        try {
            return msg.getSubject();
        } catch (MessagingException e) {
            return "";
        }
    }

    /**
     * Quarantine a message
     * 
     * @param msg
     *        The message
     * @param tx
     *        The SMTP transaction
     * @param report
     *        The spam report
     * @param file
     *        The message file
     * @return The result
     */
    private boolean quarantineMail(MimeMessage msg, SmtpTransaction tx, SpamReport report, File file)
    {
        List<InternetAddress> addrList = tx.getRecipients(true);
        InternetAddress[] addresses = addrList.toArray(new InternetAddress[addrList.size()]);

        InternetAddress from = getFromNoEx(msg);
        String sender;
        if (from == null || from.getAddress() == null) {
            sender = tx.getFrom() == null ? "<>" : tx.getFrom().getAddress();
        } else {
            sender = from.getAddress();
        }
        return quarantine.quarantineMail(file, new MailSummary(sender, getSubjectNoEx(msg), getQuarantineCategory(), getQuarantineDetail(report), MIMEUtil.attachmentCount(msg), file.length()), addresses);
    }
}
