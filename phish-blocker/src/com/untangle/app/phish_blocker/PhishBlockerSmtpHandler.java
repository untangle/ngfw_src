/**
 * $Id$
 */

package com.untangle.app.phish_blocker;

import java.util.Map;

import com.untangle.app.smtp.WrappedMessageGenerator;
import com.untangle.app.spam_blocker.SpamReport;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;

/**
 * Protocol Handler which is called-back as scanable messages are encountered.
 */
public class PhishBlockerSmtpHandler extends com.untangle.app.spam_blocker.SpamSmtpHandler
{
    private static final String MOD_SUB_TEMPLATE = "[PHISH] $MIMEMessage:SUBJECT$";

    private WrappedMessageGenerator msgGenerator;

    /**
     * Constructor
     * 
     * @param app
     *        The phish blocker application
     */
    protected PhishBlockerSmtpHandler(PhishBlockerApp app)
    {
        super(app);

        msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE, getTranslatedBodyTemplate(), this);
    }

    /**
     * Gets the translated body for a blocked message
     * 
     * @return The translated body
     */
    @Override
    public String getTranslatedBodyTemplate()
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String bodyTemplate = i18nUtil.tr("The attached message from") + " $MIMEMessage:FROM$\r\n" + i18nUtil.tr("was determined by the Phish Blocker to be phish (a fraudulent email intended to steal information).") + "  " + "\n\r" + i18nUtil.tr("The kind of phish that was found was") + " $SPAMReport:FULL$";
        return bodyTemplate;
    }

    /**
     * Gets the translated subject for a blocked message
     * 
     * @return
     */
    @Override
    public String getTranslatedSubjectTemplate()
    {
        return MOD_SUB_TEMPLATE;
    }

    /**
     * Get the quarantine category for a blocked message
     * 
     * @return The quarantine category
     */
    @Override
    protected String getQuarantineCategory()
    {
        return "PHISH";
    }

    /**
     * Gets the quarantine detail for a blocked message
     * 
     * @param report
     *        The spam report
     * @return The quarantine detail
     */
    @Override
    protected String getQuarantineDetail(SpamReport report)
    {
        return "PHISH";
    }

    /**
     * Gets the wrapped message
     * 
     * @return The wrapped message
     */
    @Override
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return msgGenerator;
    }
}
