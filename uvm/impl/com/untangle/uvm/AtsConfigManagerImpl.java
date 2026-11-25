package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsConfigManagerImpl implements AtsConfigManager
{
    private final SafeConfigManager safe;
    private final Supplier<ConfigManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsConfigManagerImpl(SafeConfigManager safe, Supplier<ConfigManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object doFactoryReset()
    {
        return safe.doFactoryReset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDomainName()
    {
        return delegate.get().getDomainName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getHostName()
    {
        return delegate.get().getHostName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object restoreSystemBackup(@SafeCheckParam(SafeType.FILE_PATH) String argFileName, @SafeCheckParam(SafeType.REGEX_PATTERN) String maintainRegex)
    {
        return delegate.get().restoreSystemBackup(argFileName, maintainRegex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object setDomainName(@SafeCheckParam(SafeType.HOSTNAME) String argName)
    {
        return delegate.get().setDomainName(argName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object setHostName(@SafeCheckParam(SafeType.HOSTNAME) String argName)
    {
        return delegate.get().setHostName(argName);
    }
}
