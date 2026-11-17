package com.untangle.app.reports;

/**
 * Internal construction entrypoints, separate from callable Reports facades.
 */
public final class SafeReportsManagerFactory
{
    private SafeReportsManagerFactory()
    {
    }

    static SafeReportsManager create(ReportsManager delegate)
    {
        return delegate == null ? null : new SafeReportsManagerImpl(implementation(delegate));
    }

    private static ReportsManagerImpl implementation(ReportsManager delegate)
    {
        if (!(delegate instanceof ReportsManagerImpl)) {
            throw new IllegalArgumentException("Reports facade requires the existing Reports implementation");
        }
        return (ReportsManagerImpl) delegate;
    }
}
