/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.util.Map;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.I18nUtil;

public class ReportingOutsideAccessValve extends OutsideValve
{
    public ReportingOutsideAccessValve() {}

    protected boolean isOutsideAccessAllowed()
    {
        return getSystemSettings().getOutsideHttpsReportingEnabled();
    }

    protected String outsideErrorMessage()
    {
        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("off-site access to reporting is not allowed", i18n_map);
    }

    protected String httpErrorMessage()
    {
        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("standard access to reporting", i18n_map);
    }
}
