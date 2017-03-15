/**
 * $Id$
 */
package com.untangle.node.phish_blocker;

import java.util.Map;

import com.untangle.node.smtp.WrappedMessageGenerator;
import com.untangle.node.smtp.quarantine.QuarantineNodeView;
import com.untangle.node.smtp.safelist.SafelistNodeView;
import com.untangle.node.spam_blocker.SpamReport;
import com.untangle.node.spam_blocker.SpamSmtpConfig;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishBlockerSmtpHandler extends com.untangle.node.spam_blocker.SpamSmtpHandler
{
    private static final String MOD_SUB_TEMPLATE = "[PHISH] $MIMEMessage:SUBJECT$";
   
    private WrappedMessageGenerator msgGenerator;
    
    protected PhishBlockerSmtpHandler( PhishBlockerApp node )
    {
        super( node );

        msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,getTranslatedBodyTemplate(), this);
    }
    
    @Override
    public String getTranslatedBodyTemplate()
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String bodyTemplate = i18nUtil.tr("The attached message from")
                              + " $MIMEMessage:FROM$\r\n"
                              + i18nUtil.tr("was determined by the Phish Blocker to be phish (a fraudulent email intended to steal information).") + "  "
                              + "\n\r" + i18nUtil.tr("The kind of phish that was found was") + " $SPAMReport:FULL$";
        return bodyTemplate;
    }
    
    @Override
    public String getTranslatedSubjectTemplate()
    {
        return MOD_SUB_TEMPLATE;
    }

    @Override
    protected String getQuarantineCategory()
    {
        return "PHISH";
    }

    @Override
    protected String getQuarantineDetail(SpamReport report)
    {
        return "PHISH";
    }

    @Override
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return msgGenerator;
    }

}
