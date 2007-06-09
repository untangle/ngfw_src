/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ConnectivityTester.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.uvm;

/**
 * Allows the user to customize the branding of the product.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface BrandingManager
{
    /**
     * Get the settings.
     *
     * @return the settings.
     */
    BrandingSettings getBrandingSettings();

    /**
     * Set the settings.
     *
     * @param bs the settings.
     */
    void setBrandingSettings(BrandingSettings bs);
}
