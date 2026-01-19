/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Constants;

import org.apache.commons.lang3.StringUtils;
import java.security.SecureRandom;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
/**
 * Password utility.
 */
public class PasswordUtil
{
    public static final String PASSWORD_HASH_ALGORITHM = "MD5";

    public static final int SALT_LENGTH = 8;

    // We pass no seed here, so we'll get different random numbers each time.
    private static SecureRandom srng = new SecureRandom();

    private final static Logger logger = LogManager.getLogger(PasswordUtil.class);

    private final static String passwordManager = "/usr/bin/password-manager";
    private static final String EMPTY_PASSWORD_SENTINEL = "__EMPTY__";
    
    
    /**  ************************* NOTE *************************
     *  Currently binary is generating warn related to TPM including the password in second line
     *  so handled the code accordingly in getEncryptPassword() and getDecryptPassword()
     *  Also while executing the command password is shown in log, I  handled this by overriding rhe execOutput() 
     *  and sending the value as false so you will not see any cmd log for password encryption and decryption
     * 
     * To get it in log for debug purpose you can set the value as true
     * 
    */
    /**
     * Encrypt the provided password by invoking password manager
     * command to retrieve the encrypted value.
     * @param password a <code>String</code> containing the password to be encrypt
     * @return a <code>String</code> containing the encrypt password or null if an error occurs during encryption so caller need to handle this.
     * @throw IllegalArgumentException if the encrypted password is null or empty.
     * @throw IllegalStateException if the encryption output is invalid (e.g., the command output cannot be parsed correctly).
    */
    public static String getEncryptPassword(String password){
        try {
            if (password == null) {
                throw new IllegalArgumentException("password can not be null.");
            }
            //paasword encryption should work for empty string too as we allow empty passwords
            if(password.isEmpty()){
                //set empty string as EMPTY_PASSWORD_SENTINEL so process run sccessfully
                password = EMPTY_PASSWORD_SENTINEL;
            }
            return execCmdProcessBuilder(passwordManager, "-e", password);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Password can not be null or encryption output is invalid.", e);
        } 
        catch (Exception e) {
            logger.error("Exception occured while encrypting the password", e);
        }
        return null;
    }
    /**
     * Decrypts the provided encrypted password by invoking password manager
     * command to retrieve the decrypted value.
     * @param encryptedPassword a <code>String</code>  containing encrypted password string to be decrypted.
     * @return a <code>String</code> containing decrypted password, or null if an error occurs during decryption so caller need to handle this.
     * @throw IllegalArgumentException if the encrypted password is null or empty.
     * @throw IllegalStateException if the decryption output is invalid (e.g., the command output cannot be parsed correctly).
     */
    public static String getDecryptPassword(String encryptedPassword) {
        try {
            if (StringUtils.isBlank(encryptedPassword)) {
                throw new IllegalArgumentException("Encrypted password can not be null or empty.");
            }
            String decrypted =  execCmdProcessBuilder(passwordManager, "-d", encryptedPassword);
            //return empty string for EMPTY_PASSWORD_SENTINEL
            if (EMPTY_PASSWORD_SENTINEL.equals(decrypted)) {
                return "";
            }
            return decrypted;
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Encrypted password can not be null or empty, or decryption output is invalid. ", e);
        } catch (Exception e) {
            logger.error("Exception occured while decrypting the password ", e);
        }
        return null;       
    }
    /**
     * Executes the provided command to encrypt/decrypt the password and processes the output.
     * 
     * @param command The command to execute for password encryption/decryption.
     * @return The encrypted/decrypted password string extracted from the command output.
     * @throw IllegalStateException if the command output is invalid (does not contain expected output result).
     * @throws Exception * If an error occurs while starting or interacting with the process.     
    */
    private static String execCmdProcessBuilder(String... command) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(command);

        // Redirect stderrstdout
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("password-manager exited with code: " + exitCode);
            throw new IllegalStateException("password-manager failed");
        }
        String[] encryptOrDecryptPassword = output.toString().split(Constants.NEW_LINE);
        if (encryptOrDecryptPassword.length <= 1) {
            throw new IllegalStateException("Output is invalid.");
        }
        return encryptOrDecryptPassword[1];  
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

    /**
     * Password  exception
     */
    @SuppressWarnings("serial")
    public static class CryptoProcessException extends Exception
    {
        /**
         * Initialize instance of CryptoProcessException.
         * @param  message String of message.
         * @return         Instance of CryptoProcessException.
         */
        public CryptoProcessException(String message) {
            super(message);
        }

        /**
         * Initialize instance of CryptoProcessException.
         * @param  message String of message.
         * @param  cause Trowable of cause.
         * @return         Instance of CryptoProcessException.
         */
        public CryptoProcessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
