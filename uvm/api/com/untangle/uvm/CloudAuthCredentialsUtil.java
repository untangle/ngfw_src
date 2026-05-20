/**
 * $Id$
 */
package com.untangle.uvm;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Reads the NGFW cloud auth request token from the credentials file shipped
 * by the untangle-cloud-auth-credentials package. The file holds the token
 * encrypted; this util shells out to the helper to extract the field, then
 * decrypts via PasswordUtil. Result is cached for the JVM lifetime.
 */
public class CloudAuthCredentialsUtil
{
    private static final String CLOUD_AUTH_PATH = "/var/lib/untangle-cloud-auth/";
    private static final String UVM_BIN_DIR = "uvm.bin.dir";

    private static final Logger logger = LogManager.getLogger(CloudAuthCredentialsUtil.class);

    private static volatile String cachedAuthRequestToken = null;

    /**
     * Gets the NGFW cloud auth request token, reading and decrypting from the credentials file if not already cached.
     * @return The decrypted cloud auth request token.
     * @throws PasswordUtil.CryptoProcessException If there is an error reading or decrypting the token.
     */
    public static String getAuthRequestToken() throws PasswordUtil.CryptoProcessException
    {
        String token = cachedAuthRequestToken;
        if (token != null) {
            return token;
        }
        synchronized (CloudAuthCredentialsUtil.class) {
            if (cachedAuthRequestToken != null) {
                return cachedAuthRequestToken;
            }
            String helper = System.getProperty(UVM_BIN_DIR) + "/ut-cloud-auth-helper.sh";
            String encrypted = UvmContextFactory.context().execManager().execOutput(helper + " encryptedAuthRequest " + CLOUD_AUTH_PATH);
            if (StringUtils.isBlank(encrypted)) {
                throw new PasswordUtil.CryptoProcessException("Failed to read encrypted cloud auth token from " + CLOUD_AUTH_PATH);
            }
            String decrypted = PasswordUtil.getDecryptPassword(encrypted.trim());
            if (decrypted == null) {
                throw new PasswordUtil.CryptoProcessException("Failed to decrypt cloud auth token");
            }
            cachedAuthRequestToken = decrypted;
            return cachedAuthRequestToken;
        }
    }
}
