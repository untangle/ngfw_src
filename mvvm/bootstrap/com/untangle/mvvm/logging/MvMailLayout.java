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

public class MvMailLayout extends MvPatternLayout {

    // If null, we're the mvvm.
    private String componentName;

    public MvMailLayout(String componentName) {
        // This will get reset by our xml config later...
        super(MvPatternLayout.MV_DEFAULT_CONVERSION_PATTERN);
        this.componentName = componentName;
    }

    @Override public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(componentName);
        sb.append("\n");
        for (int i = 0; i < componentName.length(); i++)
            sb.append('-');
        sb.append("\n");
        return sb.toString();
    }
    @Override public String getFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nEnd of ");
        sb.append(componentName);
        sb.append("\n\n");
        return sb.toString();
    }
}
