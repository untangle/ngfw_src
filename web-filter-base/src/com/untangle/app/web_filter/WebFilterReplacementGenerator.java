/**
 * $Id: WebFilterReplacementGenerator.java 41284 2015-09-18 07:03:39Z dmorris $
 */
package com.untangle.app.web_filter;

import java.net.InetAddress;

import com.untangle.app.web_filter.WebFilterBlockDetails;
import com.untangle.app.web_filter.WebFilterReplacementGenerator;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * ReplacementGenerator for Web Filter.
 */
public class WebFilterReplacementGenerator extends WebFilterBaseReplacementGenerator
{
    public WebFilterReplacementGenerator(AppSettings appSettings)
    {
        super(appSettings);
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        return "http://" + host + "/web-filter/blockpage?nonce=" + nonce + "&appid=" + appSettings.getId();
    }
}
