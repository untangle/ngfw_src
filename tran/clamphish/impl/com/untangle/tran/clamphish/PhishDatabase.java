/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.tran.clamphish;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;

class PhishDataBase implements DomainDatabase
{
    private static final byte[] DB_SALT = "oU3q.72p".getBytes();

    private static final DomainDatabase NULL_DB = new NullDatabase();

    private final Database db;
    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    private PhishDataBase() throws DatabaseException
    {
        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        File dbHome = new File(System.getProperty("bunnicula.db.dir"));
        try {
            Environment dbEnv = new Environment(dbHome, envCfg);

            // Open the database. Create it if it does not already exist.
            DatabaseConfig dbCfg = new DatabaseConfig();
            dbCfg.setAllowCreate(true);
            db = dbEnv.openDatabase(null, "goog-black-url.db", dbCfg);
        } catch (DatabaseException exn) {
            logger.warn("could not open phish database", exn);
            throw exn;
        }
    }

    // factories --------------------------------------------------------------

    static DomainDatabase getDatabase()
    {
        DomainDatabase pdb;

        try {
            pdb = new PhishDataBase();
        } catch (DatabaseException exn) {
            pdb = NULL_DB;
        }

        return pdb;
    }

    // DomainDatabase methods -------------------------------------------------

    public void getPatterns(String hostStr)
    {
        byte[] host = hostStr.getBytes();

        byte[] in = new byte[DB_SALT.length + host.length];
        System.arraycopy(DB_SALT, 0, in, 0, DB_SALT.length);
        System.arraycopy(host, 0, in, DB_SALT.length, host.length);

        // XXX Switch to Fast MD5 http://www.twmacinta.com/myjava/fast_md5.php
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exn) {
            logger.warn("Could not get MD5 algorithm", exn);
            return;
        }

        byte[] hash = md.digest(in);

        DatabaseEntry k = new DatabaseEntry(host);
        DatabaseEntry v = new DatabaseEntry();

        // XXX hopefully we can just use READ_UNCOMMITTED
        OperationStatus status;
        try {
            status = db.get(null, k, v, LockMode.DEFAULT);
        } catch (DatabaseException exn) {
            logger.warn("could not access database", exn);
            return;
        }

        if (OperationStatus.SUCCESS == status) {
            byte[] data = v.getData();
            in = new byte[8 + DB_SALT.length + host.length];
            System.arraycopy(data, 0, in, 0, 8);
            System.arraycopy(DB_SALT, 0, in, 8, DB_SALT.length);
            System.arraycopy(host, 0, in, 8 + DB_SALT.length, host.length);
            md.reset();
            hash = md.digest(in);

            Cipher rc4;
            try {
                rc4 = Cipher.getInstance("RC4");
                Key key = new SecretKeySpec(hash, "RC4");
                rc4.init(Cipher.DECRYPT_MODE, key);
            } catch (GeneralSecurityException exn) {
                logger.warn("could not get RC4 algorithm", exn);
                return;
            }

            System.out.println("UPDATE");
            byte[] output = rc4.update(data, 8, data.length);
            System.out.println("OUTPUT: " + new String(output));
        } else {
            System.out.println("HOST NOT FOUND: " + hostStr);
        }
    }

    // private classes --------------------------------------------------------

    private static class NullDatabase implements DomainDatabase
    {
        NullDatabase() { }

        // DomainDatabase methods ---------------------------------------------

        public void getPatterns(String host) { System.out.println("NULL"); }
    }

    public static void main(String[] args) throws Exception
    {
        DomainDatabase pdb = PhishDataBase.getDatabase();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(pdb + " READY!");
        String line;
        System.out.print("Host: ");
        while (null != (line = br.readLine())) {
            System.out.print("Host: ");
            pdb.getPatterns(line);
        }
    }
}
