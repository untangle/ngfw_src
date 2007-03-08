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

package com.untangle.tran.util;

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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

public class EncryptedUrlList extends UrlList
{
    private static final byte[] DB_SALT = "oU3q.72p".getBytes();

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[[^ ]+ ([0-9.]+)\\]");
    private static final Pattern TUPLE_PATTERN = Pattern.compile("\\+([0-9A-F]+)\t([A-Za-z0-9+/=]+)");

    private final URL databaseUrl;

    private final Logger logger = Logger.getLogger(getClass());

    public EncryptedUrlList(File dbHome, String dbName, URL databaseUrl)
        throws DatabaseException, IOException
    {
        super(dbHome, dbName);

        this.databaseUrl = databaseUrl;
    }

    // UrlList methods --------------------------------------------------------

    protected String initDatabase(Database db)
        throws IOException
    {
        InputStream is = databaseUrl.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();

        String version;

        Matcher matcher = VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            version = matcher.group(1);
        } else {
            version = null;
            logger.warn("No version number: " + line);
        }

        while (null != (line = br.readLine())) {
            matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                byte[] host = new BigInteger(matcher.group(1), 16).toByteArray();
                byte[] b64Data = matcher.group(2).getBytes();
                byte[] regexp = base64Decode(matcher.group(2));

                try {
                    db.put(null, new DatabaseEntry(host),
                           new DatabaseEntry(regexp));
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }

        return version;
    }

    protected String updateDatabase(Database db, String version)
        throws IOException
    {
        // XXX implement proper updating
        Cursor c = null;
        try {
            DatabaseEntry k = new DatabaseEntry();
            DatabaseEntry v = new DatabaseEntry();

            c = db.openCursor(null, null);
            while (OperationStatus.SUCCESS == c.getNext(k, v, LockMode.DEFAULT)) {
                c.delete();
            }
        } catch (DatabaseException exn) {
            logger.warn("could not clear database");
        } finally {
            if (null != c) {
                try {
                    c.close();
                } catch (DatabaseException exn) {
                    logger.warn("could not close cursor", exn);
                }
            }
        }

        return initDatabase(db);
    }

    protected byte[] getKey(byte[] host)
    {
        byte[] in = new byte[DB_SALT.length + host.length];
        System.arraycopy(DB_SALT, 0, in, 0, DB_SALT.length);

        System.arraycopy(host, 0, in, DB_SALT.length, host.length);

        // XXX Switch to Fast MD5 http://www.twmacinta.com/myjava/fast_md5.php
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exn) {
            logger.warn("Could not get MD5 algorithm", exn);
            return null;
        }

        return md.digest(in);
    }

    protected List<String> getValues(byte[] host, byte[] data)
    {
        byte[] buf = new byte[8 + DB_SALT.length + host.length];
        System.arraycopy(DB_SALT, 0, buf, 0, DB_SALT.length);
        System.arraycopy(data, 0, buf, DB_SALT.length, 8);
        System.arraycopy(host, 0, buf, 8 + DB_SALT.length, host.length);

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exn) {
            logger.warn("Could not get MD5 algorithm", exn);
            return Collections.emptyList();
        }
        buf = md.digest(buf);

        Cipher arcfour;
        try {
            arcfour = Cipher.getInstance("ARCFOUR");
            Key key = new SecretKeySpec(buf, "ARCFOUR");
            arcfour.init(Cipher.DECRYPT_MODE, key);
        } catch (GeneralSecurityException exn) {
            logger.warn("could not get ARCFOUR algorithm", exn);
            return Collections.emptyList();
        }

        try {
            buf = arcfour.doFinal(data, 8, data.length - 8);
        } catch (GeneralSecurityException exn) {
            logger.warn("could not decrypt regexp", exn);
            return Collections.emptyList();
        }

        return split(buf);
    }

    protected boolean matches(String str, String pat)
    {
        return str.matches(pat);
    }

    // private methods --------------------------------------------------------

    private byte[] base64Decode(String s) {
        try {
            return new BASE64Decoder().decodeBuffer(s);
        } catch (IOException exn) {
            logger.warn("could not decode", exn);
            return new byte[0];
        }
    }
}
