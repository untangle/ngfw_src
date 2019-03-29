/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.uvm.util.Pair;

/**
 * Class responsible for wrapping/unwrapping Authentication Tokens. Not based on any strong crypto - just keeping it
 * hard for bad guys to do bad things.
 */
class AuthTokenManager
{
    enum DecryptOutcome
    {
        OK, NOT_A_TOKEN, MALFORMED_TOKEN
    };

    private final Logger m_logger = Logger.getLogger(AuthTokenManager.class);

    private static final String ALG = "Blowfish";
    private static final byte[] INNER_MAGIC = "eks".getBytes();
    private static final byte[] OUTER_MAGIC = "emma".getBytes();

    private SecretKeySpec m_key;

    /**
     * Set the key to be used
     * @param key Array of byte.
     */
    void setKey(byte[] key)
    {
        try {
            m_key = new SecretKeySpec(key, ALG);
        } catch (Exception ex) {
            m_logger.warn("Unable to create key", ex);
        }
    }

    /**
     * Create an authentication token for the given username. The returned token is a String, but may not be web-safe
     * (i.e. URLEncoding). <br>
     * <br>
     * If there is any problem, null us returned
     * 
     * @param username
     *            the username
     * 
     * @return the token
     */
    String createAuthToken(String username)
    {
        try {
            // Create the encrypted payload
            byte[] usernameBytes = username.getBytes();
            byte[] toEncrypt = new byte[usernameBytes.length + INNER_MAGIC.length];

            System.arraycopy(INNER_MAGIC, 0, toEncrypt, 0, INNER_MAGIC.length);
            System.arraycopy(usernameBytes, 0, toEncrypt, INNER_MAGIC.length, usernameBytes.length);

            // Encrypt
            Cipher c = Cipher.getInstance(ALG);
            c.init(Cipher.ENCRYPT_MODE, m_key);
            byte[] encrypted = c.doFinal(toEncrypt);

            // Put the known chars at the front, to detect a token
            byte[] toEncode = new byte[encrypted.length + OUTER_MAGIC.length];
            System.arraycopy(OUTER_MAGIC, 0, toEncode, 0, OUTER_MAGIC.length);
            System.arraycopy(encrypted, 0, toEncode, OUTER_MAGIC.length, encrypted.length);

            return new String(Base64.encodeBase64(toEncode));
        } catch (Exception ex) {
            m_logger.warn("Unable to create token", ex);
            return null;
        }
    }

    /**
     * Attempt to decrypt the auth token.
     * 
     * @param token
     *            the token
     * 
     * @return the outcome, where the String is only valid if the outcome is "OK"
     */
    Pair<DecryptOutcome, String> decryptAuthToken(String token)
    {
        try {
            // Decode
            byte[] decodedBytes = Base64.decodeBase64(token.getBytes());

            // Check for outer magic
            if (!matches(OUTER_MAGIC, decodedBytes, 0, OUTER_MAGIC.length)) {
                return new Pair<>(DecryptOutcome.NOT_A_TOKEN);
            }

            // Decrypt
            Cipher c = Cipher.getInstance(ALG);
            c.init(Cipher.DECRYPT_MODE, m_key);
            byte[] decrypted = c.doFinal(decodedBytes, OUTER_MAGIC.length, decodedBytes.length - OUTER_MAGIC.length);

            if (!matches(INNER_MAGIC, decrypted, 0, INNER_MAGIC.length)) {
                return new Pair<>(DecryptOutcome.MALFORMED_TOKEN);
            }

            return new Pair<>(DecryptOutcome.OK, new String(decrypted, INNER_MAGIC.length,
                    decrypted.length - INNER_MAGIC.length));

        } catch (Exception ex) {
            m_logger.warn("Unable to decrypt token", ex);
            return new Pair<>(DecryptOutcome.MALFORMED_TOKEN);
        }
    }

    /**
     * Determine if matches.
     * @param  pattern Array of byte to find.
     * @param  inspect Array of byte to search.
     * @param  start   Starting position.
     * @param  len     Length to search
     * @return         true of match found, false otherwise.
     */
    private boolean matches(byte[] pattern, byte[] inspect, int start, int len)
    {
        if (inspect == null || len < pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (inspect[start + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}
