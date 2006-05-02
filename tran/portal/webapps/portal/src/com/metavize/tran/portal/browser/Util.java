/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.portal.browser;

class Util
{
    static String stripSlash(String s)
    {
        while ('/' == s.charAt(s.length() - 1)) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }
}
