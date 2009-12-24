/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spyware;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.Tid;

public class SpywareReplacementGenerator
    extends ReplacementGenerator<SpywareBlockDetails>
{
    private static final String SIMPLE_BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<script id='metavizeDetect' type='text/javascript'>\n"
        + "var e = document.getElementById(\"metavizeDetect\")\n"
        + "if (window == window.top && e.parentNode.tagName == \"BODY\") {\n"
        + "  document.writeln(\"<center><b>Untangle Spyware Blocker</b></center>\")\n"
        + "  document.writeln(\"<p>This site blocked because it may be a spyware site.</p>\")\n"
        + "  document.writeln(\"<p>Host: %s</p>\")\n"
        + "  document.writeln(\"<p>URI: %s</p>\")\n"
        + "  document.writeln(\"<p>Please contact %s.</p>\")\n"
        + "}\n"
        + "</script>"
        + "</BODY></HTML>";

    // constructors -----------------------------------------------------------

    public SpywareReplacementGenerator(Tid tid)
    {
        super(tid);
    }

    // ReplacementGenerator methods -------------------------------------------

    protected String getReplacement(SpywareBlockDetails bd)
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();
        return String.format(SIMPLE_BLOCK_TEMPLATE, bd.getHost(), bd.getUrl(),
                             bs.getContactHtml());
    }

    protected String getRedirectUrl(String nonce, String host, Tid tid)
    {
        return "http://" + host + "/spyware/detect.jsp?nonce=" + nonce
            + "&tid=" + tid;
    }

    @Override
    protected SpywareBlockDetails getTestData()
    {
        try {
            return new SpywareBlockDetails( "test-host.example.com", 
                                            "/simple-spyware", 
                                            InetAddress.getByName( "192.168.1.101" ));
        } catch ( UnknownHostException e ) {
            return null;
        }
    }
}
