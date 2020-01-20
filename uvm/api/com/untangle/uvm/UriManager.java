/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * the System Manager API
 */
public interface UriManager
{
    UriManagerSettings getSettings();

    void setSettings(UriManagerSettings settings);

    String getUri(String url);
}
