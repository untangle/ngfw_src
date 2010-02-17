/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util.test;

import java.io.File;

import com.untangle.node.util.IOUtil;
import com.untangle.node.util.OpenSSLCAWrapper;
import com.untangle.node.util.OpenSSLWrapper;


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
