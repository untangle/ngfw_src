/**
 * $Id$
 */

package com.untangle.uvm;

/**
 * Test-only RPC bridge that exposes
 * {@link com.untangle.uvm.util.SafeCheckValidator} to remote callers (the
 * safecheck-sweep tooling under {@code tools/safecheck-sweep/}).
 *
 * <p>The method validates a single string value against a list of
 * {@link com.untangle.uvm.util.SafeType} names and an optional allow-list,
 * mirroring the semantics of {@link com.untangle.uvm.util.SafeCheck}
 * annotations. It is read-only and has no side effects.</p>
 */
public interface SafeCheckTool
{
    /**
     * Validate a single value against a list of SafeType names and an
     * optional allow-list.
     *
     * <ul>
     *   <li>Empty or null {@code safeTypeNames} falls back to
     *       {@link com.untangle.uvm.util.SafeType#SIMPLE_TEXT}
     *       (matching the validator's existing rule).</li>
     *   <li>{@code allow} short-circuit applied before the SafeType regex
     *       check (case-sensitive {@link String#equals}).</li>
     *   <li>Multi-type OR: accepts if any listed SafeType accepts.</li>
     *   <li>Unknown SafeType name is reported in the result; not thrown.</li>
     * </ul>
     *
     * <p>Parameter types are {@code String[]} (not {@code List<String>}) so
     * Jabsorb-over-JSON-RPC callers can pass bare JSON arrays without the
     * {@code {"javaClass": "java.util.ArrayList", "list": [...]}} wrapper
     * that {@code List<String>} would require on the wire.</p>
     *
     * @param value         the value being checked (null or empty is accepted by every SafeType)
     * @param safeTypeNames array of SafeType enum constant names (e.g. {@code ["HOSTNAME", "IP_OR_CIDR"]})
     * @param allow         optional literal allow-list; may be null or empty
     * @return {@code "OK"} if the value is accepted, {@code "REJECTED: <reason>"} otherwise
     */
    String validate(String value, String[] safeTypeNames, String[] allow);
}
