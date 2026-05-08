/**
 * $Id$
 */

package com.untangle.uvm.util;

/**
 * Thrown by {@link SafeCheckValidator} when a value annotated with
 * {@link SafeCheck} fails validation against the declared {@link SafeType}.
 *
 * <p>The exception message names the field and the format requirement,
 * but never includes the offending value itself. This avoids leaking
 * credentials and prevents pivot through error-channel echo when the
 * exception propagates back to the JSON-RPC client (Jabsorb wraps it
 * as a JSON-RPC error).</p>
 */
@SuppressWarnings("serial")
public class SafeCheckValidationException extends RuntimeException
{
    /**
     * Constructs a new SafeCheckValidationException with the supplied detail message.
     *
     * @param message the detail message describing the validation failure;
     *                must not include the offending value (see class Javadoc).
     */
    public SafeCheckValidationException(String message)
    {
        super(message);
    }
}
