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

package com.untangle.uvm.logging;

/**
 * Layout for each logging context's section in the log emails.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class UtMailLayout extends UtPatternlayout
{
    // If null, we're the uvm.
    private final String componentName;

    // constructors -----------------------------------------------------------

    public UtMailLayout(String componentName)
    {
        // This gets reset by our xml config later...
        super(UtPatternlayout.MV_DEFAULT_CONVERSION_PATTERN);

        this.componentName = componentName;
    }

    // Layout methods ---------------------------------------------------------

    @Override
    public String getHeader()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(componentName);
        sb.append("\n");
        for (int i = 0; i < componentName.length(); i++) {
            sb.append('-');
        }
        sb.append("\n");

        return sb.toString();
    }

    @Override
    public String getFooter()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\nEnd of ");
        sb.append(componentName);
        sb.append("\n\n");

        return sb.toString();
    }
}
