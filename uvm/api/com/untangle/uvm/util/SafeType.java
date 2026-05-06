/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Typed allowlists used by {@link SafeCheck} to validate string field
 * values at the JSON-RPC boundary.
 *
 * <p>Each enum value carries (a) a validation rule and (b) a default
 * error message describing the format requirement. Rules are
 * <b>allowlist-based</b> - the value must match the type's pattern,
 * not merely lack particular metacharacters.</p>
 *
 * <h3>Null / empty policy</h3>
 * Every type accepts {@code null} and the empty string {@code ""} as
 * valid. Most settings fields are nullable by design (unset password,
 * unconfigured WAN test, etc.), and many ship with empty-string
 * defaults. Rejecting those would break round-trips on any settings
 * panel that has not been touched yet.
 *
 * <h3>Leading-character rule</h3>
 * Every regex-based type requires the first character to be
 * {@code [A-Za-z0-9]}. This closes the argv-option-injection class -
 * a value beginning with {@code -} would otherwise be parsed as a flag
 * by downstream CLI tools.
 *
 * <h3>OPAQUE_SECRET contract</h3>
 * {@link #OPAQUE_SECRET} permits any printable Unicode (excluding
 * control characters) so that legitimate passwords containing
 * {@code $}, {@code &}, {@code "} etc. are not rejected. Fields of
 * this type <b>must never</b> be passed to a string-concatenation
 * exec sink or naive single/double-quote-wrapped into a shell
 * template - every sink is responsible for using
 * {@code execCommand(...)} (Java) or {@code shlex.quote(...)} (Python)
 * around the value.
 */
public enum SafeType
{
    /**
     * Hostname / DNS label. Length 1-253, leading alphanumeric.
     * Allows '_' for parity with the existing UI vtype
     * (uvm/servlets/admin/app/overrides/form/field/VTypes.js:23 accepts
     * {@code [a-zA-Z0-9\-_.]+}) and to support Windows / NetBIOS-style
     * hostnames such as {@code SERVER_01} that have always been accepted
     * by the legacy validator. Non-ASCII is intentionally not allowed -
     * neither the UI vtype nor the legacy {@code hostnamePat} in
     * {@code NetworkManagerImpl.TroubleshootingValidator} accept it.
     */
    HOSTNAME(
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$"),
        253,
        "must be a valid hostname (alphanumeric, '.', '_', '-'; first char alphanumeric; max 253)"),

    /**
     * Linux interface name. Length 1-32 - matches the existing
     * {@code INTERFACE_NAME_PATTERN} in {@code NetworkManagerImpl}
     * (Linux kernel IFNAMSIZ is 16 but the codebase already accepts up
     * to 32, so we match it for backwards compatibility).
     * Allows ':' and '.' for aliases/VLANs ('eth0:0', 'eth0.1', 'br.eth0-1').
     * Leading character must be alphanumeric - closes the argv-option-injection
     * class (a value like '-rf' would otherwise be parsed as a flag by
     * downstream CLI tools; the kernel itself does not allow leading '-' either).
     */
    INTERFACE(
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]*$"),
        32,
        "must be a valid interface name (alphanumeric, '.', '_', ':', '-'; first char alphanumeric; max 32)"),

    /**
     * Single-segment filename. Excludes '/' and '..'. The validator further
     * rejects any value containing '..' as a substring or any '/' character.
     * Path canonicalization at the sink is still required for TOCTOU safety.
     */
    FILENAME(
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$"),
        255,
        "must be a valid filename (alphanumeric, '.', '_', '-'; no '..' or '/'; max 255)"),

    /**
     * Alphanumeric token with '_', '.', '-'. Length 1-64.
     *
     * <p>Widened from the original {@code [A-Za-z0-9_]+} to support
     * real-world SNMP community strings that commonly include '-' or
     * '.' (e.g. {@code "net-monitoring"}, {@code "site.east"}). The
     * leading-character rule restricts the first char to
     * {@code [A-Za-z0-9_]} - this keeps backwards compatibility with
     * the legacy underscore-leading tokens AND blocks the
     * argv-option-injection class (a value like {@code -rf} cannot pass
     * because '-' is forbidden at position 0; a value like {@code .conf}
     * is also blocked because '.' would be interpreted as an OID prefix
     * in some snmpd directives).</p>
     *
     * <p>The remaining allowed characters ('-', '.', '_') are inert in
     * every current sink: snmpd.conf is whitespace/newline-delimited,
     * argv tokens are space-delimited, and none of these characters are
     * shell metacharacters. Spaces, newlines, and the shell-meta set
     * ({@code $ & ` ; | < > " ' \ * ?}) remain rejected.</p>
     */
    ALPHANUM(
        Pattern.compile("^[A-Za-z0-9_][A-Za-z0-9._-]*$"),
        64,
        "must be alphanumeric (letters, digits, '_', '.', '-'; first char alphanumeric or '_'; max 64)"),

    /**
     * Permissive text for descriptions, contact strings, system locations.
     * Allows Unicode letters, digits, whitespace, and the safe punctuation
     * set [. _ - @ : , / + ( )]. Excludes shell metacharacters
     * ({@code $ & ` ; | < > \n \r " '}).
     */
    SIMPLE_TEXT(
        Pattern.compile("^[\\p{L}\\p{N}\\p{Zs}._@:,/+()-]{1,256}$"),
        256,
        "must contain only letters, digits, spaces, and the safe punctuation . _ - @ : , / + ( ) (max 256)"),

    /**
     * IPv4/IPv6 literal address (no DNS lookup) with optional CIDR prefix.
     * Validated with pure-Java regexes - no third-party dependency. See
     * {@link #IPV4_PATTERN} and {@link #IPV6_PATTERN}.
     */
    IP_OR_CIDR(
        null, // validated programmatically, see validate()
        49,   // longest IPv6 (39) + '/' + 3-digit prefix; 6 chars headroom
        "must be a valid IPv4/IPv6 address or CIDR (no hostnames; e.g. 10.0.0.1 or 192.168.1.0/24)"),

    /**
     * List of {@link #IP_OR_CIDR} entries separated by commas, newlines,
     * or both. Used for multi-CIDR fields like
     * {@code IpsecVpnNetwork.remoteNetworks} (newline-separated in UI
     * textarea) and {@code IpsecVpnTunnel.leftSubnet}/{@code rightSubnet}
     * (comma-separated, natively supported by strongswan's
     * {@code leftsubnet=} directive).
     *
     * <p><b>Whitespace handling:</b> each entry is trimmed before
     * validation; blank entries are skipped. Accepts LF, CRLF, comma,
     * or any combination as separators.</p>
     *
     * <p><b>Limits:</b> total string length lessthan equals 8192 chars; max 256
     * entries - both well above the size of any realistic VPN config
     * but bounded to avoid pathological input.</p>
     */
    IP_OR_CIDR_LIST(
        null,  // validated programmatically, see validate()
        8192,
        "must be a comma- or newline-separated list of valid IPv4/IPv6 addresses or CIDRs (max 256 entries)"),

    /** http(s) URL with required host and no embedded credentials. */
    URL(
        null, // validated programmatically, see validate()
        2048,
        "must be a valid http(s) URL with a host and no embedded credentials"),

    /**
     * Base64-encoded 32-byte key shape (e.g. WireGuard public/private keys).
     * SHAPE-ONLY: this validates only the regex, NOT that the decoded
     * payload is exactly 32 bytes. Sinks that need the strict 32-byte
     * guarantee must additionally call their domain-specific check
     * (e.g. {@code WireGuardVpnManager.isValidWGKey}).
     */
    BASE64_KEY(
        Pattern.compile("^[A-Za-z0-9+/]{43}=$"),
        44,
        "must be a 44-character base64-encoded key"),

    /**
     * Free-form secret. Accepts any printable Unicode including shell
     * metacharacters; rejects only control characters. See class
     * Javadoc for the sink-side handling contract.
     */
    OPAQUE_SECRET(
        null, // validated programmatically, see validate()
        256,
        "must be 1-256 characters; no control characters allowed");

    /** Strict IPv4 dotted-quad: each octet 0-255, no leading zeros beyond a single 0. */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}" +
        "(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$");

    /**
     * IPv6 literal. Covers full form (8 groups), zero-compressed form (::),
     * and IPv4-mapped tail (e.g. ::ffff:1.2.3.4). Excludes scope IDs (%eth0)
     * and IPv6-in-brackets - both legal forms are not used in our settings.
     */
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^(" +
            // full 8-group form
            "([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}" +
            "|" +
            // :: at start (e.g. ::1, ::ffff:0:0)
            "::([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,6})?" +
            "|" +
            // :: in middle (e.g. fe80::1, 2001:db8::1)
            "([0-9A-Fa-f]{1,4}:){1,6}(:[0-9A-Fa-f]{1,4}){1,6}" +
            "|" +
            // :: at end (e.g. 2001:db8::)
            "([0-9A-Fa-f]{1,4}:){1,7}:" +
            "|" +
            // IPv4-mapped tail in zero-compressed form (e.g. ::ffff:192.168.1.1)
            "::([0-9A-Fa-f]{1,4}:){0,5}" +
                "(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}" +
                "(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])" +
            "|" +
            // IPv4-mapped tail with explicit groups (e.g. 0:0:0:0:0:ffff:1.2.3.4)
            "([0-9A-Fa-f]{1,4}:){6}" +
                "(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}" +
                "(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])" +
        ")$");

    private final Pattern pattern;
    private final int maxLength;
    private final String defaultMessage;

    /**
     * Enum constructor.
     *
     * @param pattern        compiled regex used by {@link #validate(String)} for
     *                       regex-based types; {@code null} for types validated
     *                       programmatically (IP_OR_CIDR, IP_OR_CIDR_LIST, URL,
     *                       OPAQUE_SECRET).
     * @param maxLength      maximum permitted length of an accepted value.
     * @param defaultMessage human-readable description of the format requirement,
     *                       used when an annotation does not supply its own
     *                       error message.
     */
    SafeType(Pattern pattern, int maxLength, String defaultMessage)
    {
        this.pattern = pattern;
        this.maxLength = maxLength;
        this.defaultMessage = defaultMessage;
    }

    /**
     * @return the per-type human-readable error message used when an
     *         annotation does not supply its own {@code errorMessage()}.
     */
    public String defaultMessage()
    {
        return defaultMessage;
    }

    /**
     * Validates a value against this type.
     *
     * <p><b>Null and empty policy:</b> {@code null} and {@code ""} always
     * return {@code true}. Defaults across the codebase frequently use
     * empty string for optional fields; rejecting those would break
     * round-trips on unconfigured panels.</p>
     *
     * @param value the string to check (may be null / empty)
     * @return {@code true} if the value is acceptable for this type,
     *         {@code false} otherwise. Never throws - programmatic
     *         validators (IP_OR_CIDR, URL) catch their own parser
     *         exceptions and convert to {@code false}.
     */
    public boolean validate(String value)
    {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (value.length() > maxLength) {
            return false;
        }

        switch (this) {
            case IP_OR_CIDR:
                return validateIpOrCidr(value);
            case IP_OR_CIDR_LIST:
                return validateIpOrCidrList(value);
            case URL:
                return validateUrl(value);
            case OPAQUE_SECRET:
                return validateOpaqueSecret(value);
            case FILENAME:
                if (value.indexOf('/') >= 0 || value.contains("..")) {
                    return false;
                }
                return pattern.matcher(value).matches();
            default:
                return pattern.matcher(value).matches();
        }
    }

    /**
     * Comma- or newline-separated IP_OR_CIDR list. Splits on any
     * combination of commas, LF, and CRLF; trims each segment; skips
     * blank entries; requires every non-empty segment to validate as
     * IP_OR_CIDR. Bounded to 256 entries to prevent pathological input.
     *
     * @param value the candidate list; assumed non-null and non-empty.
     * @return {@code true} if every non-blank entry is a valid IP/CIDR and
     *         the entry count does not exceed 256; {@code false} otherwise.
     */
    private static boolean validateIpOrCidrList(String value)
    {
        // Split on any run of commas/newlines (handles "1.1.1.1,\n2.2.2.2"
        // and other mixed-separator forms).
        String[] entries = value.split("[,\\r\\n]+", -1);
        if (entries.length > 256) {
            return false;
        }
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!validateIpOrCidr(trimmed)) {
                return false;
            }
        }
        return true;
    }

    /**
     * IP literal (no DNS) optionally followed by a CIDR prefix length.
     * Pure regex - no third-party dependency.
     *
     * @param value the candidate IP or CIDR string; assumed non-null and non-empty.
     * @return {@code true} if {@code value} is a valid IPv4 or IPv6 literal,
     *         optionally followed by a {@code /prefix} of the appropriate
     *         per-family range; {@code false} otherwise.
     */
    private static boolean validateIpOrCidr(String value)
    {
        String addressPart = value;
        int slash = value.indexOf('/');
        if (slash >= 0) {
            addressPart = value.substring(0, slash);
            String prefixPart = value.substring(slash + 1);
            int prefix;
            try {
                prefix = Integer.parseInt(prefixPart);
            } catch (NumberFormatException e) {
                return false;
            }
            if (prefix < 0) {
                return false;
            }
            // Per-family prefix range check
            if (IPV4_PATTERN.matcher(addressPart).matches()) {
                if (prefix > 32) return false;
            } else if (IPV6_PATTERN.matcher(addressPart).matches()) {
                if (prefix > 128) return false;
            } else {
                return false;
            }
            return true;
        }
        return IPV4_PATTERN.matcher(addressPart).matches()
            || IPV6_PATTERN.matcher(addressPart).matches();
    }

    /**
     * http(s) URL with required host and no userinfo (no embedded credentials).
     *
     * @param value the candidate URL string; assumed non-null and non-empty.
     * @return {@code true} if {@code value} parses as a URI with an http or
     *         https scheme, a non-empty host, and no userinfo component;
     *         {@code false} on parse failure or any rule violation.
     */
    private static boolean validateUrl(String value)
    {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            scheme = scheme.toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return false;
            }
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                return false;
            }
            if (uri.getUserInfo() != null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Printable Unicode; reject any Cc-class control character.
     *
     * @param value the candidate secret string; assumed non-null and non-empty.
     * @return {@code true} if {@code value} contains no control characters;
     *         {@code false} otherwise.
     */
    private static boolean validateOpaqueSecret(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.getType(c) == Character.CONTROL) {
                return false;
            }
        }
        return true;
    }
}
