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

package com.untangle.node.virus;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.Tid;

/**
 * ReplacementGenerator for Virus.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class VirusReplacementGenerator
    extends ReplacementGenerator<VirusBlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site blocked because of inappropriate content</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    // constructors -----------------------------------------------------------

    VirusReplacementGenerator(Tid tid)
    {
        super(tid);
    }

    // ReplacementGenerator methods -------------------------------------------

    @Override
    protected String getReplacement(VirusBlockDetails details)
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();

        return String.format(BLOCK_TEMPLATE, details.getVendor(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             bs.getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, Tid tid)
    {
        return "http://" + host + "/virus/blockpage?nonce=" + nonce
            + "&tid=" + tid;
    }

    @Override
    protected VirusBlockDetails getTestData()
    {
        return new VirusBlockDetails( "test-host.example.com", 
                                      "http://test-host.example.com/sample-virus", 
                                      "testing", "virus" );
    }
}
