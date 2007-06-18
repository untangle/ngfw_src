/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * $Id$
 */

package com.untangle.uvm.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.untangle.uvm.security.UvmPrincipal;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.RealmBase;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;

class UvmRealm extends RealmBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String userQuery
        = "SELECT password, read_only FROM u_user WHERE login = ?";

    // XXX Very small memory leak here if the nonce is never used (quite rare)
    private HashMap<String, Principal> nonces = new HashMap<String, Principal>();

    // public methods ---------------------------------------------------------

    // Used by servlets (reports, store)
    public String generateAuthNonce(InetAddress clientAddr, Principal user) {
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException x) {
            throw new Error("Algorithm " + PASSWORD_HASH_ALGORITHM
                            + " not available in Java VM");
        }
        long currentTime = System.currentTimeMillis();
        String nonceValue = clientAddr + ":" +
            currentTime + ":MetavizeUvm94114";
        byte[] buffer = d.digest(nonceValue.getBytes());
        String nonce = new BASE64Encoder().encode(buffer);
        nonces.put(nonce, user);

        return UvmAuthenticator.AUTH_NONCE_FIELD_NAME + "="
            + URLEncoder.encode(nonce);
    }

    // Used by servlets (reports, store)
    public Principal authenticateWithNonce(String nonce)
    {
        Principal user = nonces.remove(nonce);
        if (logger.isDebugEnabled())
            logger.debug("Attempting to authenticate with nonce " + nonce + ", got user: " + user);
        return user;
    }

    // Realm methods ----------------------------------------------------------

    @Override
    public Principal authenticate(String username, String credentials)
    {
        DataSourceFactory dsf = DataSourceFactory.factory();

        boolean readOnly;

        Connection c = null;
        try {
            // XXX use pool
            c = dsf.getConnection();

            logger.debug("doing query: " + userQuery);
            PreparedStatement ps = c.prepareStatement(userQuery);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                logger.warn("no such user: " + username);
                return null;
            }

            byte[] hashedPasswd  = rs.getBytes("password");
            if (!check(credentials, hashedPasswd)) {
                logger.warn("bad password for user: " + username);
                return null;
            } else {
                readOnly = rs.getBoolean("read_only");
            }
        } catch (SQLException exn) {
            logger.warn("could not query domains", exn);
            return null;
        } finally {
            try {
                if (null != c) {
                    dsf.closeConnection(c);
                }
            } catch (SQLException exn) {
                logger.warn(exn);
            }
        }

        return new UvmPrincipal(username, readOnly);
    }

    @Override
    public boolean hasRole(Principal p, String role)
    {
        return null != role && role.equalsIgnoreCase("user")
            && p instanceof UvmPrincipal;
    }


    // RealmBase methods ------------------------------------------------------

    protected String getPassword(String username) { return null; }
    protected Principal getPrincipal(String username) { return null; }
    protected String getName() { return "UvmRealm"; }

    // private methods --------------------------------------------------------

    // XXX im too lazy , ill just paste from PasswdUtil
    private static final String PASSWORD_HASH_ALGORITHM = "MD5";
    private static final int SALT_LENGTH = 8;

    // XXX im too lazy , ill just paste from PasswdUtil
    private static boolean check(String passwd, byte[] hashedPasswd)
    {
        if (hashedPasswd.length - SALT_LENGTH < 1)
            throw new IllegalArgumentException("hashed passwd is too short");

        byte[] salt = new byte[SALT_LENGTH];
        byte[] rawPW = new byte[hashedPasswd.length - SALT_LENGTH];
        System.arraycopy(hashedPasswd, 0, rawPW, 0, rawPW.length);
        System.arraycopy(hashedPasswd, rawPW.length, salt, 0, SALT_LENGTH);
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException x) {
            throw new Error("Algorithm " + PASSWORD_HASH_ALGORITHM
                            + " not available in Java VM");
        }
        d.reset();
        d.update(passwd.getBytes());
        d.update(salt);
        byte[] testRawPW = d.digest();
        if (rawPW.length != testRawPW.length)
            throw new IllegalArgumentException
                ("hashed password has incorrect length");
        for (int i = 0; i < testRawPW.length; i++)
            if (testRawPW[i] != rawPW[i])
                return false;
        return true;
    }

    /**
     * Perform access control based on the specified authorization constraint.
     * Return <code>true</code> if this constraint is satisfied and processing
     * should continue, or <code>false</code> otherwise.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     * @param context The Context to which client of this class is attached.
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean hasResourcePermission(Request request,
                                         Response response,
                                         SecurityConstraint []constraints,
                                         Context context)
        throws IOException
    {
        // Have we already authenticated someone?
        Principal principal = request.getUserPrincipal();

        if ( logger.isDebugEnabled()) logger.debug( "Authenticating against [" + principal + "]" );

        if ( !isValidPrincipal( principal )) {
            if ( logger.isDebugEnabled()) {
                logger.debug("No UvmPrincipal, trying to find a principal from the session" );
            }

            org.apache.catalina.Session session = request.getSessionInternal(false);
            if (null != session) {
                principal = session.getPrincipal();
                if ( logger.isDebugEnabled()) {
                    logger.debug("Found principal[" + principal + "] from session: " +session );
                }
            }
        }

        /* Check once again */
        if ( isValidPrincipal( principal )) return true;

        /* Revert to the standard verification methods */
        return super.hasResourcePermission(request,response,constraints,context);
    }


    /* Should be moved to a util, used also by UvmAuthenticator */
    private boolean isValidPrincipal( Principal principal )
    {
        return ( null != principal && ( principal instanceof UvmPrincipal ));
    }
}
