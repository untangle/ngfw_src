/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.directory_connector;

import java.util.Objects;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeRadiusManagerImpl implements SafeRadiusManager
{
    private final RadiusManagerImpl delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeRadiusManagerImpl(RadiusManagerImpl delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getRadiusStatusForSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     * @param username The username argument
     * @param password The password argument
     * @return The delegated result
     */
    @Override
    public String getRadiusStatusForSettings(DirectoryConnectorSettings newSettings, String username, String password)
    {
        return delegate.getRadiusStatusForSettings(newSettings, username, password);
    }
}
