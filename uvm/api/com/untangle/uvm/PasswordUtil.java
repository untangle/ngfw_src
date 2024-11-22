/**
 * $Id$
 */
package com.untangle.uvm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.untangle.uvm.UvmContextFactory;
import org.apache.commons.lang3.StringUtils;
import java.security.SecureRandom;

/**
 * Password utility.
 */
public class PasswordUtil
{
    public static final String PASSWORD_HASH_ALGORITHM = "MD5";

    public static final int SALT_LENGTH = 8;

    // We pass no seed here, so we'll get different random numbers each time.
    private static SecureRandom srng = new SecureRandom();

    /**
     * Encrypt the provided password by invoking an password manager
     * command to retrieve the encrypted value.
     * 
     * @param password a <code>String</code> containing the password to be encrypt
     * @return a <code>String</code> containing the encrypt password
    */
    public static String getEncryptPassword(String password){
        String command = new StringBuilder("/usr/bin/password-manager -e ").append(password).toString();
        String cmdOutput =  UvmContextFactory.context().execManager().execOutput(command);
        String[] encryptPassword = cmdOutput.split("\n");
        if (encryptPassword.length <= 1) {
            throw new IllegalStateException("Decryption output is invalid.");
        }
        return encryotPassword[1];
    }
    /**
     * Decrypts the provided encrypted password by invoking an external password manager
     * command to retrieve the decrypted value.
     * @param encryptedPassword a <code>String</code> containing the encrypted password to be decrypted
     * @return a <code>String</code> containing the decrypted password
        */
    public static String getDecryptPassword(String encryptedPassword) {
        if (StringUtils.isBlank(encryptedPassword)) {
            throw new IllegalArgumentException("Encrypted password must not be null or empty.");
        }
        String command = "/usr/bin/password-manager -d " + encryptedPassword;
        String cmdOutput = UvmContextFactory.context().execManager().execOutput(command);
        String[] decryptPassword = cmdOutput.split("\n");
        if (decryptPassword.length <= 1) {
            throw new IllegalStateException("Decryption output is invalid.");
        }
        return decryptPassword[1];
    }
    
    /**
     * Use <code>encrypt</code> to encrypt a password (using a one-way hash
     * function) before storing into the User prefs app. We automatically
     * randomly salt the password before hashing.
     *
     * @param passwd a <code>String</code> giving the password to be encrypted
     * @return a <code>byte[]</code> value containing the hashed password
     */
    // This should possibly take a char[] instead to facilitate clearing? XXX
    public static byte[] encrypt(String passwd)
    {
        byte[] salt = new byte[SALT_LENGTH];
        srng.nextBytes(salt);
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException x) {
            throw new Error("Algorithm " + PASSWORD_HASH_ALGORITHM + " not available in Java VM");
        }
        d.reset();
        d.update(passwd.getBytes());
        d.update(salt);
        byte[] rawPW = d.digest();
        byte[] result = new byte[rawPW.length + salt.length];
        System.arraycopy(rawPW, 0, result, 0, rawPW.length);
        System.arraycopy(salt, 0, result, rawPW.length, SALT_LENGTH);
        return result;
    }

    /**
     * <code>check</code> checks to see if the given password encrypts
     * into the given hashed password.
     *
     * @param passwd a <code>String</code> giving the cleartext
     * password to be checked
     * @param hashedPasswd a <code>byte[]</code> giving the hashed
     * password to be checked against
     * @return a <code>boolean</code> true if they match, false
     * otherwise
     */
    public static boolean check(String passwd, byte[] hashedPasswd)
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
            throw new Error("Algorithm " + PASSWORD_HASH_ALGORITHM + " not available in Java VM");
        }
        d.reset();
        d.update(passwd.getBytes());
        d.update(salt);
        byte[] testRawPW = d.digest();
        if (rawPW.length != testRawPW.length)
            throw new IllegalArgumentException
                ("hashed password has incorrect length (got " + rawPW.length + " wanted " + testRawPW.length + ")");
        for (int i = 0; i < testRawPW.length; i++)
            if (testRawPW[i] != rawPW[i])
                return false;
        return true;
    }
}
