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
     * Permissive text for descriptions, contact strings, system locations,
     * and natural-language names like {@code O'Brien} or {@code John's Folder}.
     * Allows Unicode letters, digits, whitespace, the safe punctuation
     * set [. _ - @ : , / + ( )], and the apostrophe ['].
     * Excludes shell metacharacters ({@code $ & ` ; | < > \n \r "}).
     *
     * <p>The apostrophe is permitted because every current sink consumes
     * {@code SIMPLE_TEXT} values either argv-form (e.g. openssl), URL-encoded
     * (URIBuilder), or as JSON HTTP-body content - none string-concatenate
     * the value into a shell command line. Future sinks that do shell
     * interpolation must quote with {@code shlex.quote(...)} (Python) or
     * {@code execCommand(List.of(...))} (Java); this is the same contract
     * already required for {@link #OPAQUE_SECRET}.</p>
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
     * IEEE 802 MAC address (RFC 7042) in standard hex-octet form,
     * separated by ':' or '-'. Examples: {@code aa:bb:cc:dd:ee:ff},
     * {@code AA-BB-CC-DD-EE-FF}.
     *
     * <p>Length is fixed at 17 (12 hex digits + 5 separators). The regex
     * matches the existing UI rule {@code macAddressRegex} in
     * {@code vuntangle/src/plugins/init-vee-validate.js:26} so backend and
     * UI agree exactly.</p>
     */
    MAC_ADDRESS(
        Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$"),
        17,
        "must be a valid MAC address (e.g. aa:bb:cc:dd:ee:ff or AA-BB-CC-DD-EE-FF)"),

    /**
     * Free-form secret. Accepts any printable Unicode including shell
     * metacharacters; rejects only control characters. See class
     * Javadoc for the sink-side handling contract.
     */
    OPAQUE_SECRET(
        null, // validated programmatically, see validate()
        256,
        "must be 1-256 characters; control characters (NUL, ESC, BEL, etc.) are not allowed"),

    /**
     * RFC 5321-style email address. Length 1-254 (the SMTP path limit).
     * Pattern requires a single '@', a non-empty local part starting
     * with an alphanumeric character (closes argv-option-injection),
     * and a domain with at least one dot and a 2+ letter TLD. The
     * local-part character class is the conservative subset
     * {@code [A-Za-z0-9._%+-]} - quoted-local-parts and IP-literal
     * domains are not accepted; neither has ever been used in this
     * codebase's UI flows.
     */
    EMAIL(
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._%+-]*@[A-Za-z0-9][A-Za-z0-9.-]*\\.[A-Za-z]{2,}$"),
        254,
        "must be an email address like user@example.com , local part must start with a letter or digit, domain must have at least one dot and a 2+ letter TLD (max 254 chars)"),

    /**
     * Multi-line email subject/body template that may contain
     * {@code ${var}} placeholders. Length up to 8192 chars to fit
     * realistic alert templates with multiple variable substitutions.
     *
     * <p>Validated programmatically: rejects any character whose
     * Unicode category is {@code Cc} (control) <i>except</i> the line
     * separators {@code \r} and {@code \n} and the horizontal tab
     * {@code \t}. This blocks NUL, ESC, BEL, etc. - vectors that could
     * be smuggled into a subject header or body for downstream
     * mail-injection - while preserving normal multiline content.</p>
     */
    EMAIL_TEMPLATE(
        null, // validated programmatically, see validate()
        8192,
        "must contain only printable text, line breaks (CR/LF), and tabs , other control characters (NUL, ESC, BEL, etc.) are not allowed (max 8192 chars)"),

    /**
     * PEM-encoded data block (certificate, key, or chain). Length cap
     * 16384 chars - sized to cover a worst-case real-world chain
     * (server + 3 RSA-4096 intermediates &asymp; 9-12 KB) with margin
     * for unusual but legitimate cases (cross-sign certs, long DNs).
     * Rejects gross abuse (multi-MB blobs) without blocking real uploads.
     *
     * <p>Validated programmatically: requires at least one
     * {@code -----BEGIN <LABEL>-----} ... {@code -----END <LABEL>-----}
     * envelope; permits multiple concatenated blocks separated by
     * whitespace (legal for cert chains). Body characters between the
     * BEGIN/END markers are restricted to the base64 alphabet plus
     * whitespace. Cryptographic validity is <b>not</b> checked here -
     * the OpenSSL sink layer handles that.</p>
     */
    PEM(
        null, // validated programmatically, see validate()
        16384,
        "must be a PEM block bounded by -----BEGIN ...----- and -----END ...----- markers with a base64 body (max 16 KB)"),

    /**
     * X.500 distinguished name as composed by the certificate UI:
     * {@code /CN=Foo/C=US/ST=CA/L=City/O=Org} etc. Each RDN is a 1-4
     * letter type label, an {@code =}, and a value drawn from the
     * conservative subset {@code [A-Za-z0-9 ._@:+()*-]}. No quoted
     * RDNs, no embedded slashes inside values - both legal in X.500
     * but neither used by the UI dialog. {@code *} is permitted to
     * support wildcard CNs (e.g. {@code /CN=*.example.com}); this is
     * safe because the value is passed argv-form to openssl and never
     * interpreted by a shell. Length capped at 512.
     */
    CERT_SUBJECT(
        Pattern.compile("^/[A-Za-z]{1,4}=[A-Za-z0-9 ._@:+()*-]+(/[A-Za-z]{1,4}=[A-Za-z0-9 ._@:+()*-]+)*$"),
        512,
        "each field in the certificate subject must be non-empty and use only letters, digits, spaces, or . _ @ : + ( ) * - (e.g. /CN=Host/C=US/ST=California/L=City/O=Org or /CN=*.example.com; max 512 chars)"),

    /**
     * X.509 Subject Alternative Name list as composed by the certificate
     * UI. Comma-separated entries; each entry is one of:
     * <ul>
     *   <li>{@code DNS:hostname} or {@code DNS:*.hostname} (wildcard SAN)</li>
     *   <li>{@code IP:ipv4}</li>
     *   <li>bare hostname (with optional {@code *.} prefix)</li>
     *   <li>bare IPv4 literal</li>
     * </ul>
     *
     * <p>Length cap 1024 - covers realistic multi-SAN certs without
     * permitting pathological input. Each entry is trimmed; blank
     * entries are skipped.</p>
     *
     * <p>Sink: passed argv-form to openssl as the {@code subjectAltName}
     * extension. {@code *} is permitted for wildcard SANs and is not a
     * shell metacharacter in this argv path.</p>
     */
    SAN_LIST(
        null,  // validated programmatically, see validate()
        1024,
        "must be a comma-separated list of SAN entries (e.g. DNS:host.example.com, DNS:*.example.com, IP:10.0.0.1; max 1024 chars)"),

    /**
     * Regular-expression pattern. Permits any printable Unicode -
     * including all regex metacharacters ({@code . * + ? ^ $ ( ) [ ] { } | \})
     * and chars like {@code $ & ;} - so legitimate patterns such as
     * {@code .*\/network.*} or {@code ^/etc/.*\.conf$} are not rejected.
     *
     * <p>The only chars rejected are control characters (Unicode {@code Cc}
     * category) other than the horizontal tab. This blocks NUL, ESC, BEL,
     * CR, LF - vectors for log-line smuggling, terminal-escape injection,
     * and string-truncation in C-based regex engines.</p>
     *
     * <p>Length cap 1024 - large enough for any realistic regex, small
     * enough to bound pathological input.</p>
     *
     * <p><b>Sink contract:</b> use only when the value is passed
     * <i>argv-form</i> (e.g. {@code execCommand(script, List.of(..., regex))}).
     * Never string-concatenate a {@code REGEX_PATTERN} value into a shell
     * command line - regex chars like {@code $ ` ( )} are also shell metas
     * and will execute if the value reaches a shell.</p>
     */
    REGEX_PATTERN(
        null,  // validated programmatically, see validate()
        1024,
        "must be a regular expression up to 1024 characters; control characters (NUL, ESC, BEL, CR, LF) are not allowed"),

    /**
     * Absolute filesystem path for a server-controlled file (e.g. a
     * tempfile path returned by an upload handler). Length up to 4096
     * (Linux {@code PATH_MAX}).
     *
     * <p>Pattern: must begin with {@code /} (absolute) - this closes the
     * argv-option-injection class (a value starting with {@code -}
     * cannot pass). Body characters limited to
     * {@code [A-Za-z0-9._/-]} - no whitespace, no shell metacharacters,
     * no backslash. The validator additionally rejects any {@code ..}
     * substring to block path traversal.</p>
     *
     * <p>This is a content check only; canonicalisation and the
     * "must live under {@code /tmp/...}" check (where applicable)
     * remain the responsibility of the sink.</p>
     */
    FILE_PATH(
        Pattern.compile("^/[A-Za-z0-9._/-]+$"),
        4096,
        "must be an absolute path starting with '/' using only letters, digits, '.', '_', '/', or '-' , no '..', whitespace, or shell metacharacters (max 4096 chars)");

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
            case EMAIL_TEMPLATE:
                return validateEmailTemplate(value);
            case PEM:
                return validatePem(value);
            case SAN_LIST:
                return validateSanList(value);
            case REGEX_PATTERN:
                return validateRegexPattern(value);
            case FILENAME:
                if (value.indexOf('/') >= 0 || value.contains("..")) {
                    return false;
                }
                return pattern.matcher(value).matches();
            case FILE_PATH:
                if (value.contains("..")) {
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

    /** Hostname segment with optional leading '*.' wildcard. */
    private static final Pattern SAN_HOSTNAME_PATTERN = Pattern.compile(
        "^(\\*\\.)?[A-Za-z0-9][A-Za-z0-9._-]*$");

    /**
     * Comma-separated SAN list. Each entry is one of:
     *   DNS:hostname (with optional '*.' wildcard)
     *   IP:ipv4
     *   bare hostname (with optional '*.' wildcard)
     *   bare ipv4
     * Bounded to 64 entries; blank entries skipped.
     *
     * @param value the candidate list; assumed non-null and non-empty.
     * @return {@code true} if every non-blank entry parses as one of the
     *         shapes above; {@code false} otherwise.
     */
    private static boolean validateSanList(String value)
    {
        String[] entries = value.split(",", -1);
        if (entries.length > 64) {
            return false;
        }
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String body = trimmed;
            if (trimmed.startsWith("DNS:")) {
                body = trimmed.substring(4);
                if (body.isEmpty()) return false;
                if (!SAN_HOSTNAME_PATTERN.matcher(body).matches()) return false;
            } else if (trimmed.startsWith("IP:")) {
                body = trimmed.substring(3);
                if (body.isEmpty()) return false;
                if (!IPV4_PATTERN.matcher(body).matches()
                    && !IPV6_PATTERN.matcher(body).matches()) {
                    return false;
                }
            } else {
                // bare entry: IPv4 literal or hostname (with optional '*.' )
                if (!IPV4_PATTERN.matcher(trimmed).matches()
                    && !SAN_HOSTNAME_PATTERN.matcher(trimmed).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Regex pattern body. Permissive: rejects only Unicode {@code Cc}
     * (control) characters other than the horizontal tab. All other
     * printable code points - including regex metas {@code . * + ? ^ $ ( ) [ ] { } | \}
     * and shell-shaped chars like {@code $ & ;} - are accepted.
     *
     * @param value the candidate regex; assumed non-null and non-empty.
     * @return {@code true} if {@code value} contains no disallowed control
     *         characters; {@code false} otherwise.
     */
    private static boolean validateRegexPattern(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\t') continue;
            if (Character.getType(c) == Character.CONTROL) {
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

    /**
     * Multi-line template body. Permits CR, LF, and TAB; rejects every
     * other Cc-class control character (NUL, ESC, BEL, etc.) - those are
     * the vectors for SMTP header smuggling and similar downstream
     * mail-injection attacks.
     *
     * @param value the candidate template string; assumed non-null and non-empty.
     * @return {@code true} if no disallowed control characters are present.
     */
    private static boolean validateEmailTemplate(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') continue;
            if (Character.getType(c) == Character.CONTROL) {
                return false;
            }
        }
        return true;
    }

    /** Recognises lines like "-----BEGIN CERTIFICATE-----" / "-----END CERTIFICATE-----". */
    private static final Pattern PEM_BEGIN = Pattern.compile("-----BEGIN ([A-Z0-9 ]+)-----");
    private static final Pattern PEM_END   = Pattern.compile("-----END ([A-Z0-9 ]+)-----");

    /** Base64 alphabet plus PEM line-wrap whitespace. */
    private static final Pattern PEM_BODY_CHARS = Pattern.compile("^[A-Za-z0-9+/=\\s]*$");

    /**
     * PEM envelope check. Requires at least one
     * {@code -----BEGIN <LABEL>-----} ... {@code -----END <LABEL>-----}
     * pair with matching label and a base64-only body. Permits multiple
     * concatenated blocks (cert chain) separated by whitespace. Rejects
     * any character outside the base64 alphabet within a block - this
     * blocks shell metacharacter smuggling between BEGIN/END markers.
     *
     * <p>Cryptographic validity is not checked - the OpenSSL sink
     * parses the PEM for real before use.</p>
     *
     * @param value the candidate PEM string; assumed non-null and non-empty.
     * @return {@code true} if at least one well-formed envelope is present
     *         and the entire input parses as a sequence of envelopes.
     */
    private static boolean validatePem(String value)
    {
        java.util.regex.Matcher beginM = PEM_BEGIN.matcher(value);
        java.util.regex.Matcher endM   = PEM_END.matcher(value);
        int pos = 0;
        int blocks = 0;
        while (beginM.find(pos)) {
            int beginStart = beginM.start();
            int beginEnd   = beginM.end();
            String label   = beginM.group(1);

            // Anything between pos and beginStart must be whitespace
            // (blank lines or stray newlines between blocks are legal;
            // shell metacharacters / commentary are not).
            for (int i = pos; i < beginStart; i++) {
                if (!Character.isWhitespace(value.charAt(i))) return false;
            }

            if (!endM.find(beginEnd)) return false;
            if (!label.equals(endM.group(1))) return false; // mismatched label

            String body = value.substring(beginEnd, endM.start());
            if (!PEM_BODY_CHARS.matcher(body).matches()) return false;

            pos = endM.end();
            blocks++;
        }
        if (blocks == 0) return false;

        // Trailing whitespace (only) after the last END marker is OK.
        for (int i = pos; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) return false;
        }
        return true;
    }
}
