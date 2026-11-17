/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeLanguageManagerImpl implements SafeLanguageManager
{
    private final Supplier<LanguageManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeLanguageManagerImpl(Supplier<LanguageManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getLanguageSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public LanguageSettings getLanguageSettings()
    {
        return delegate.get().getLanguageSettings();
    }

    /**
     * getLanguagesList through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<LocaleInfo> getLanguagesList()
    {
        return delegate.get().getLanguagesList();
    }

    /**
     * getTranslations through the reviewed RPC contract.
     * @param module The module argument
     * @return The delegated result
     */
    @Override
    public Map<String, String> getTranslations(String module)
    {
        return delegate.get().getTranslations(module);
    }

    /**
     * setLanguageSettings through the reviewed RPC contract.
     * @param langSettings The langSettings argument
     */
    @Override
    public void setLanguageSettings(LanguageSettings langSettings)
    {
        delegate.get().setLanguageSettings(langSettings);
    }

    /**
     * synchronizeLanguage through the reviewed RPC contract.
     */
    @Override
    public void synchronizeLanguage()
    {
        delegate.get().synchronizeLanguage();
    }
}
