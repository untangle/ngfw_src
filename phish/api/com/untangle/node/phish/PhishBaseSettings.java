/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/phish/api/com/untangle/node/phish/PhishBaseSettings.java $
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

import java.io.Serializable;

import com.untangle.node.spam.SpamBaseSettings;

/**
 * Settings for the Phish node.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class PhishBaseSettings extends SpamBaseSettings implements Serializable
{
    private boolean enableGooglePhishList = true;

    // constructors -----------------------------------------------------------

    public PhishBaseSettings() {}

    
    // accessors --------------------------------------------------------------

    public boolean getEnableGooglePhishList()
    {
        return enableGooglePhishList;
    }

    public void setEnableGooglePhishList(boolean enableGooglePhishList)
    {
        this.enableGooglePhishList = enableGooglePhishList;
    }
}
