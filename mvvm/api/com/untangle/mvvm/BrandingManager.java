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

package com.untangle.mvvm;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Allows the user to customize the branding of the product.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface BrandingManager
{
    Set<String> getImageNames();
    byte[] getImage(String name) throws IOException;
    Map<String, byte[]> getImages() throws IOException;
    void addImage(String filename, byte[] image) throws IOException;
}
