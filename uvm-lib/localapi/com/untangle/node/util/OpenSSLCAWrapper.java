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

package com.untangle.node.util;
import static com.untangle.node.util.Ascii.LF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//Uses Exit code 1 if there is a problem.

//This command is used to verify the signature of the
//CA on the given signed cert
//openssl verify -CAfile ca-pub.crt my-cert.crt
//
//The output is lame - "my-cert.crt: OK"

/**
 * Wrapper around using OpenSSL as a small CA.  To "delete" the CA
 * simply delete the root file.
 * <br>
 * This was created for the wrong reasons (don't ask), but may be useful
 * in the future.
 */
public class OpenSSLCAWrapper {

    private static final String CONF_FILE_NAME = "openssl.cnf";

    private final File m_rootDir;
    private final boolean m_useUVMThreads;

    private OpenSSLCAWrapper(File rootDir, boolean useUVMThreads) {
        m_rootDir = rootDir;
        m_useUVMThreads = useUVMThreads;
    }

    /**
     * Get the CA cert for this CA instance.  Note that if this is going
     * to be sent to a browser or put into an email, the file should
     * be of MIME type "application/x-x509-ca-cert"
     *
     * @return the bytes of the CA cert.  May be written to a ".pem",
     *         ".crt" file.
     */
    public byte[] getCACert() throws IOException {
        return IOUtil.fileToBytes(new File(m_rootDir, "ca-cert.pem"));
    }

    /**
     * Sign the given Certificate Sign Request
     *
     * @param certRequest the bytes of the cert request
     *
     * @return the bytes of the signed certificate
     */
    public byte[] signCSR(byte[] certRequest)
        throws IOException {

        File temp = File.createTempFile("cert", ".tmp", m_rootDir);
        try {
            IOUtil.bytesToFile(certRequest, temp);
            byte[] ret = signCSR(temp);
            IOUtil.delete(temp);
            return ret;
        }
        catch(IOException ex) {
            IOUtil.delete(temp);
            throw ex;
        }
    }

    /**
     * Sign the given Certificate Sign Request
     *
     * @param certRequest the file containing the cert request
     *
     * @return the bytes of the signed certificate
     */
    public byte[] signCSR(File certRequest)
        throws IOException {

        //openssl ca -batch -config file -in file

        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "openssl",
                                                             new String[] {
                                                                 "ca",
                                                                 "-batch",
                                                                 "-config",
                                                                 m_rootDir.getAbsolutePath() + File.separator + CONF_FILE_NAME,
                                                                 "-in",
                                                                 certRequest.getAbsolutePath(),
                                                                 "-notext"
                                                             },
                                                             null,
                                                             m_rootDir,
                                                             true,
                                                             false,
                                                             1000*60);

        if(result.exitCode==0) {
            return result.stdOut;
        }
        throw new IOException("openssl exited with value " + result.exitCode);
    }


    /**
     * Create a new CA with all programatic defaults (i.e. identifies itself
     * as "mv-edgeguard" from san mateo).
     *
     * @param rootDir the root dir of this CA.  <b>Warning</b> - there is currently
     *        no check to see if you're clobbering existing files
     *
     * @param useUVMThreads when exec-ing openssl, should we assume we are
     *        in the UVM process.
     *
     * @return an initialized instance
     */
    public static OpenSSLCAWrapper create(File rootDir, boolean useUVMThreads)
        throws IOException {
        return create(rootDir,
                      useUVMThreads,
                      "mv-edgeguard" + System.currentTimeMillis(),
                      "US",
                      "CA",
                      "San Mateo",
                      "mv-devices",
                      "mv-edgeguard@localhost");
    }

    /**
     * Create a new CA.
     *
     * @param rootDir the root dir of this CA.  <b>Warning</b> - there is currently
     *        no check to see if you're clobbering existing files
     * @param useUVMThreads when exec-ing openssl, should we assume we are
     *        in the UVM process.
     * @param commonName the CN of the CA
     * @param country
     * @param state
     * @param city
     * @param organization
     * @param email
     *
     * @return an initialized instance
     */
    public static OpenSSLCAWrapper create(File rootDir,
                                          boolean useUVMThreads,
                                          String commonName,
                                          String country,
                                          String state,
                                          String city,
                                          String organization,
                                          String email) throws IOException {

        if(!rootDir.exists()) {
            rootDir.mkdirs();
        }

        //Create the conf dir
        String conf = createConf(rootDir,
                                 commonName,
                                 country,
                                 state,
                                 city,
                                 organization,
                                 email);

        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(new File(rootDir, CONF_FILE_NAME));
            fOut.write(conf.getBytes());
            fOut.flush();
            fOut.close();
        }
        catch(IOException ex) {
            IOUtil.close(fOut);
            throw ex;
        }

        //Create the required files/directories
        File crl = new File(rootDir, "crl");
        if(!crl.exists()) {
            crl.mkdirs();
        }
        File prv = new File(rootDir, "private");
        if(!prv.exists()) {
            prv.mkdirs();
        }
        File newcerts = new File(rootDir, "newcerts");
        if(!newcerts.exists()) {
            newcerts.mkdirs();
        }

        fOut = null;
        try {
            fOut = new FileOutputStream(new File(rootDir, "serial"));
            fOut.write("01\n".getBytes());
            fOut.flush();
            fOut.close();
        }
        catch(IOException ex) {
            IOUtil.close(fOut);
            throw ex;
        }

        File index = new File(rootDir, "index");
        index.createNewFile();

        //Now, create our private key
        //openssl req -nodes -config openssl.cnf -days 1825 -x509 -newkey rsa -out ca-cert.pem -outform PEM
        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "openssl",
                                                             new String[] {
                                                                 "req",
                                                                 "-nodes",
                                                                 "-config",
                                                                 CONF_FILE_NAME,
                                                                 "-days",
                                                                 "1825",
                                                                 "-x509",
                                                                 "-newkey",
                                                                 "rsa",
                                                                 "-out",
                                                                 "ca-cert.pem",
                                                                 "-outform",
                                                                 "PEM"
                                                             },
                                                             null,
                                                             rootDir,
                                                             false,
                                                             false,
                                                             1000*60);


        return new OpenSSLCAWrapper(rootDir, useUVMThreads);

    }

    /**
     * Creates the config file wired to be a CA.  Note that
     * we turn off things like  any sembelance of constraints
     * on the requests as well as unique subjects within the scope
     * of our local database.
     */
    private static String createConf(File rootDir,
                                     String commonName,
                                     String country,
                                     String state,
                                     String city,
                                     String organization,
                                     String email) {

        StringBuilder sb = new StringBuilder();


        sb.append("HOME                    = .").append(LF);
        sb.append("RANDFILE                = $ENV::HOME/.rnd").append(LF);
        sb.append("[ ca ]").append(LF);
        sb.append("default_ca      = CA_default").append(LF);

        sb.append("[ CA_default ]").append(LF);
        sb.append("dir             = " + rootDir.getAbsolutePath()).append(LF);
        sb.append("new_certs_dir   = $dir/newcerts").append(LF);
        sb.append("crl_dir         = $dir/crl").append(LF);
        sb.append("database        = $dir/index").append(LF);
        sb.append("certificate     = $dir/ca-cert.pem").append(LF);
        sb.append("serial          = $dir/serial").append(LF);
        sb.append("crl             = $dir/ca-crl.pem").append(LF);
        sb.append("private_key     = $dir/private/ca-key.pem").append(LF);
        sb.append("RANDFILE        = $dir/private/.rand").append(LF);
        sb.append("unique_subject  = no").append(LF);

        sb.append("x509_extensions = usr_cert").append(LF);
        sb.append("default_crl_days= 30").append(LF);
        sb.append("default_days    = 1825").append(LF);
        sb.append("default_md      = sha1").append(LF);
        sb.append("preserve        = no").append(LF);

        sb.append("policy = policy_anything").append(LF);

        sb.append("[ policy_anything ]").append(LF);
        sb.append("countryName             = optional").append(LF);
        sb.append("stateOrProvinceName     = optional").append(LF);
        sb.append("localityName            = optional").append(LF);
        sb.append("organizationName        = optional").append(LF);
        sb.append("organizationalUnitName  = optional").append(LF);
        sb.append("commonName              = supplied").append(LF);
        sb.append("emailAddress            = optional").append(LF);

        sb.append("[ req ]").append(LF);
        sb.append("default_bits            = 2048").append(LF);
        sb.append("default_keyfile         = ./private/ca-key.pem").append(LF);
        sb.append("default_md              = sha1").append(LF);

        sb.append("prompt                  = no").append(LF);
        sb.append("distinguished_name      = root_ca_distinguished_name").append(LF);

        sb.append("x509_extensions = v3_ca").append(LF);
        sb.append("string_mask = nombstr").append(LF);

        sb.append("[ root_ca_distinguished_name ]").append(LF);
        sb.append("commonName = " + commonName).append(LF);
        sb.append("countryName = " + country).append(LF);
        sb.append("stateOrProvinceName = " + state).append(LF);
        sb.append("localityName = " + city).append(LF);
        sb.append("0.organizationName = " + organization).append(LF);
        sb.append("emailAddress = " + email).append(LF);

        sb.append("[ usr_cert ]").append(LF);
        sb.append("basicConstraints=CA:FALSE").append(LF);
        sb.append("subjectKeyIdentifier=hash").append(LF);
        sb.append("authorityKeyIdentifier=keyid,issuer:always").append(LF);

        sb.append("[ v3_req ]").append(LF);
        sb.append("basicConstraints = CA:FALSE").append(LF);
        sb.append("keyUsage = nonRepudiation, digitalSignature, keyEncipherment").append(LF);

        sb.append("[ v3_ca ]").append(LF);
        sb.append("subjectKeyIdentifier=hash").append(LF);
        sb.append("authorityKeyIdentifier=keyid:always,issuer:always").append(LF);
        sb.append("basicConstraints = CA:true").append(LF);

        sb.append("[ crl_ext ]").append(LF);
        sb.append("authorityKeyIdentifier=keyid:always,issuer:always").append(LF);

        return sb.toString();

    }



    public static void main(String[] args) throws Exception {

        OpenSSLCAWrapper wrapper = OpenSSLCAWrapper.create(new File("myNewCA"), false);

        doIt(wrapper, "foo", "foo1");
        doIt(wrapper, "foo", "foo2");
        doIt(wrapper, "boo", "boo1");
        doIt(wrapper, "boo", "boo2");

    }

    private static void doIt(OpenSSLCAWrapper wrapper, String cn, String name)
        throws Exception {
        byte[] pK1Bytes = OpenSSLWrapper.genKey();
        File pK1File = new File(name + ".pri");
        IOUtil.bytesToFile(pK1Bytes, pK1File);

        byte[] cRSBytes = OpenSSLWrapper.createCSR(cn + ".com",
                                                   name,
                                                   "US",
                                                   "Cali",
                                                   "San Mateo",
                                                   pK1File);
        File cSRFile = new File(name + ".csr");
        IOUtil.bytesToFile(cRSBytes, cSRFile);

        byte[] certBytes = wrapper.signCSR(cSRFile);
        File certFile = new File(name + ".crt");
        IOUtil.bytesToFile(certBytes, certFile);
    }


}

