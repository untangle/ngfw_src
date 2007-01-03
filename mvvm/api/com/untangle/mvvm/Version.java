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

package com.untangle.mvvm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author  Dirk Morris
 */
public class Version {
    public static String getVersion()
    {
        String line;

        try {
            InputStream is = Version.class.getClassLoader().getResourceAsStream("PUBVERSION");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bis = new BufferedReader(isr);
            line = bis.readLine();
        } catch (IOException exn) {
            line = "unknown version";
        }

        return line;
    }
}
