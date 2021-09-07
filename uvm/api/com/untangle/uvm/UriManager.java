/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * The URI Manager API
 */
public interface UriManager
{
    UriManagerSettings getSettings();

    void setSettings(UriManagerSettings settings);

    String getUri(String url);

    String getUriWithPath(String url);

    UriTranslation getUriTranslationByHost(String url);
}
