/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.net.InetAddress;

import javax.activation.DataHandler;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.MailExport;
import com.untangle.app.smtp.MailExportFactory;
import com.untangle.app.smtp.SmtpMessageEvent;
import com.untangle.app.smtp.SmtpTransaction;
import com.untangle.app.smtp.TemplateTranslator;
import com.untangle.app.smtp.WrappedMessageGenerator;
import com.untangle.app.smtp.handler.ScannedMessageResult;
import com.untangle.app.smtp.handler.SmtpEventHandler;
import com.untangle.app.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;
import com.untangle.app.smtp.mime.MIMEOutputStream;
import com.untangle.app.smtp.mime.MIMEUtil;
import com.untangle.app.smtp.mime.HeaderNames;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.util.GlobUtil;

/**
 * Protocol Handler which is called-back as messages are found which are
 * candidates for Virus Scanning.
 */
public class VirusSmtpHandler extends SmtpEventHandler implements TemplateTranslator
{
    private static final String MOD_SUB_TEMPLATE = "[VIRUS] $MIMEMessage:SUBJECT$";

    private final Logger logger = Logger.getLogger(VirusSmtpHandler.class);

    private final VirusBlockerBaseApp node;

    private final long timeout;

    private final WrappedMessageGenerator generator;

    protected class VirusSmtpState extends VirusBlockerState
    {
        private VirusFileManager fileManager = null;
        private boolean memoryMode = false;
    }

    public VirusSmtpHandler(VirusBlockerBaseApp node)
    {
        super();

        this.node = node;

        MailExport mailExport = MailExportFactory.factory().getExport();
        this.timeout = mailExport.getExportSettings().getSmtpTimeout();

        this.generator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE, getTranslatedBodyTemplate(), this);
    }

    @Override
    public boolean getScanningEnabled(AppTCPSession session)
    {
        return node.getSettings().getScanSmtp();
    }

    @Override
    public long getMaxServerWait(AppTCPSession session)
    {
        return timeout;
    }

    @Override
    public long getMaxClientWait(AppTCPSession session)
    {
        return timeout;
    }

    @Override
    public int getGiveUpSz(AppTCPSession session)
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getTranslatedBodyTemplate()
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String bodyTemplate = i18nUtil.tr("The attached message from") + " $MIMEMessage:FROM$\r\n" + i18nUtil.tr("was found to contain the virus") + " \"$VirusReport:VIRUS_NAME$\".\r\n" + i18nUtil.tr("The infected portion of the message was removed by Virus Blocker.") + "\r\n";
        return bodyTemplate;
    }

    @Override
    public String getTranslatedSubjectTemplate()
    {
        return MOD_SUB_TEMPLATE;
    }

    @Override
    public ScannedMessageResult blockPassOrModify(AppTCPSession session, MimeMessage msg, SmtpTransaction tx, SmtpMessageEvent msgInfo)
    {
        logger.debug("Message[" + msgInfo.getMessageId() + "] blockPassOrModify()");
        this.node.incrementScanCount();

        List<Part> candidateParts = MIMEUtil.getParts(msg);
        if (logger.isDebugEnabled()) {
            logger.debug("Message[" + msgInfo.getMessageId() + "] has " + candidateParts.size() + " scannable parts");
        }

        boolean foundVirus = false;
        // Kind-of a hack. I need the scanResult
        // for the wrapped message. If nore than one was found,
        // we'll just use the first
        VirusScannerResult scanResultForWrap = null;

        String actionTaken = "pass";
        String configuredAction = node.getSettings().getSmtpAction();
        String virusName = null;

        for (Part part : candidateParts) {
            String disposition = null;
            String contentType = null;
            boolean shouldScan = true;
            try {
                disposition = part.getDisposition();
                contentType = part.getContentType();
                shouldScan = shouldScan(disposition, contentType);
            } catch (Exception e) {
                logger.warn("Exception", e);
            }

            if (!shouldScan) {
                logger.debug("Message[" + msgInfo.getMessageId() + "] Skip part: Disposition: " + disposition + " ContentType: " + contentType);
                continue;
            }

            VirusScannerResult scanResult;
            if (ignoredHost(session.sessionEvent().getSServerAddr()) || ignoredHost(session.sessionEvent().getCClientAddr())) {
                scanResult = VirusScannerResult.CLEAN;
                logger.warn("Message[" + msgInfo.getMessageId() + "] Ignore SMTP: " + session.sessionEvent().getCClientAddr().getHostAddress() + " -> " + session.sessionEvent().getSServerAddr().getHostAddress());
            } else {
                scanResult = scanPart(session, part);
            }

            if (scanResult == null) {
                logger.warn("Message[" + msgInfo.getMessageId() + "] Scanning returned null (error already reported).  Skip " + "part assuming local error");
                continue;
            }
            if (scanResult.isClean()) actionTaken = "pass";
            else actionTaken = configuredAction;

            if (scanResult.isClean()) {
                logger.debug("Message[" + msgInfo.getMessageId() + "] Clean part: Disposition: " + disposition + " ContentType: " + contentType);
            } else {
                logger.debug("Message[" + msgInfo.getMessageId() + "] VIRUS: Disposition: " + disposition + " ContentType: " + contentType);

                if (!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;
                virusName = scanResult.getVirusName();

                if ("pass".equals(actionTaken)) {
                    logger.debug("Message[" + msgInfo.getMessageId() + "] Passing infected part as-per policy");
                } else if ("block".equals(actionTaken)) {
                    logger.debug("Message[" + msgInfo.getMessageId() + "] Stop scanning remaining parts, as the policy is to block");
                    break;
                } else { /* remove */
                    if (part == msg) {
                        logger.debug("Message[" + msgInfo.getMessageId() + "] Top-level message itself was infected.  \"Remove\"" + "virus by converting part to text");
                    } else {
                        logger.debug("Message[" + msgInfo.getMessageId() + "] Removing infected part");
                    }

                    try {
                        MIMEUtil.removeChild(part);
                        msg.saveChanges();
                    } catch (Exception ex) {
                        logger.error("Exception repoving child part", ex);
                    }
                }
            }
        }

        VirusSmtpEvent event = new VirusSmtpEvent(msgInfo, !foundVirus, virusName, actionTaken, this.node.getName());
        this.node.logEvent(event);

        if (foundVirus) {
            if ("block".equals(configuredAction)) {
                logger.debug("Message[" + msgInfo.getMessageId() + "] Returning BLOCK as-per policy");
                this.node.incrementBlockCount();
                return new ScannedMessageResult(BlockOrPassResult.DROP);
            } else if ("remove".equals(configuredAction)) {
                logger.debug("Message[" + msgInfo.getMessageId() + "] REMOVE (wrap) message");
                MimeMessage wrappedMsg = this.generator.wrap(msg, tx, scanResultForWrap);
                this.node.incrementRemoveCount();
                return new ScannedMessageResult(wrappedMsg);
            } else {
                logger.debug("Message[" + msgInfo.getMessageId() + "] Passing infected message (as-per policy)");
                this.node.incrementPassedInfectedMessageCount();
            }
        }
        this.node.incrementPassCount();
        return new ScannedMessageResult(BlockOrPassResult.PASS);
    }

    @Override
    public BlockOrPassResult blockOrPass(AppTCPSession session, MimeMessage msg, SmtpTransaction tx, SmtpMessageEvent msgInfo)
    {
        logger.debug("Message[" + msgInfo.getMessageId() + "] blockOrPass()");
        this.node.incrementScanCount();

        List<Part> candidateParts = MIMEUtil.getParts(msg);
        if (logger.isDebugEnabled()) {
            logger.debug("Message[" + msgInfo.getMessageId() + "] has " + candidateParts.size() + " scannable parts");
        }
        String action = this.node.getSettings().getSmtpAction();

        // Check for the impossible-to-satisfy action of "REMOVE"
        if ("remove".equals(action)) {
            // Change action now, as it'll make the event logs
            // more accurate
            logger.debug("Message[" + msgInfo.getMessageId() + "] Implicitly converting policy from \"REMOVE\"" + " to \"BLOCK\" as we have already begun to trickle");
            action = "block";
        }

        boolean foundVirus = false;
        VirusScannerResult scanResultForWrap = null;
        String virusName = null;

        for (Part part : candidateParts) {
            String disposition = null;
            String contentType = null;
            boolean shouldScan = true;
            try {
                disposition = part.getDisposition();
                contentType = part.getContentType();
                shouldScan = shouldScan(disposition, contentType);
            } catch (Exception e) {
                logger.warn("Exception", e);
            }

            if (shouldScan) {
                try {
                    logger.debug("Message[" + msgInfo.getMessageId() + "] Skip part: Disposition: " + disposition + " ContentType: " + contentType);
                } catch (Exception e) {
                    logger.warn("Exception", e);
                }
                continue;
            }

            VirusScannerResult scanResult;
            if (ignoredHost(session.sessionEvent().getSServerAddr()) || ignoredHost(session.sessionEvent().getCClientAddr())) {
                scanResult = VirusScannerResult.CLEAN;
                logger.warn("Message[" + msgInfo.getMessageId() + "] Ignore SMTP: " + session.sessionEvent().getCClientAddr().getHostAddress() + " -> " + session.sessionEvent().getSServerAddr().getHostAddress());
            } else {
                scanResult = scanPart(session, part);
            }

            if (scanResult == null) {
                logger.warn("Message[" + msgInfo.getMessageId() + "] Scanning returned null (error already reported).  Skip " + "part assuming local error");
                continue;
            }

            if (scanResult.isClean()) {
                logger.debug("Message[" + msgInfo.getMessageId() + "] Clean part: Disposition: " + disposition + " ContentType: " + contentType);
            } else {
                logger.debug("Message[" + msgInfo.getMessageId() + "] VIRUS: Disposition: " + disposition + " ContentType: " + contentType);

                if (!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;
                virusName = scanResult.getVirusName();

                // Make log report
                VirusSmtpEvent event = new VirusSmtpEvent(msgInfo, scanResult.isClean(), scanResult.getVirusName(), scanResult.isClean() ? "pass" : action, this.node.getName());
                this.node.logEvent(event);

                if ("pass".equals(action)) {
                    logger.debug("Message[" + msgInfo.getMessageId() + "] Passing infected part as-per policy");
                } else {
                    logger.debug("Message[" + msgInfo.getMessageId() + "] Stop scanning any remaining parts as we will block message");
                    break;
                }
            }
        }

        VirusSmtpEvent event = new VirusSmtpEvent(msgInfo, !foundVirus, virusName, (foundVirus ? action : "pass"), this.node.getName());
        this.node.logEvent(event);

        if (foundVirus) {
            if ("block".equals(action)) {
                logger.debug("Message[" + msgInfo.getMessageId() + "] Blocking mail as-per policy");
                this.node.incrementBlockCount();
                return BlockOrPassResult.DROP;
            }
        }
        this.node.incrementPassCount();
        return BlockOrPassResult.PASS;
    }

    @Override
    protected boolean isAllowedExtension(String extension, AppTCPSession session)
    {
        // Thread safety
        String str = extension.toUpperCase();
        if ("STARTTLS".equals(str)) {
            // if the SSL inspector is active we always allow STARTTLS
            if (session.globalAttachment(AppSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) return (true);
            return node.getSettings().getSmtpAllowTls();
        } else {
            return super.isAllowedExtension(extension, session);
        }
    }

    @Override
    protected boolean isAllowedCommand(String command, AppTCPSession session)
    {
        String str = command.toUpperCase();
        if ("STARTTLS".equals(str)) {
            // if the SSL inspector is active we always allow STARTTLS
            if (session.globalAttachment(AppSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) return (true);
            return node.getSettings().getSmtpAllowTls();
        } else {
            return super.isAllowedCommand(command, session);
        }
    }

    /**
     * Returns null if there was an error.
     */
    private VirusScannerResult scanPart(AppTCPSession session, Part part)
    {
        // Get the part as a file
        VirusSmtpState state = null;
        try {
            state = partToFile(session, part);
            session.attach(state);
        } catch (Exception ex) {
            logger.error("Exception writing MIME part to file", ex);
            return null;
        }
        // Call VirusScanner
        try {
            logger.debug("Scanning the SMTP file: " + state.fileManager.getFileDisplayName());
            VirusScannerResult result = this.node.getScanner().scanFile(state.fileManager.getFileObject(), session);
            if (result == null || result == VirusScannerResult.ERROR) {
                logger.warn("Received an error scan report.  Assume local error" + " and report file clean");
                return null;
            }
            state.fileManager.delete();
            return result;
        } catch (Exception ex) {
            try {
                logger.error("Exception scanning MIME part in file \"" + state.fileManager.getTempFileAbsolutePath() + "\"", ex);
                state.fileManager.delete();
            } catch (Exception e) {
            }
            return null;
        }
    }

    private VirusSmtpState partToFile(AppTCPSession session, Part part)
    {
        VirusSmtpState state = new VirusSmtpState();
        state.memoryMode = node.getSettings().getForceMemoryMode();

        try {
            state.fileManager = new VirusFileManager(state.memoryMode, "VirusMimePart-");

            if (state.memoryMode == false) {
                session.attachTempFile(state.fileManager.getTempFileAbsolutePath());
            }

            MIMEOutputStream mimeOut = new MIMEOutputStream(state.fileManager);
            DataHandler dh = part.getDataHandler();
            dh.writeTo(mimeOut);
            mimeOut.flush();
            state.fileManager.flush();
            state.fileHash = state.fileManager.getFileHash();
            state.fileManager.close();
            return state;
        } catch (Exception ex) {
            try {
                state.fileManager.close();
            } catch (Exception ignore) {
            }
            try {
                state.fileManager.delete();
            } catch (Exception ignore) {
            }
            logger.error("Exception writing MIME Message to file", ex);
            return null;
        }
    }

    private boolean ignoredHost(InetAddress host)
    {
        if (host == null) {
            return false;
        }

        Pattern p;

        for (Iterator<GenericRule> i = node.getSettings().getPassSites().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled()) {
                p = (Pattern) sr.attachment();
                if (null == p) {
                    try {
                        p = Pattern.compile(GlobUtil.globToRegex(sr.getString()));
                    } catch (Exception error) {
                        logger.error("Unable to compile passSite=" + sr.getString());
                    }
                    sr.attach(p);
                }
                if (p.matcher(host.getHostName()).matches()) {
                    return true;
                }
                if (p.matcher(host.getHostAddress()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldScan(String disposition, String contentType)
    {
        if (disposition != null) {
            if (disposition.equalsIgnoreCase(HeaderNames.ATTACHMENT_DISPOSITION_STR)) return true;
        }
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            if (contentType.startsWith("text")) return false;
            if (contentType.startsWith("image")) return false;
            if (contentType.startsWith("message")) return false;
        }
        return false;
    }

    protected void clearEventHandlerCache()
    {
    }
}
