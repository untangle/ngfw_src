/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.tran.mail.impl.imap.ImapCasingFactory;
import com.metavize.tran.mail.impl.smtp.SmtpCasingFactory;
import com.metavize.tran.mail.papi.*;
import org.apache.log4j.Logger;

public class MailTransformImpl extends AbstractTransform
    implements MailTransform, MailExport
{
    private final Logger logger = Logger.getLogger(MailTransformImpl.class);

    private final CasingPipeSpec SMTP_PIPE_SPEC = new CasingPipeSpec
        ("smtp", this, SmtpCasingFactory.factory(),
         Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS);
    private final CasingPipeSpec POP_PIPE_SPEC = new CasingPipeSpec
        ("pop", this, PopCasingFactory.factory(),
         Fitting.POP_STREAM, Fitting.POP_TOKENS);
    private final CasingPipeSpec IMAP_PIPE_SPEC = new CasingPipeSpec
        ("imap", this, ImapCasingFactory.factory(),
         Fitting.IMAP_STREAM, Fitting.IMAP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { SMTP_PIPE_SPEC, POP_PIPE_SPEC, IMAP_PIPE_SPEC };

    private MailTransformCommon common;

    // constructors -----------------------------------------------------------

    public MailTransformImpl()
    {
        logger.debug("MailTransformImpl");

        MailExportFactory.factory().registerExport(getTid().getPolicy(), this);
    }

    // MailTransform methods --------------------------------------------------

    public MailTransformSettings getMailTransformSettings()
    {
        return null == common ? null : common.getMailTransformSettings();
    }

    public void setMailTransformSettings(MailTransformSettings settings)
    {
        if (null != common) {
            common.setMailTransformSettings(this, settings);
        }
    }

    // MailExport methods -----------------------------------------------------

    public MailTransformSettings getExportSettings()
    {
        return getMailTransformSettings();
    }

    // Transform methods ------------------------------------------------------

    public void reconfigure()
    {
        if (null != common) {
            common.reconfigure();
        }
    }

    protected void initializeSettings() { }

    protected void postInit(String[] args)
    {
        common = MailTransformCommon.common(this);
        common.registerListener(this);
        doReconfigure(common.getMailTransformSettings());
    }

    protected void preDestroy()
    {
        common.deregisterListener(this);
        common = null;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // package protected methods ----------------------------------------------

    void doReconfigure(MailTransformSettings settings)
    {
        SMTP_PIPE_SPEC.setEnabled(settings.isSmtpEnabled());
        POP_PIPE_SPEC.setEnabled(settings.isPopEnabled());
        IMAP_PIPE_SPEC.setEnabled(settings.isImapEnabled());

        /* release session if parser doesn't catch or
         * explicitly throws its own parse exception
         * (parser will catch certain parse exceptions)
         */
        POP_PIPE_SPEC.setReleaseParseExceptions(true);

    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getMailTransformSettings();
    }

    public void setSettings(Object settings)
    {
        setMailTransformSettings((MailTransformSettings)settings);
    }
}
