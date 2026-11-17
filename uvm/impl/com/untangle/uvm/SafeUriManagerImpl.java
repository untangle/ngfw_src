/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeUriManagerImpl implements SafeUriManager
{
    private final Supplier<UriManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeUriManagerImpl(Supplier<UriManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public UriManagerSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * getUri through the reviewed RPC contract.
     * @param url The url argument
     * @return The delegated result
     */
    @Override
    public String getUri(String url)
    {
        return delegate.get().getUri(url);
    }

    /**
     * getUriTranslationByHost through the reviewed RPC contract.
     * @param url The url argument
     * @return The delegated result
     */
    @Override
    public UriTranslation getUriTranslationByHost(String url)
    {
        return delegate.get().getUriTranslationByHost(url);
    }

    /**
     * getUriWithPath through the reviewed RPC contract.
     * @param url The url argument
     * @return The delegated result
     */
    @Override
    public String getUriWithPath(String url)
    {
        return delegate.get().getUriWithPath(url);
    }
}
