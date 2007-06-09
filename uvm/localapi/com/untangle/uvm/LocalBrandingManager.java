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

package com.untangle.uvm;

import java.io.File;

/**
 * Allows the user to customize the branding of the product.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface LocalBrandingManager extends BrandingManager
{
    /**
     * Retrieve the file that holds the logo.
     */
    File getLogoFile();
}
