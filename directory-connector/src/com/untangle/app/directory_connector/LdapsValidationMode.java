/**
 * Directory Connector LDAPS validation-mode definitions.
 */
package com.untangle.app.directory_connector;

/** Phase 1 LDAPS modes. Unknown or missing modes never authorize Trust All. */
public enum LdapsValidationMode
{
    /** Grandfathered LDAPS behavior that does not authenticate the server certificate. */
    LEGACY_INSECURE,
    /** Certificate validation using the record's explicitly selected trust sources. */
    PKI;

    /**
     * Parse an exact mode name, rejecting missing or unrecognized values.
     *
     * @param value Required serialized mode name.
     * @return The recognized validation mode.
     */
    public static LdapsValidationMode parse(String value)
    {
        if (value == null) {
            throw new IllegalArgumentException("LDAPS certificate validation mode is required");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown LDAPS certificate validation mode");
        }
    }
}
