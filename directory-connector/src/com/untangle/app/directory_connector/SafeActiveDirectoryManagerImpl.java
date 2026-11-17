/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.directory_connector;

import java.util.Objects;
import javax.naming.ServiceUnavailableException;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeActiveDirectoryManagerImpl implements SafeActiveDirectoryManager
{
    private final ActiveDirectoryManagerImpl delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeActiveDirectoryManagerImpl(ActiveDirectoryManagerImpl delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getStatusForSettings through the reviewed RPC contract.
     * @param testServerSettings The testServerSettings argument
     * @return The delegated result
     */
    @Override
    public JSONObject getStatusForSettings(ActiveDirectoryServer testServerSettings)
    {
        return delegate.getStatusForSettings(testServerSettings);
    }

    /**
     * getUsers through the reviewed RPC contract.
     * @param domain The domain argument
     * @return The delegated result
     * @throws ServiceUnavailableException If the operation fails
     */
    @Override
    public JSONArray getUsers(String domain) throws ServiceUnavailableException
    {
        return delegate.getUsers(domain);
    }
}
