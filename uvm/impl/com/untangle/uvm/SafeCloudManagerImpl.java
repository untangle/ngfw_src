/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeCloudManagerImpl implements SafeCloudManager
{
    private final Supplier<CloudManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeCloudManagerImpl(Supplier<CloudManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * accountCreate through the reviewed RPC contract.
     * @param email The email argument
     * @param password The password argument
     * @param firstName The firstName argument
     * @param lastName The lastName argument
     * @param companyName The companyName argument
     * @param uid The uid argument
     * @param applianceModel The applianceModel argument
     * @param majorVersion The majorVersion argument
     * @param installType The installType argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    @Override
    public JSONObject accountCreate(@SafeCheckParam(SafeType.EMAIL) String email, @SafeCheckParam(SafeType.OPAQUE_SECRET) String password, @SafeCheckParam(SafeType.SIMPLE_TEXT) String firstName, @SafeCheckParam(SafeType.SIMPLE_TEXT) String lastName, @SafeCheckParam(SafeType.SIMPLE_TEXT) String companyName, @SafeCheckParam(SafeType.ALPHANUM) String uid, @SafeCheckParam(SafeType.SIMPLE_TEXT) String applianceModel, @SafeCheckParam(SafeType.ALPHANUM) String majorVersion, @SafeCheckParam(SafeType.ALPHANUM) String installType) throws Exception
    {
        return delegate.get().accountCreate(email, password, firstName, lastName, companyName, uid, applianceModel, majorVersion, installType);
    }

    /**
     * accountLogin through the reviewed RPC contract.
     * @param email The email argument
     * @param password The password argument
     * @param uid The uid argument
     * @param applianceModel The applianceModel argument
     * @param majorVersion The majorVersion argument
     * @param installType The installType argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    @Override
    public JSONObject accountLogin(@SafeCheckParam(SafeType.EMAIL) String email, @SafeCheckParam(SafeType.OPAQUE_SECRET) String password, @SafeCheckParam(SafeType.ALPHANUM) String uid, @SafeCheckParam(SafeType.SIMPLE_TEXT) String applianceModel, @SafeCheckParam(SafeType.ALPHANUM) String majorVersion, @SafeCheckParam(SafeType.ALPHANUM) String installType) throws Exception
    {
        return delegate.get().accountLogin(email, password, uid, applianceModel, majorVersion, installType);
    }
}
