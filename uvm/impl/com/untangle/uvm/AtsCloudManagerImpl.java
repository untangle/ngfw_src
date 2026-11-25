package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsCloudManagerImpl implements AtsCloudManager
{
    private final SafeCloudManager safe;
    private final Supplier<CloudManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsCloudManagerImpl(SafeCloudManager safe, Supplier<CloudManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject accountCreate(@SafeCheckParam(SafeType.EMAIL) String email, @SafeCheckParam(SafeType.OPAQUE_SECRET) String password, @SafeCheckParam(SafeType.SIMPLE_TEXT) String firstName, @SafeCheckParam(SafeType.SIMPLE_TEXT) String lastName, @SafeCheckParam(SafeType.SIMPLE_TEXT) String companyName, @SafeCheckParam(SafeType.ALPHANUM) String uid, @SafeCheckParam(SafeType.SIMPLE_TEXT) String applianceModel, @SafeCheckParam(SafeType.ALPHANUM) String majorVersion, @SafeCheckParam(SafeType.ALPHANUM) String installType) throws Exception
    {
        return safe.accountCreate(email, password, firstName, lastName, companyName, uid, applianceModel, majorVersion, installType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject accountLogin(@SafeCheckParam(SafeType.EMAIL) String email, @SafeCheckParam(SafeType.OPAQUE_SECRET) String password, @SafeCheckParam(SafeType.ALPHANUM) String uid, @SafeCheckParam(SafeType.SIMPLE_TEXT) String applianceModel, @SafeCheckParam(SafeType.ALPHANUM) String majorVersion, @SafeCheckParam(SafeType.ALPHANUM) String installType) throws Exception
    {
        return safe.accountLogin(email, password, uid, applianceModel, majorVersion, installType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject accountLogin(@SafeCheckParam(SafeType.EMAIL) String login, @SafeCheckParam(SafeType.OPAQUE_SECRET) String password) throws Exception
    {
        return delegate.get().accountLogin(login, password);
    }
}
