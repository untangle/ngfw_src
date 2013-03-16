/**
 * $Id$
 */
package com.untangle.node.virus;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.smtp.SMTPNotifyAction;
import com.untangle.node.mail.papi.smtp.SmtpTransaction;
import com.untangle.node.mail.papi.smtp.sapi.BufferingSessionHandler;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEPart;
import com.untangle.node.mime.MIMEUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.NodeTCPSession;


/**
 * Protocol Handler which is called-back as messages
 * are found which are candidates for Virus Scanning.
 */
public class SmtpSessionHandler extends BufferingSessionHandler
{
    private static final String MOD_SUB_TEMPLATE =
        "[VIRUS] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Virus Blocker.\r\n";

    private final Logger logger = Logger.getLogger(SmtpSessionHandler.class);
    private final Pipeline pipeline;

    private final VirusNodeImpl virusImpl;

    private final WrappedMessageGenerator generator;
    
    public SmtpSessionHandler(NodeTCPSession session, long maxClientWait, long maxSvrWait, VirusNodeImpl impl)
    {
        super(Integer.MAX_VALUE, maxClientWait, maxSvrWait, true);

        this.virusImpl = impl;
        this.pipeline = UvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
        this.generator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE, MOD_BODY_TEMPLATE);
    }

    @Override
    public BPMEvaluationResult blockPassOrModify(MIMEMessage msg, SmtpTransaction tx, MessageInfo msgInfo)
    {
        this.logger.debug("[handleMessageCanBlock] called");
        this.virusImpl.incrementScanCount();

        MIMEPart[] candidateParts = MIMEUtil.getCandidateParts(msg);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Message has: " + candidateParts.length
                           + " scannable parts");
        }

        boolean foundVirus = false;
        //Kind-of a hack.  I need the scanResult
        //for the wrapped message.  If nore than one was found,
        //we'll just use the first
        VirusScannerResult scanResultForWrap = null;

        String actionTaken = "pass";

        for(MIMEPart part : candidateParts) {
            if(!MIMEUtil.shouldScan(part)) {
                this.logger.debug("Skipping part which does not need to be scanned");
                continue;
            }
            VirusScannerResult scanResult = scanPart(part);

            if(scanResult == null) {
                this.logger.warn("Scanning returned null (error already reported).  Skip " + "part assuming local error");
                continue;
            }
            if (scanResult.isClean())
                actionTaken = "pass";
            else
                actionTaken = virusImpl.getSettings().getSmtpAction();
            
            //Make log report
            VirusSmtpEvent event = new VirusSmtpEvent(msgInfo, scanResult, actionTaken, this.virusImpl.getScanner().getVendorName());
            this.virusImpl.logEvent(event);

            if(scanResult.isClean()) {
                this.logger.debug("Part clean");
            }
            else {
                if(!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                this.logger.debug("Part contained virus");
                if("pass".equals(actionTaken)) {
                    this.logger.debug("Passing infected part as-per policy");
                }
                else if("block".equals(actionTaken)) {
                    this.logger.debug("Stop scanning remaining parts, as the policy is to block");
                    break;
                }
                else { /* remove */
                    if(part == msg) {
                        this.logger.debug("Top-level message itself was infected.  \"Remove\"" + "virus by converting part to text");
                    }
                    else {
                        this.logger.debug("Removing infected part");
                    }

                    try {
                        MIMEUtil.removeChild(part);
                    }
                    catch(Exception ex) {
                        this.logger.error("Exception repoving child part", ex);
                    }
                }
            }
        }

        if(foundVirus) {
            if("block".equals(actionTaken)) {
                this.logger.debug("Returning BLOCK as-per policy");
                this.virusImpl.incrementBlockCount();
                return BLOCK_MESSAGE;
            }
            else if("remove".equals(actionTaken)) {
                this.logger.debug("REMOVE (wrap) message");
                MIMEMessage wrappedMsg = this.generator.wrap(msg, tx, scanResultForWrap);
                this.virusImpl.incrementRemoveCount();
                return new BPMEvaluationResult(wrappedMsg);
            }
            else {
                this.logger.debug("Passing infected message (as-per policy)");
                this.virusImpl.incrementPassedInfectedMessageCount();
            }
        }
        this.virusImpl.incrementPassCount();
        return PASS_MESSAGE;
    }


    @Override
    public BlockOrPassResult blockOrPass(MIMEMessage msg, SmtpTransaction tx, MessageInfo msgInfo)
    {
        this.logger.debug("[handleMessageCanNotBlock]");
        this.virusImpl.incrementScanCount();

        MIMEPart[] candidateParts = MIMEUtil.getCandidateParts(msg);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Message has: " + candidateParts.length
                           + " scannable parts");
        }
        String action = this.virusImpl.getSettings().getSmtpAction();

        //Check for the impossible-to-satisfy action of "REMOVE"
        if("remove".equals(action)) {
            //Change action now, as it'll make the event logs
            //more accurate
            this.logger.debug("Implicitly converting policy from \"REMOVE\"" +
                           " to \"BLOCK\" as we have already begun to trickle");
            action = "block";
        }

        boolean foundVirus = false;
        VirusScannerResult scanResultForWrap = null;

        for(MIMEPart part : candidateParts) {
            if(!MIMEUtil.shouldScan(part)) {
                this.logger.debug("Skipping part which does not need to be scanned");
                continue;
            }
            VirusScannerResult scanResult = scanPart(part);

            if(scanResult == null) {
                this.logger.warn("Scanning returned null (error already reported).  Skip " +
                              "part assuming local error");
                continue;
            }

            if(scanResult.isClean()) {
                this.logger.debug("Part clean");
            }
            else {
                this.logger.debug("Part contained virus");
                if(!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                //Make log report
                VirusSmtpEvent event = new VirusSmtpEvent(
                                                          msgInfo,
                                                          scanResult,
                                                          scanResult.isClean()?"pass":action,
                                                          this.virusImpl.getScanner().getVendorName());
                this.virusImpl.logEvent(event);

                if("pass".equals(action)) {
                    this.logger.debug("Passing infected part as-per policy");
                }
                else {
                    this.logger.debug("Scop scanning any remaining parts as we will block message");
                    break;
                }
            }
        }
        if(foundVirus) {
            if("block".equals(action)) {
                this.logger.debug("Blocking mail as-per policy");
                this.virusImpl.incrementBlockCount();
                return BlockOrPassResult.BLOCK;
            }
        }
        this.virusImpl.incrementPassCount();
        return BlockOrPassResult.PASS;
    }

    /**
     * Returns null if there was an error.
     */
    private VirusScannerResult scanPart(MIMEPart part)
    {
        //Get the part as a file
        File f = null;
        try {
            f = part.getContentAsFile( true );
        }
        catch(Exception ex) {
            this.logger.error("Exception writing MIME part to file", ex);
            return null;
        }

        //Call VirusScanner
        try {

            VirusScannerResult result = this.virusImpl.getScanner().scanFile(f);
            if(result == null || result == VirusScannerResult.ERROR) {
                this.logger.warn("Received an error scan report.  Assume local error" +
                              " and report file clean");
                //TODO bscott This is scary
                return null;
            }
            return result;
        }
        catch(Exception ex) {
            //TODO bscott I'd like to preserve this file and include it
            //     in some type of "report".
            this.logger.error("Exception scanning MIME part in file \"" +
                           f.getAbsolutePath() + "\"", ex);
            //No need to delete the file.  This will be handled by the MIMEPart itself
            //through its normal lifecycle
            return null;
        }
    }
}
