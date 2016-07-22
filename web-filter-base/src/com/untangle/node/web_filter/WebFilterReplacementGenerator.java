/**
 * $Id: WebFilterReplacementGenerator.java 41284 2015-09-18 07:03:39Z dmorris $
 */
package com.untangle.node.web_filter;

import java.net.InetAddress;

import com.untangle.node.web_filter.WebFilterBlockDetails;
import com.untangle.node.web_filter.WebFilterReplacementGenerator;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;

/**
 * ReplacementGenerator for Web Filter.
 */
public class WebFilterReplacementGenerator extends WebFilterBaseReplacementGenerator
{
    public WebFilterReplacementGenerator(NodeSettings nodeSettings)
    {
        super(nodeSettings);
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeSettings nodeSettings)
    {
        return "http://" + host + "/web-filter/blockpage?nonce=" + nonce + "&tid=" + nodeSettings.getId();
    }
}
