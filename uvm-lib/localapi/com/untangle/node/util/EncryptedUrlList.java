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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

/**
 * <code>UrlList</code> that holds entries that are encoded as
 * described in:
 * {@link http://wiki.mozilla.org/Phishing_Protection:_Server_Spec}.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class EncryptedUrlList extends UrlList
{
    private static final byte[] DB_SALT = "oU3q.72p".getBytes();

    private static final Pattern TUPLE_PATTERN = Pattern.compile("([+-])([0-9A-F]+)\t([A-Za-z0-9+/=]+)?");

    private final Logger logger = Logger.getLogger(getClass());

    public EncryptedUrlList(File dbHome, URL databaseUrl, String dbName)
        throws DatabaseException, IOException
    {
        super(dbHome, databaseUrl, dbName, null, null);
    }

    public EncryptedUrlList(File dbHome, URL databaseUrl, String dbName,
                            Map<String, String> extraParams, File initFile)
        throws DatabaseException, IOException
    {
        super(dbHome, databaseUrl, dbName, extraParams, initFile);
    }

    // UrlList methods --------------------------------------------------------

    protected boolean updateDatabase(Database db, BufferedReader br)
        throws IOException
    {
        boolean blankLine = false;

        String line;
        while (null != (line = br.readLine())) {
            blankLine = line.trim().equals("");

            Matcher matcher = TUPLE_PATTERN.matcher(line);
            if (matcher.find()) {
                boolean add = matcher.group(1).equals("+");
                byte[] host = new BigInteger(matcher.group(2), 16).toByteArray();

                try {
                    if (add) {
                        byte[] regexp = base64Decode(matcher.group(3));
                        db.put(null, new DatabaseEntry(host),
                               new DatabaseEntry(regexp));
                    } else {
                        db.delete(null, new DatabaseEntry(host));
                    }
                } catch (DatabaseException exn) {
                    logger.warn("could not add database entry", exn);
                }
            }
        }

        return blankLine;
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
        try {
            return str.matches(pat);
        } catch (PatternSyntaxException exn) {
            logger.warn("bad pattern: " + exn.getMessage());
            return false;
        }
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
