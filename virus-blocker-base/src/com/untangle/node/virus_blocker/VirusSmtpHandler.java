/**
 * $Id$
 */
package com.untangle.node.virus_blocker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;

import javax.activation.DataHandler;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.MailExport;
import com.untangle.node.smtp.MailExportFactory;
import com.untangle.node.smtp.SmtpMessageEvent;
import com.untangle.node.smtp.SmtpTransaction;
import com.untangle.node.smtp.TemplateTranslator;
import com.untangle.node.smtp.WrappedMessageGenerator;
import com.untangle.node.smtp.handler.ScannedMessageResult;
import com.untangle.node.smtp.handler.SmtpEventHandler;
import com.untangle.node.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;
import com.untangle.node.smtp.mime.MIMEOutputStream;
import com.untangle.node.smtp.mime.MIMEUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.node.GenericRule;
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

    protected class VirusSmtpStatus
    {
        private File diskFile = null;
        private String fileHash = null;
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
    public boolean getScanningEnabled(NodeTCPSession session)
    {
        return node.getSettings().getScanSmtp();
    }

    @Override
    public long getMaxServerWait(NodeTCPSession session)
    {
        return timeout;
    }

    @Override
    public long getMaxClientWait(NodeTCPSession session)
    {
        return timeout;
    }

    @Override
    public int getGiveUpSz(NodeTCPSession session)
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
    public ScannedMessageResult blockPassOrModify(NodeTCPSession session, MimeMessage msg, SmtpTransaction tx, SmtpMessageEvent msgInfo)
    {
        this.logger.debug("[handleMessageCanBlock] called");
        this.node.incrementScanCount();

        List<Part> candidateParts = MIMEUtil.getCandidateParts(msg);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Message has: " + candidateParts.size() + " scannable parts");
        }

        boolean foundVirus = false;
        // Kind-of a hack. I need the scanResult
        // for the wrapped message. If nore than one was found,
        // we'll just use the first
        VirusScannerResult scanResultForWrap = null;

        String actionTaken = "pass";
        String configuredAction = node.getSettings().getSmtpAction();

        for (Part part : candidateParts) {
            if (!MIMEUtil.shouldScan(part)) {
                this.logger.debug("Skipping part which does not need to be scanned");
                continue;
            }

            VirusScannerResult scanResult;
            if (ignoredHost(session.sessionEvent().getSServerAddr()) || ignoredHost(session.sessionEvent().getCClientAddr())) {
                scanResult = VirusScannerResult.CLEAN;
                logger.warn("Passed in SMTP");
            } else {
                scanResult = scanPart(session, part);
            }

            if (scanResult == null) {
                this.logger.warn("Scanning returned null (error already reported).  Skip " + "part assuming local error");
                continue;
            }
            if (scanResult.isClean())
                actionTaken = "pass";
            else
                actionTaken = configuredAction;

            // Make log report
            VirusSmtpEvent event = new VirusSmtpEvent(msgInfo, scanResult.isClean(), scanResult.getVirusName(), actionTaken, this.node.getName());
            this.node.logEvent(event);

            if (scanResult.isClean()) {
                this.logger.debug("Part clean");
            } else {
                if (!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                this.logger.debug("Part contained virus");
                if ("pass".equals(actionTaken)) {
                    this.logger.debug("Passing infected part as-per policy");
                } else if ("block".equals(actionTaken)) {
                    this.logger.debug("Stop scanning remaining parts, as the policy is to block");
                    break;
                } else { /* remove */
                    if (part == msg) {
                        this.logger.debug("Top-level message itself was infected.  \"Remove\"" + "virus by converting part to text");
                    } else {
                        this.logger.debug("Removing infected part");
                    }

                    try {
                        MIMEUtil.removeChild(part);
                        msg.saveChanges();
                    } catch (Exception ex) {
                        this.logger.error("Exception repoving child part", ex);
                    }
                }
            }
        }

        if (foundVirus) {
            if ("block".equals(configuredAction)) {
                this.logger.debug("Returning BLOCK as-per policy");
                this.node.incrementBlockCount();
                return new ScannedMessageResult(BlockOrPassResult.DROP);
            } else if ("remove".equals(configuredAction)) {
                this.logger.debug("REMOVE (wrap) message");
                MimeMessage wrappedMsg = this.generator.wrap(msg, tx, scanResultForWrap);
                this.node.incrementRemoveCount();
                return new ScannedMessageResult(wrappedMsg);
            } else {
                this.logger.debug("Passing infected message (as-per policy)");
                this.node.incrementPassedInfectedMessageCount();
            }
        }
        this.node.incrementPassCount();
        return new ScannedMessageResult(BlockOrPassResult.PASS);
    }

    @Override
    public BlockOrPassResult blockOrPass(NodeTCPSession session, MimeMessage msg, SmtpTransaction tx, SmtpMessageEvent msgInfo)
    {
        this.logger.debug("[handleMessageCanNotBlock]");
        this.node.incrementScanCount();

        List<Part> candidateParts = MIMEUtil.getCandidateParts(msg);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Message has: " + candidateParts.size() + " scannable parts");
        }
        String action = this.node.getSettings().getSmtpAction();

        // Check for the impossible-to-satisfy action of "REMOVE"
        if ("remove".equals(action)) {
            // Change action now, as it'll make the event logs
            // more accurate
            this.logger.debug("Implicitly converting policy from \"REMOVE\"" + " to \"BLOCK\" as we have already begun to trickle");
            action = "block";
        }

        boolean foundVirus = false;
        VirusScannerResult scanResultForWrap = null;

        for (Part part : candidateParts) {
            if (!MIMEUtil.shouldScan(part)) {
                this.logger.debug("Skipping part which does not need to be scanned");
                continue;
            }

            VirusScannerResult scanResult;
            if (ignoredHost(session.sessionEvent().getSServerAddr()) || ignoredHost(session.sessionEvent().getCClientAddr())) {
                scanResult = VirusScannerResult.CLEAN;
                logger.warn("Passed in SMTP");
            } else {
                scanResult = scanPart(session, part);
            }

            if (scanResult == null) {
                this.logger.warn("Scanning returned null (error already reported).  Skip " + "part assuming local error");
                continue;
            }

            if (scanResult.isClean()) {
                this.logger.debug("Part clean");
            } else {
                this.logger.debug("Part contained virus");
                if (!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                // Make log report
                VirusSmtpEvent event = new VirusSmtpEvent(msgInfo, scanResult.isClean(), scanResult.getVirusName(), scanResult.isClean() ? "pass" : action, this.node.getName());
                this.node.logEvent(event);

                if ("pass".equals(action)) {
                    this.logger.debug("Passing infected part as-per policy");
                } else {
                    this.logger.debug("Scop scanning any remaining parts as we will block message");
                    break;
                }
            }
        }
        if (foundVirus) {
            if ("block".equals(action)) {
                this.logger.debug("Blocking mail as-per policy");
                this.node.incrementBlockCount();
                return BlockOrPassResult.DROP;
            }
        }
        this.node.incrementPassCount();
        return BlockOrPassResult.PASS;
    }

    @Override
    protected boolean isAllowedExtension(String extension, NodeTCPSession session)
    {
        // Thread safety
        String str = extension.toUpperCase();
        if ("STARTTLS".equals(str)) {
            // if the SSL inspector is active we always allow STARTTLS
            if (session.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) return (true);
            return node.getSettings().getSmtpAllowTls();
        } else {
            return super.isAllowedExtension(extension, session);
        }
    }

    @Override
    protected boolean isAllowedCommand(String command, NodeTCPSession session)
    {
        String str = command.toUpperCase();
        if ("STARTTLS".equals(str)) {
            // if the SSL inspector is active we always allow STARTTLS
            if (session.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) return (true);
            return node.getSettings().getSmtpAllowTls();
        } else {
            return super.isAllowedCommand(command, session);
        }
    }

    /**
     * Returns null if there was an error.
     */
    private VirusScannerResult scanPart(NodeTCPSession session, Part part)
    {
        // Get the part as a file
        VirusSmtpStatus status = null;
        try {
            status = partToFile(session, part);
        } catch (Exception ex) {
            this.logger.error("Exception writing MIME part to file", ex);
            return null;
        }
        // Call VirusScanner
        try {
            VirusScannerResult result = this.node.getScanner().scanFile(status.diskFile, status.fileHash);
            if (result == null || result == VirusScannerResult.ERROR) {
                this.logger.warn("Received an error scan report.  Assume local error" + " and report file clean");
                return null;
            }
            status.diskFile.delete();
            return result;
        } catch (Exception ex) {
            try {
                this.logger.error("Exception scanning MIME part in file \"" + status.diskFile.getAbsolutePath() + "\"", ex);
                status.diskFile.delete();
            } catch (Exception e) {
            }
            return null;
        }
    }

    private VirusSmtpStatus partToFile(NodeTCPSession session, Part part)
    {
        VirusSmtpStatus status = new VirusSmtpStatus();
        FileOutputStream fOut = null;
        try {
            status.diskFile = File.createTempFile("MimePart-", null);
            if (status.diskFile != null) session.attachTempFile(status.diskFile.getAbsolutePath());
            fOut = new FileOutputStream(status.diskFile);
            MessageDigest msgDigest = MessageDigest.getInstance("MD5");
            DigestOutputStream dOut = new DigestOutputStream(fOut, msgDigest);
            MIMEOutputStream mimeOut = new MIMEOutputStream(dOut);
            DataHandler dh = part.getDataHandler();
            dh.writeTo(mimeOut);
            mimeOut.flush();
            dOut.flush();
            fOut.flush();
            
            BigInteger val = new BigInteger(1, msgDigest.digest());
            status.fileHash = String.format("%1$032x", val);
            
            fOut.close();

            return status;
        } catch (Exception ex) {
            try {
                fOut.close();
            } catch (Exception ignore) {
            }
            try {
                status.diskFile.delete();
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

}
