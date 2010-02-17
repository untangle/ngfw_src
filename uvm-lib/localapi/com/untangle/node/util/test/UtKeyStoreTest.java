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
import com.untangle.node.util.UtKeyStore;
import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.security.RFC2253Name;


/**
 * Name says it all
 */
public class UtKeyStoreTest {


    private static void testSep(String test) {
        System.out.println("");
        System.out.println("");
        System.out.println("======================= " + test + " ===============================");
        System.out.println("");
    }

    private static void stepSep() {
        System.out.println("");
        System.out.println("");
        System.out.println("-----------------------------------------------------");
    }



    private static void debug(String msg) {
        System.out.println(msg);
    }

    private static boolean doTest1(OpenSSLCAWrapper ca) {
        testSep("doTest1");
        try {

            //--------------------------------------------------
            stepSep();
            debug("Create KeyStore");
            UtKeyStore kt = UtKeyStore.open("myKeystore1", "changeit", true);


            //--------------------------------------------------
            stepSep();
            debug("List");
            String[] aliases = kt.listAliases();

            for(String alias : aliases) {
                debug("Alias: " + alias);
            }


            //--------------------------------------------------
            stepSep();
            RFC2253Name dn = RFC2253Name.create();
            dn.add("CN", "foo.com");
            dn.add("L", "San Mateo");
            kt.generateKey("foo", dn);
            debug("Generated key for foo");

            //--------------------------------------------------
            stepSep();
            debug("Get cert info");
            byte[] tempSelfSignedCert = kt.exportEntry("foo");
            if(tempSelfSignedCert == null) {
                debug("Self signed cert is null!?!");
            }
            CertInfo ci = OpenSSLWrapper.getCertInfo(tempSelfSignedCert);
            debug(ci.toString());


            //--------------------------------------------------
            stepSep();
            debug("Check if it is self-signed");
            debug("Self signed? " + ci.appearsSelfSigned());

            //--------------------------------------------------
            stepSep();
            debug("Create CSR");
            byte[] csrBytes = kt.createCSR("foo");
            File csrFile = new File("foo.csr");
            IOUtil.bytesToFile(csrBytes, csrFile);


            //--------------------------------------------------
            stepSep();
            debug("Sign the CSR");
            byte[] signedCertBytes = ca.signCSR(csrFile);
            File signedCertFile = new File("foo.crt");
            IOUtil.bytesToFile(signedCertBytes, signedCertFile);


            //--------------------------------------------------
            stepSep();
            debug("Print cert info");
            ci = OpenSSLWrapper.getCertInfo(signedCertBytes);
            debug(ci.toString());


            //--------------------------------------------------
            stepSep();
            debug("Create a bogus key, CSR (with same CN) and sign it.  We");
            debug("will then attempt to import it and get an error.");
            byte[] bogusPKBytes = OpenSSLWrapper.genKey();
            byte[] bogusCSR = OpenSSLWrapper.createCSR(
                                                       "foo.com",
                                                       null,
                                                       null,
                                                       null,
                                                       "San Mateo",
                                                       bogusPKBytes);
            byte[] bogusCert = ca.signCSR(bogusCSR);

            try {
                kt.importCert(bogusCert, "foo");
                debug("Expected error");
                System.exit(100);
            }
            catch(Exception ex) {
                debug("Got error (as expected) importing wrong cert under an existing alias");
                ex.printStackTrace();
            }

            //--------------------------------------------------
            stepSep();
            debug("Attempt to import the signed cert (correct one) before");
            debug("importing the CA cert.  Should be an error");
            try {
                kt.importCert(signedCertFile, "foo");
                debug("Expected error");
                System.exit(100);
            }
            catch(Exception ex) {
                debug("Got error (as expected) importing wrong cert under an existing alias");
                ex.printStackTrace();
            }

            //--------------------------------------------------
            stepSep();
            debug("Import this CA's cert into the KeyStore");
            kt.importCert(new File("myCA/ca-cert.pem"), "fooCA");

            //--------------------------------------------------
            stepSep();
            debug("Now, we should be able to import our *signed* cert (the good one).");
            kt.importCert(signedCertFile, "foo");


            //--------------------------------------------------
            stepSep();
            debug("List");
            aliases = kt.listAliases();

            for(String alias : aliases) {
                System.out.println("Alias: " + alias);
            }
            return true;
        }
        catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }


    public static void main(String[] args) throws Exception {

        // Create a CA.  Used for a few of the tests
        stepSep();
        debug("Create an OpenSSLCA for all tests");
        OpenSSLCAWrapper ca = OpenSSLCAWrapper.create(new File("myCA"),
                                                      false,
                                                      "ca.metavize.com",
                                                      "US", "California", "San Mateo", "metavize ca", "foo@moo.com");

        if(!doTest1(ca)) {
            System.out.println("***TEST FAILED***");
        }

    }

}

