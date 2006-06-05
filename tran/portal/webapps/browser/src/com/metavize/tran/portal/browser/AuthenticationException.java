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

class AuthenticationException extends Exception
{
    private final String url;

    public AuthenticationException(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }
}
