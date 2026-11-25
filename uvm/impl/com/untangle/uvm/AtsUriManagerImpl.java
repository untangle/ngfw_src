package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsUriManagerImpl implements AtsUriManager
{
    private final SafeUriManager safe;
    private final Supplier<UriManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsUriManagerImpl(SafeUriManager safe, Supplier<UriManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UriManagerSettings getSettings()
    {
        return safe.getSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUri(String url)
    {
        return safe.getUri(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UriTranslation getUriTranslationByHost(String url)
    {
        return safe.getUriTranslationByHost(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUriWithPath(String url)
    {
        return safe.getUriWithPath(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettings(UriManagerSettings settings)
    {
        delegate.get().setSettings(settings);
    }
}
