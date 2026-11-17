/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeUriManager.
 */
public interface SafeUriManager
{
    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    UriManagerSettings getSettings();

    /**
     * getUri through the reviewed RPC contract.
     * @param url The url argument
     * @return The delegated result
     */
    String getUri(String url);

    /**
     * getUriTranslationByHost through the reviewed RPC contract.
     * @param url The url argument
     * @return The delegated result
     */
    UriTranslation getUriTranslationByHost(String url);

    /**
     * getUriWithPath through the reviewed RPC contract.
     * @param url The url argument
     * @return The delegated result
     */
    String getUriWithPath(String url);
}
