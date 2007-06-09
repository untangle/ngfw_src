/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.untangle.node.util.test;

import java.io.*;

import com.untangle.node.util.*;


/**
 * ...name says it all.  Currently, just run from "main" but could
 * be made into JUnit, etc.
 *
 * Here is an additional "tick" to verify things:
 * prompt> openssl s_server -CAfile ca-pub.crt -key my-pk1.pri -cert my-cert1.crt -www -debug
 *
 * This launches a little web server on port 4433.  You can then observe the browser's
 * reaction to the cert chain, etc.
 */
public class OpenSSLTests {

    public static void main(String[] args) throws Exception {

        System.out.println("Create a CA");
        OpenSSLCAWrapper ca =
            OpenSSLCAWrapper.create(new File("myTestCA"), false);

        System.out.println("Output our CA's public cert to \"ca-pub.crt\"");
        IOUtil.bytesToFile(ca.getCACert(), new File("ca-pub.crt"));

        System.out.println("Create a private key");
        byte[] pk1Bytes = OpenSSLWrapper.genKey();
        File privateKey1File = new File("my-pk1.pri");
        IOUtil.bytesToFile(pk1Bytes, privateKey1File);

        System.out.println("Create a CSR");
        byte[] csr1Bytes = OpenSSLWrapper.createCSR("gobbles.metavize.com",
                                                    "metavize", "US", "California", "San Mateo", pk1Bytes);
        File csr1File = new File("my-csr1.pem");
        IOUtil.bytesToFile(csr1Bytes, csr1File);

        System.out.println("Sign the CSR from our CA");
        byte[] cert1Bytes = ca.signCSR(csr1Bytes);
        File cert1File = new File("my-cert1.crt");
        IOUtil.bytesToFile(cert1Bytes, cert1File);

    }

}
