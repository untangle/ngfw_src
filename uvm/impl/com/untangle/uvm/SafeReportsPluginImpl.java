/**
 * Reviewed normal CMD plugin contract.
 */
package com.untangle.uvm;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Preserves the current plugin instance and exact CMD overloads.
 */
public final class SafeReportsPluginImpl implements SafeReportsPlugin
{
    private final Plugin delegate;

    /**
     * Create the reviewed plugin facade.
     * @param delegate The current underlying plugin
     */
    SafeReportsPluginImpl(Plugin delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * ExecuteReportQuery preserving the existing CMD plugin contract.
     * @param argument0 The original plugin argument
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    @Override
    public ArrayList ExecuteReportQuery(String argument0) throws Exception
    {
        return (ArrayList) SafePluginInvocation.invoke(delegate, "ExecuteReportQuery", new Class<?>[] {String.class}, new Object[] {argument0});
    }

    /**
     * GetLastError preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    @Override
    public String GetLastError() throws Exception
    {
        return (String) SafePluginInvocation.invoke(delegate, "GetLastError", new Class<?>[] {}, new Object[] {});
    }
}
