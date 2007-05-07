/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.logging;

/**
 * Layout for each logging context's section in the log emails.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class MvMailLayout extends MvPatternLayout
{
    // If null, we're the mvvm.
    private final String componentName;

    public MvMailLayout(String componentName)
    {
        // This gets reset by our xml config later...
        super(MvPatternLayout.MV_DEFAULT_CONVERSION_PATTERN);

        this.componentName = componentName;
    }

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
