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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.util.ArrayList;

import com.untangle.uvm.security.RFC2253Name;

//TODO backup keystore on all operations

/**
 * Wrapper around the crappy Java Keytool application.  Uses a
 * mix of calls to the native app as well as some programatic
 * manipulation to make-up for some lacking features in the command-
 * line version.
 * <br>
 * Instances of this class are <b>not</b> threadsafe, and it is
 * recommended that a single (synchronized) instance be used for a given
 * VM/File pair.
 */
public class UtKeyStore {

    private final String m_ktPass;
    private final File m_ktFile;


    /**
     * Enum of the different entries within the key store
     */
    public static enum UtKSEntryType {
        PrivKey,
        SecKey,
        TrustedCert
    };

    /**
     * Private constructor.
     */
    private UtKeyStore(File ktFile, String ktPass) {
        m_ktFile = ktFile;
        m_ktPass = ktPass;
    }

    /**
     * Export an entry from the key store.
     *
     * Returns null if there is no such alias
     *
     * @param alias the alias
     *
     * @return the bytes of the exported cert
     */
    public byte[] exportEntry(String alias)
        throws IOException {

        //=============================================
        // keytool -export -rfc -alias foo \
        // -keystore myKeystore
        //=============================================

        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "keytool",
                                                             new String[] {
                                                                 "-export",
                                                                 "-rfc",
                                                                 "-keystore",
                                                                 m_ktFile.getAbsolutePath(),
                                                                 "-storepass",
                                                                 m_ktPass,
                                                                 "-keypass",
                                                                 m_ktPass,
                                                                 "-alias",
                                                                 alias
                                                             },
                                                             null,
                                                             null,
                                                             true,
                                                             true,
                                                             1000*60);


        if(result.exitCode!=0) {
            throw new IOException("Exception Importing cert.  Return code " + result.exitCode +
                                  ", stdout \"" +
                                  new String(result.stdOut) + "\", stderr \"" +
                                  new String(result.stdErr) + "\"");
        }
        return result.stdOut;
    }

    /**
     * True if the operation was a success.  False if there
     * is no such key.recommended
     * <br>
     * <b>Warning - this will overwrite an existing
     * key, so check via {@link #containsAlias containsAlias}</b>
     *
     * @param alias the entry alias
     *
     * @return true if the entry existed and it was renamed.
     */
    public boolean renameEntry(String oldAlias, String newAlias)
        throws GeneralSecurityException, IOException {

        if(!containsAlias(oldAlias)) {
            return false;
        }

        KeyStore ks = openKeyJavaStore();

        UtKSEntryType type = getEntryTypeImpl(ks, oldAlias);
        if(type == null) {
            return false;
        }

        KeyStore.Entry entry = null;


        switch(type) {
        case SecKey:
        case PrivKey:
            entry = ks.getEntry(oldAlias, new KeyStore.PasswordProtection(m_ktPass.toCharArray()));
            ks.setEntry(newAlias, entry, new KeyStore.PasswordProtection(m_ktPass.toCharArray()));
            break;
        case TrustedCert:
            entry = ks.getEntry(oldAlias, null);
            ks.setEntry(newAlias, entry, null);
            break;
        }

        //Now, save it
        saveKeystore(ks, m_ktFile, m_ktPass);

        return true;

    }



    /**
     * Import a cert into the store.  This call should be used
     * to import trusted certs (i.e. CA certs, client certs etc).
     * <br>
     * Note that this clobbers existing entries (i.e. you may want to
     * check containsAlias first).  Also, it permits placing the
     * same cert into the store under different aliases.
     *
     * @param certBytes the bytes of the cert
     * @param alias the alias into-which the cert will be placed
     */
    public void importCert(byte[] certBytes, String alias)
        throws IOException {
        File temp = File.createTempFile("cert", ".tmp", m_ktFile.getParentFile());
        try {
            IOUtil.bytesToFile(certBytes, temp);
            importCert(temp, alias);
            IOUtil.delete(temp);
        }
        catch(IOException ex) {
            IOUtil.delete(temp);
            throw ex;
        }
    }



    /**
     * Import a cert into the store.  This call should be used
     * to import trusted certs (i.e. CA certs, client certs etc).
     * <br>
     * Note that this clobbers existing entries (i.e. you may want to
     * check containsAlias first).  Also, it permits placing the
     * same cert into the store under different aliases.
     *
     * @param certFile the cert file
     * @param alias the alias into-which the cert will be placed
     */
    public void importCert(File certFile, String alias)
        throws IOException {

        //=================================================
        // keytool  -import -keystore keystore -storepass \
        //   changeit -keypass changeit -noprompt -alias fooCA \
        //   -file myNewCA/ca-cert.pem
        //=================================================

        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "keytool",
                                                             new String[] {
                                                                 "-import",
                                                                 "-keystore",
                                                                 m_ktFile.getAbsolutePath(),
                                                                 "-storepass",
                                                                 m_ktPass,
                                                                 "-keypass",
                                                                 m_ktPass,
                                                                 "-alias",
                                                                 alias,
                                                                 "-noprompt",
                                                                 "-file",
                                                                 certFile.getAbsolutePath()
                                                             },
                                                             null,
                                                             null,
                                                             true,
                                                             true,
                                                             1000*60);


        if(result.exitCode!=0) {
            throw new IOException("Exception Importing cert.  Return code " + result.exitCode +
                                  ", stdout \"" +
                                  new String(result.stdOut) + "\", stderr \"" +
                                  new String(result.stdErr) + "\"");
        }
    }



    /**
     * Create a certificate request.  Note that first one
     * must create the key within the store (under the same
     * alias) via {@link #generateKey generateKey}.
     *
     * @param alias the alias of an existing entry
     *
     * @exception InvalidKeyException if the alias does not exist
     */
    public byte[] createCSR(String alias)
        throws IOException,
               InvalidKeyException,
               GeneralSecurityException {

        //check if the alias exists
        if(!containsAlias(alias)) {
            throw new InvalidKeyException("No such alias \"" + alias + "\"");
        }

        // ====================================
        // Tested command:
        // keytool -certreq -keystore keystore \
                                      // -storepass changeit -keypass changeit \
                                      // -alias foo -keyalg RSA
                                      // ====================================
                                      SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                                                           "keytool",
                                                                                           new String[] {
                                                                                               "-certreq",
                                                                                               "-keystore",
                                                                                               m_ktFile.getAbsolutePath(),
                                                                                               "-storepass",
                                                                                               m_ktPass,
                                                                                               "-keypass",
                                                                                               m_ktPass,
                                                                                               "-alias",
                                                                                               alias,
                                                                                               "-keyalg",
                                                                                               "RSA"
                                                                                           },
                                                                                           null,
                                                                                           null,
                                                                                           true,
                                                                                           true,
                                                                                           1000*60);


                                      if(result.exitCode!=0) {
                                          throw new IOException("Exception creating CSR.  Return code " + result.exitCode +
                                                                ", stdout \"" +
                                                                new String(result.stdOut) + "\", stderr \"" +
                                                                new String(result.stdErr) + "\"");
                                      }
                                      return result.stdOut;
    }


    /**
     * Generate a new PrivateKey in the store.
     * Really, it generates a self-signed cert good
     * for 5 years.
     *
     * Note that the key is not returned, as it isn't
     * "needed".
     *
     * <b>Warning - this will overwrite an existing
     * key, so check via {@link #containsAlias containsAlias}</b>
     *
     * @param alias the alias
     * @param dname the distinguished name
     */
    public void generateKey(String alias,
                            RFC2253Name dname)
        throws IOException {
        generateKey(alias, dname, 365*5);
    }

    /**
     * Generate a new PrivateKey in the store.
     * Really, it generates a self-signed cert.
     *
     * Note that the key is not returned, as it isn't
     * "needed".
     *
     * <b>Warning - this will overwrite an existing
     * key, so check via {@link #containsAlias containsAlias}</b>
     *
     * @param alias the alias
     * @param dname the distinguished name
     * @param durationInDays the duration in days of the key
     */
    public void generateKey(String alias,
                            RFC2253Name dname,
                            int durationInDays)
        throws IOException {


        //========================================
        // Tested command
        // keytool -genkey -keystore keystore \
        //-storepass changeit -keypass changeit \
        //-alias foo -dname "C=US, ST=Cali, L=San Mateo, O=foo1, CN=foo.com"
        //========================================

        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "keytool",
                                                             new String[] {
                                                                 "-genkey",
                                                                 "-keystore",
                                                                 m_ktFile.getAbsolutePath(),
                                                                 "-storepass",
                                                                 m_ktPass,
                                                                 "-keypass",
                                                                 m_ktPass,
                                                                 "-keyalg",
                                                                 "RSA",
                                                                 "-alias",
                                                                 alias,
                                                                 "-validity",
                                                                 Integer.toString(durationInDays),
                                                                 "-dname",
                                                                 dname.toRFC2253String()
                                                             },
                                                             null,
                                                             null,
                                                             true,
                                                             true,
                                                             1000*10);


        if(result.exitCode!=0) {
            throw new IOException("Exception updating Keystore.  Return code " +
                                  result.exitCode +
                                  ", stdout \"" +
                                  new String(result.stdOut) + "\", stderr \"" +
                                  new String(result.stdErr) + "\"");
        }
    }


    /**
     * Test if this KeyStore contains the given alias.
     *
     * @param name the name of the entry
     *
     * @return true if contained.
     */
    public boolean containsAlias(String name)
        throws IOException,
               GeneralSecurityException {
        String[] aliases = listAliases();
        for(String alias : aliases) {
            if(name.equals(alias)) {
                return true;
            }
        }
        return false;
    }


    /**
     * List all aliases within the keystore
     *
     * @return all aliases
     */
    public String[] listAliases()
        throws IOException,
               GeneralSecurityException {

        KeyStore ks = openKeyJavaStore();
        java.util.Enumeration<String> aliases =
            ks.aliases();

        ArrayList<String> ret = new ArrayList<String>();
        while(aliases.hasMoreElements()) {
            ret.add(aliases.nextElement());
        }

        return ret.toArray(new String[ret.size()]);
    }

    public UtKSEntryType getEntryType(String alias)
        throws IOException, GeneralSecurityException {
        return getEntryTypeImpl(openKeyJavaStore(), alias);
    }


    /**
     * Create a new KeyStore
     *
     * @param fileName the name of the keystore file
     * @param pwd the password for the file
     *
     * @return the opened key store
     */
    public static UtKeyStore create(String fileName,
                                    String pwd)
        throws IOException {

        //============================================
        // Here is the command to create a keystore.
        // Note that you cannot just "create", you
        // need to implicitly create while adding
        // a key.  Hence, this "bootstrap" key for
        // a machine I never want to exist"
        //
        // keytool -genkey -keystore keystore \
        // -storepass changeit -keypass changeit \
        // -alias bootstrap -dname "CN=uselessbootstrap.untangle.com"
        //
        // If there is a problem, exits with "1"
        //============================================

        File f = new File(fileName);
        File dir = f.getParentFile();

        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "keytool",
                                                             new String[] {
                                                                 "-genkey",
                                                                 "-keystore",
                                                                 fileName,
                                                                 "-storepass",
                                                                 pwd,
                                                                 "-keypass",
                                                                 pwd,
                                                                 "-alias",
                                                                 "bootstrap",
                                                                 "-dname",
                                                                 "CN=uselessbootstrap.untangle.com"
                                                             },
                                                             null,
                                                             dir,
                                                             true,
                                                             true,
                                                             1000*60);


        if(result.exitCode==0) {
            return new UtKeyStore(f, pwd);
        }
        throw new IOException("Error creating file.  stdout \"" +
                              new String(result.stdOut) + "\", stderr \"" +
                              new String(result.stdErr) + "\"");
    }



    /**
     * Open a key store
     *
     * @param fileName the name of the keystore file
     * @param pwd the password for the file
     * @param autoCreate if true, the keystore file
     *        will be implicitly created if it does not
     *        exist
     *
     * @return the opened key store
     */
    public static UtKeyStore open(String fileName,
                                  String pwd,
                                  boolean autoCreate)
        throws FileNotFoundException,
               IOException,
               GeneralSecurityException {

        File f = new File(fileName);

        if(!f.exists()) {
            if(autoCreate) {
                return create(fileName, pwd);
            }
            throw new FileNotFoundException("No such keystore file \"" + fileName + "\"");
        }

        UtKeyStore ret = new UtKeyStore(f, pwd);

        //Test via "list".  Will throw exception if problem
        ret.listAliases();

        return ret;
    }



    /**
     * Returns null if not found
     */
    private static UtKSEntryType getEntryTypeImpl(KeyStore ks,
                                                  String alias) throws GeneralSecurityException {
        if(ks.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
            return UtKSEntryType.PrivKey;
        }
        else if(ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            return UtKSEntryType.SecKey;
        }
        else if(ks.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
            return UtKSEntryType.TrustedCert;
        }
        return null;
    }



    private static void saveKeystore(KeyStore ks, File outFile, String pwd)
        throws GeneralSecurityException, IOException {

        FileOutputStream fOut = null;

        try {
            fOut = new FileOutputStream(outFile, false);
            ks.store(fOut, pwd.toCharArray());
            fOut.flush();
            fOut.close();
        }
        catch(GeneralSecurityException ex) {
            IOUtil.close(fOut);
            throw ex;
        }
        catch(IOException ex) {
            IOUtil.close(fOut);
            throw ex;
        }

    }

    /**
     * Opens and loads a key store.  Used when programatically
     * manipulating the key store
     */
    private KeyStore openKeyJavaStore()
        throws GeneralSecurityException, IOException {

        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = null;
        try {
            fis = new java.io.FileInputStream(m_ktFile);
            ks.load(fis, m_ktPass.toCharArray());
            fis.close();
            return ks;
        }
        catch(IOException ex) {
            IOUtil.close(fis);
            throw ex;
        }
        catch(GeneralSecurityException ex) {
            IOUtil.close(fis);
            throw ex;
        }
    }
}

