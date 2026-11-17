/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.smtp;

import java.util.List;
import java.util.Objects;

import com.untangle.app.smtp.quarantine.InboxIndex;
import com.untangle.app.smtp.quarantine.InboxRecord;
import com.untangle.app.smtp.quarantine.NoSuchInboxException;
import com.untangle.app.smtp.quarantine.QuarantineMaintenenceView;
import com.untangle.app.smtp.quarantine.QuarantineUserActionFailedException;
import com.untangle.app.smtp.quarantine.SafeQuarantineMaintenenceView;
import com.untangle.app.smtp.quarantine.store.InboxSummary;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeQuarantineMaintenenceViewImpl implements SafeQuarantineMaintenenceView
{
    private final QuarantineMaintenenceView delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeQuarantineMaintenenceViewImpl(QuarantineMaintenenceView delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * deleteInboxes through the reviewed RPC contract.
     * @param accounts The accounts argument
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public void deleteInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        delegate.deleteInboxes(accounts);
    }

    /**
     * getInboxRecords through the reviewed RPC contract.
     * @param account The account argument
     * @return The delegated result
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public List<InboxRecord> getInboxRecords(String account) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        return delegate.getInboxRecords(account);
    }

    /**
     * getInboxesTotalSize through the reviewed RPC contract.
     * @return The delegated result
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public long getInboxesTotalSize() throws QuarantineUserActionFailedException
    {
        return delegate.getInboxesTotalSize();
    }

    /**
     * listInboxes through the reviewed RPC contract.
     * @return The delegated result
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public List<InboxSummary> listInboxes() throws QuarantineUserActionFailedException
    {
        return delegate.listInboxes();
    }

    /**
     * purge through the reviewed RPC contract.
     * @param account The account argument
     * @param doomedMails The doomedMails argument
     * @return The delegated result
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public InboxIndex purge(String account, String... doomedMails) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        return delegate.purge(account, doomedMails);
    }

    /**
     * rescue through the reviewed RPC contract.
     * @param account The account argument
     * @param rescuedMails The rescuedMails argument
     * @return The delegated result
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public InboxIndex rescue(String account, String... rescuedMails) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        return delegate.rescue(account, rescuedMails);
    }

    /**
     * rescueInboxes through the reviewed RPC contract.
     * @param accounts The accounts argument
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    @Override
    public void rescueInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        delegate.rescueInboxes(accounts);
    }
}
