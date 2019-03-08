/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;

/**
 * Http casing settings.
 *
 */
@SuppressWarnings("serial")
public class HttpSettings implements Serializable
{
    public static final int MIN_URI_LENGTH = 1024;
    public static final int MAX_URI_LENGTH = 4096;

    private boolean enabled = true;
    private boolean nonHttpBlocked = false;
    private boolean blockLongHeaders = false;
    private int maxUriLength = MAX_URI_LENGTH;
    private boolean blockLongUris = false;
    private boolean logReferer = true;
    
    /**
     * Create HttpSettings.
     */
    public HttpSettings() { }

    /**
     * Enabled status for casing.
     * @return isEnabled
     */
    public boolean isEnabled() { return enabled; }

    /**
     * setEnabled.
     * @param enabled
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Enables non-http traffic on port 80.
     * @return isNonHttpBlocked
     */
    public boolean isNonHttpBlocked() { return nonHttpBlocked; }

    /**
     * setNonHttpBlocked.
     * @param nonHttpBlocked
     */
    public void setNonHttpBlocked(boolean nonHttpBlocked) { this.nonHttpBlocked = nonHttpBlocked; }

    /**
     * Maximum allowable header length.
     * @return maxHeaderLength
     */
    public int getMaxHeaderLength() { return 8192; }

    /**
     * setMaxHeaderLength.
     * @param maxHeaderLength
     */
    public void setMaxHeaderLength(int maxHeaderLength) { return; }

    /**
     * Enable blocking of headers that exceed maxHeaderLength.
     * If not explicitly blocked the connection is treated as non-HTTP
     * and the behavior is determined by setNonHttpBlocked.
     * @return blockLongHeaders
     */
    public boolean getBlockLongHeaders() { return blockLongHeaders; }

    /**
     * setBlockLongHeaders.
     * @param blockLongHeaders
     */
    public void setBlockLongHeaders(boolean blockLongHeaders) { this.blockLongHeaders = blockLongHeaders; }

    /**
     * Maximum allowable URI length.
     * @return maxUriLength
     */
    public int getMaxUriLength() { return maxUriLength; }

    /**
     * Description for setMaxUriLength.
     * @param maxUriLength <doc>
     */
    public void setMaxUriLength(int maxUriLength)
    {
        if (MIN_URI_LENGTH > maxUriLength || MAX_URI_LENGTH < maxUriLength) {
            throw new IllegalArgumentException("out of bounds: " + maxUriLength);
        }
        this.maxUriLength = maxUriLength;
    }

    /**
     * Description for getBlockLongUris.
     * Enable blocking of URIs that exceed maxUriLength.
     *
     * If not explicitly blocked the connection is treated as non-HTTP
     * and the behavior is determined by setNonHttpBlocked.
     * @return blockLongUris
     */
    public boolean getBlockLongUris() { return blockLongUris; }

    /**
     * Description for setBlockLongUris.
     * @param blockLongUris
     */
    public void setBlockLongUris(boolean blockLongUris) { this.blockLongUris = blockLongUris; }

    /**
     * If true the referer header is logged in each HttpRequestEvent
     * @return logReferer
     */
    public boolean getLogReferer() { return this.logReferer; }

    /**
     * setLogReferer.
     * @param newValue
     */
    public void setLogReferer(boolean newValue) { this.logReferer = newValue; }
}
