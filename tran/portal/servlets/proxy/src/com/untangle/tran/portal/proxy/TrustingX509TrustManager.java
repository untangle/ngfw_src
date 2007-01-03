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

package com.untangle.tran.portal.proxy;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustingX509TrustManager implements X509TrustManager
{
    public X509Certificate[] getAcceptedIssuers()
    {
        return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType)
    {
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType)
    {
    }
}
