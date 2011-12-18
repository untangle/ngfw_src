/**
 * $Id$
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
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.TCPSession;

public class SpamImapHandler extends BufferingImapTokenStreamHandler
{
    private final Logger logger = Logger.getLogger(SpamImapHandler.class);

    private final SpamNodeImpl spamImpl;
    private final SpamImapConfig config;
    private final TempFileFactory fileFactory;
    private final SafelistNodeView safelist;

    private static final String MOD_SUB_TEMPLATE =
        "[SPAM] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);
    
    public SpamImapHandler(TCPSession session, long maxClientWait, long maxSvrWait, SpamNodeImpl impl, SpamImapConfig config, SafelistNodeView safelist)
    {
        super(maxClientWait, maxSvrWait, config.getMsgSizeLimit());

        this.spamImpl = impl;
        this.safelist = safelist;
        this.config = config;
        this.fileFactory = new TempFileFactory(UvmContextFactory.context(). pipelineFoundry().getPipeline(session.id()));
    }

    @Override
    public HandleMailResult handleMessage( MIMEMessage msg, MessageInfo msgInfo )
    {
        logger.debug("[handleMessage]");

        //I'm incrementing the count, even if the message is too big
        //or cannot be converted to file
        //spamImpl.incrementScanCount(); node can only have 4 metrics at this time - KenH, 8/15/08

        //Scan the message
        File f = messageToFile(msg);
        if(f == null) {
            logger.error("Error writing to file.  Unable to scan.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        if(f.length() > config.getMsgSizeLimit()) {
            logger.debug("Message larger than " + config.getMsgSizeLimit() + ".  Don't bother to scan");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.OVERSIZE);
            spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        if(safelist.isSafelisted(null, msg.getMMHeaders().getFrom(), null)) {
            logger.debug("Message sender safelisted");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.SAFELIST);
            spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        SpamMessageAction action = config.getMsgAction();
        SpamReport report = scanFile(f);
        //Handle error case
        if(report == null) {
            logger.warn("Error scanning message.  Assume pass");
            postSpamEvent(msgInfo, cleanReport(), SpamMessageAction.PASS);
            spamImpl.incrementPassCount();
            return HandleMailResult.forPassMessage();
        }

        boolean addSpamHeaders = config.getAddSpamHeaders();
        if (addSpamHeaders) {
            report.addHeaders(msg);
        }

        postSpamEvent(msgInfo, report, action);

        //Mark headers regardless of other actions
        if (config.getAddSpamHeaders()) {
            try {
                msg.getMMHeaders().removeHeaderFields(new LCString(config.getHeaderName()));
                msg.getMMHeaders().addHeaderField(config.getHeaderName(),(report.isSpam() ? "YES" : "NO"));
            } catch(HeaderParseException shouldNotHappen) {
                logger.error(shouldNotHappen);
            }
        }

        if(report.isSpam()) {//BEGIN SPAM
            logger.debug("Spam found");

            if(action == SpamMessageAction.PASS) {
                logger.debug("Although SPAM detected, pass message as-per policy");
                spamImpl.incrementPassCount();
                return HandleMailResult.forReplaceMessage(msg);
            }
            else {
                logger.debug("Marking message as-per policy");
                spamImpl.incrementMarkCount();
                MIMEMessage wrappedMsg = this.getMsgGenerator().wrap(msg, report);
                return HandleMailResult.forReplaceMessage(wrappedMsg);
            }
        }//ENDOF SPAM
        else {//BEGIN HAM
            logger.debug("Not spam");
            spamImpl.incrementPassCount();
            report.addHeaders(msg);
            return HandleMailResult.forReplaceMessage(msg);
        }//ENDOF HAM
    }

    private SpamReport cleanReport() {
        return new SpamReport(new LinkedList<ReportItem>(), 0.0f, config.getStrength()/10.0f);
    }

    /**
     * ...name says it all...
     */
    private void postSpamEvent(MessageInfo msgInfo, SpamReport report, SpamMessageAction action)
    {
        //Create an event for the reports
        SpamLogEvent spamEvent = new SpamLogEvent(msgInfo,
                                                  report.getScore(),
                                                  report.isSpam(),
                                                  report.isSpam() ? action : SpamMessageAction.PASS,
                                                  spamImpl.getScanner().getVendorName());
        spamImpl.logEvent(spamEvent);
    }

    /**
     * Wrapper that handles exceptions, and returns
     * null if there is a problem
     */
    private File messageToFile(MIMEMessage msg)
    {
        //Get the part as a file
        try {
            return msg.toFile(fileFactory);
        }
        catch(Exception ex) {
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
        //Attempt scan
        try {
            SpamReport ret = spamImpl.getScanner()
                .scanFile(f, config.getStrength() / 10.0f);
            return ret;
        }
        catch(Exception ex) {
            logger.error("Exception scanning message", ex);
            return null;
        }
    }

    /**
     * Method for returning the generator used to mark messages
     */
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return SpamImapHandler.msgGenerator;
    }

}
