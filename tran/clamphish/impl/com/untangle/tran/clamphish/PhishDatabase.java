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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

class PhishDataBase implements DomainDatabase
{
    private static final byte[] DB_SALT = "oU3q.72p".getBytes();
    private static final byte[] VERSION_KEY = "goog-black-enchash".getBytes();

    private static final DomainDatabase NULL_DB = new NullDatabase();

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[goog-black-enchash ([0-9.]+)\\]");
    private static final Pattern TUPLE_PATTERN = Pattern.compile("\\+([0-9A-F]+)\t([A-Za-z0-9+/=]+)");

    private final Database db;
    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    private PhishDataBase() throws DatabaseException
    {
        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        File dbHome = new File(System.getProperty("bunnicula.db.dir"));
        System.out.println("DB HOME: " + dbHome);
        try {
            Environment dbEnv = new Environment(dbHome, envCfg);

            // Open the database. Create it if it does not already exist.
            DatabaseConfig dbCfg = new DatabaseConfig();
            dbCfg.setAllowCreate(true);
            db = dbEnv.openDatabase(null, "google-black-enchash", dbCfg);

            try {
                loadDatabase();
                System.out.println("DATABASE COUNT: " + db.count());
            } catch (IOException exn) {
                logger.warn("could not initialize database", exn);
                System.out.println("could not initialize database");
            }
        } catch (DatabaseException exn) {
            logger.warn("could not open phish database", exn);
            System.out.println("could not open phish database");
            exn.printStackTrace();
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

    public void lookupHost(String hostStr)
    {
        for (String d = hostStr; null != d; d = nextHost(d)) {
            getPatterns(d);
        }
    }

    // private methods --------------------------------------------------------

    private void getPatterns(String hostStr)
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
            System.out.println("Could not get MD5 algorithm");
            return;
        }

        byte[] hash = md.digest(in);
        DatabaseEntry k = new DatabaseEntry(hash);
        DatabaseEntry v = new DatabaseEntry();

        // XXX hopefully we can just use READ_UNCOMMITTED
        OperationStatus status;
        try {
            status = db.get(null, k, v, LockMode.DEFAULT);
        } catch (DatabaseException exn) {
            logger.warn("could not access database", exn);
            System.out.println("could not access database");
            return;
        }
        System.out.println("STATUS: " + status);

        if (OperationStatus.SUCCESS == status) {
            byte[] data = v.getData();
            System.out.println("DATA: " + base64Encode(v.getData()));
            in = new byte[8 + DB_SALT.length + host.length];
            System.arraycopy(DB_SALT, 0, in, 0, DB_SALT.length);
            System.arraycopy(data, 0, in, DB_SALT.length, 8);
            System.arraycopy(host, 0, in, 8 + DB_SALT.length, host.length);
            System.out.println("KEY: " + new String(in));
            md.reset();
            hash = md.digest(in);

            Cipher arcfour;
            try {
                arcfour = Cipher.getInstance("ARCFOUR");
                Key key = new SecretKeySpec(hash, "ARCFOUR");
                arcfour.init(Cipher.DECRYPT_MODE, key);
            } catch (GeneralSecurityException exn) {
                logger.warn("could not get ARCFOUR algorithm", exn);
                System.out.println("could not get ARCFOUR algorithm");
                return;
            }

            System.out.println("UPDATE");

            try {
                System.out.println("DATA LENGTH: " + (data.length - 8));
                byte[] output = arcfour.doFinal(data, 8, data.length - 8);
                System.out.println("OUTPUT: " + new String(output));
            } catch (GeneralSecurityException exn) {
                logger.warn("could not decrypt regexp", exn);
                System.out.println("could not decrypt regexp");
                exn.printStackTrace();
            }
        } else {
            System.out.println("HOST NOT FOUND: " + hostStr);
        }
    }

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    private void loadDatabase() throws IOException
    {
        DatabaseEntry versionKey = new DatabaseEntry(VERSION_KEY);

        try {
            // XXX do an update if exists
            if (OperationStatus.SUCCESS == db.get(null, versionKey, new DatabaseEntry(), LockMode.DEFAULT)) {
                return;
            }
        } catch (DatabaseException exn) {
            logger.warn("could not get database version", exn);
        }

        URL url = new URL("http://sb.google.com/safebrowsing/update?version=goog-black-enchash:1:1");
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();

        String version;

        Matcher matcher = VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            version = matcher.group(1);
            try {
                db.put(null, versionKey, new DatabaseEntry(version.getBytes()));
            } catch (DatabaseException exn) {
                logger.warn("could not set database version", exn);
            }
        } else {
            logger.warn("No version number: " + line);
            System.out.println("No version number: " + line);
        }

        while (null != (line = br.readLine())) {
            matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                byte[] host = new BigInteger(matcher.group(1), 16).toByteArray();
                byte[] b64Data = matcher.group(2).getBytes();
                byte[] regexp = base64Decode(matcher.group(2));

                try {
                    db.put(null, new DatabaseEntry(host), new DatabaseEntry(regexp));
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }
    }

    private byte[] base64Decode(String s) {
        try {
            return new BASE64Decoder().decodeBuffer(s);
        } catch (IOException exn) {
            logger.warn("could not decode", exn);
            System.out.println("could not decode");
            return new byte[0];
        }
    }

    private String base64Encode(byte[] b) {
        return new BASE64Encoder().encodeBuffer(b);
    }


    // private classes --------------------------------------------------------

    private static class NullDatabase implements DomainDatabase
    {
        NullDatabase() { }

        // DomainDatabase methods ---------------------------------------------

        public void lookupHost(String host) { System.out.println("NULL"); }
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
            pdb.lookupHost(line);
        }
    }
}
