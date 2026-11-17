/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafePluginManagerImpl implements SafePluginManager
{
    private final Supplier<PluginManager> delegate;
    private Plugin commandCenterDelegate;
    private SafeCommandCenterPlugin commandCenterFacade;
    private Plugin reportsDelegate;
    private SafeReportsPlugin reportsFacade;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafePluginManagerImpl(Supplier<PluginManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getPlugin through the reviewed RPC contract.
     * @param className The className argument
     * @return The delegated result
     */
    @Override
    public synchronized SafePlugin getPlugin(String className)
    {
        Plugin current = delegate.get().getPlugin(className);
        if (current == null) {
            if ("com.untangle.uvm.CommandCenterPluginImpl".equals(className)) {
                commandCenterDelegate = null;
                commandCenterFacade = null;
            } else if ("com.untangle.uvm.ReportsPluginImpl".equals(className)) {
                reportsDelegate = null;
                reportsFacade = null;
            }
            return null;
        }
        if ("com.untangle.uvm.CommandCenterPluginImpl".equals(current.getClass().getName())) {
            if (current != commandCenterDelegate) {
                commandCenterFacade = new SafeCommandCenterPluginImpl(current);
                commandCenterDelegate = current;
            }
            return commandCenterFacade;
        }
        if ("com.untangle.uvm.ReportsPluginImpl".equals(current.getClass().getName())) {
            if (current != reportsDelegate) {
                reportsFacade = new SafeReportsPluginImpl(current);
                reportsDelegate = current;
            }
            return reportsFacade;
        }
        throw new IllegalArgumentException("No Safe facade for plugin " + current.getClass().getName());
    }

    /**
     * loadPlugins through the reviewed RPC contract.
     */
    @Override
    public synchronized void loadPlugins()
    {
        PluginManager manager = delegate.get();
        manager.loadPlugins();
        if (commandCenterDelegate != manager.getPlugin("com.untangle.uvm.CommandCenterPluginImpl")) {
            commandCenterDelegate = null;
            commandCenterFacade = null;
        }
        if (reportsDelegate != manager.getPlugin("com.untangle.uvm.ReportsPluginImpl")) {
            reportsDelegate = null;
            reportsFacade = null;
        }
    }
}
