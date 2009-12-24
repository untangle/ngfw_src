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

package com.untangle.node.phish;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.BrandingSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.Tid;

class PhishReplacementGenerator
    extends ReplacementGenerator<PhishBlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This web page was blocked because it may be designed to steal"
        + " personal information.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    // constructors -----------------------------------------------------------

    PhishReplacementGenerator(Tid tid)
    {
        super(tid);
    }

    // ReplacementGenerator methods -------------------------------------------

    @Override
    protected String getReplacement(PhishBlockDetails details)
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();

        return String.format(BLOCK_TEMPLATE, details.getHost(),
                             details.getUri(), bs.getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, Tid tid)
    {
        return "http://" + host + "/idblocker/blockpage?nonce=" + nonce
            + "&tid=" + tid;
    }

    @Override
    protected PhishBlockDetails getTestData()
    {
        try {
            return new PhishBlockDetails( "test-host.example.com", 
                                          "/sample-phish", 
                                          InetAddress.getByName( "192.168.1.101" ));
        } catch ( UnknownHostException e ) {
            return null;
        }
    }
}
