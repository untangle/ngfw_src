/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.smtp.quarantine;

import java.util.List;

import com.untangle.app.smtp.quarantine.store.InboxSummary;

/**
 * Reviewed callable operations for SafeQuarantineMaintenenceView.
 */
public interface SafeQuarantineMaintenenceView
{
    /**
     * deleteInboxes through the reviewed RPC contract.
     * @param accounts The accounts argument
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    void deleteInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * getInboxRecords through the reviewed RPC contract.
     * @param account The account argument
     * @return The delegated result
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    List<InboxRecord> getInboxRecords(String account) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * getInboxesTotalSize through the reviewed RPC contract.
     * @return The delegated result
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    long getInboxesTotalSize() throws QuarantineUserActionFailedException;

    /**
     * listInboxes through the reviewed RPC contract.
     * @return The delegated result
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    List<InboxSummary> listInboxes() throws QuarantineUserActionFailedException;

    /**
     * purge through the reviewed RPC contract.
     * @param account The account argument
     * @param doomedMails The doomedMails argument
     * @return The delegated result
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    InboxIndex purge(String account, String... doomedMails) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * rescue through the reviewed RPC contract.
     * @param account The account argument
     * @param rescuedMails The rescuedMails argument
     * @return The delegated result
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    InboxIndex rescue(String account, String... rescuedMails) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * rescueInboxes through the reviewed RPC contract.
     * @param accounts The accounts argument
     * @throws NoSuchInboxException If the operation fails
     * @throws QuarantineUserActionFailedException If the operation fails
     */
    void rescueInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException;
}
