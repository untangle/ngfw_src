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
     * Structured free-text for descriptions, contact strings, system
     * locations, and similar label fields. Allows Unicode letters, digits,
     * Unicode whitespace ({@link Character#SPACE_SEPARATOR}), and the
     * explicitly-safe punctuation set {@code . _ - @ : , / + ( ) &}.
     *
     * <p><b>Design intent -- all special characters are blocked.</b>
     * Rather than trying to enumerate which special characters are safe
     * for every current and future sink, {@code SIMPLE_TEXT} takes the
     * opposite approach: it allows only characters that have no
     * shell-metacharacter meaning in any context. The excluded set
     * therefore includes <em>all</em> of the following, even though some
     * are harmless in certain sinks today:
     * {@code ' " ` $ ; | < > ! { } [ ] # ? * \ \n \r \t}.
     * Note: {@code &} is in the <em>allowed</em> set (it is not a
     * shell-exec operator in the sinks SIMPLE_TEXT fields currently
     * reach). In particular, the apostrophe ({@code '}) is intentionally
     * excluded -- it terminates single-quoted shell strings and can break
     * out of any config-file format that uses single-quote delimiters.
     * Customers whose stored descriptions contain these characters will
     * receive an error on the next settings save; this is a documented
     * migration side-effect, not a bug.</p>
     *
     * <p><b>Migration note:</b> real-world descriptions with apostrophes
     * (e.g. {@code "admin's device"}) or pipe characters
     * (e.g. {@code "Server | DMZ"}) are correctly rejected. Customers
     * must remove those characters before saving.</p>
     */
    SIMPLE_TEXT(
        Pattern.compile("^[\\p{L}\\p{N}\\p{Zs}._@:,/+()&-]{1,256}$"),
        256,
        "must contain only letters, digits, spaces, and the safe punctuation . _ - @ : , / + ( ) & (max 256)"),

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
     * <p><b>Limits:</b> total string length &le; 8192 chars; max 256
     * entries - both well above the size of any realistic VPN config
     * but bounded to avoid pathological input.</p>
     *
     * <p><b>Sink contract:</b> this type accepts LF and CR as entry
     * separators. Any sink that interpolates an {@code IP_OR_CIDR_LIST}
     * value into a <em>single-line</em> config slot (e.g. an ipsec.conf
     * {@code leftsubnet=} directive) MUST join the parsed entries with
     * commas before writing -- never write the raw multi-line value
     * verbatim, because a literal newline would inject a new ipsec.conf
     * directive into the same conn block. Sinks that write to
     * newline-tolerant formats (e.g. a UI textarea) may use the value
     * verbatim.</p>
     */
    IP_OR_CIDR_LIST(
        null,  // validated programmatically, see validate()
        8192,
        "must be a comma- or newline-separated list of valid IPv4/IPv6 addresses or CIDRs (max 256 entries)"),

    /**
     * Comma-separated list of {@link #HOSTNAME} entries. Used for multi-domain
     * fields like {@code WireGuardVpnSettings.dnsSearchDomain}. LF and CR are
     * intentionally rejected (unlike {@link #IP_OR_CIDR_LIST}) because every
     * current sink writes this field to a single-line config slot -- a newline
     * in the value would inject a new wg-quick directive line.
     *
     * <p><b>Limits:</b> total string length &le; 8192 chars; max 256 entries.
     * Each entry validates as {@link #HOSTNAME}.</p>
     */
    HOSTNAME_LIST(
        null,  // validated programmatically, see validate()
        8192,
        "must be a comma-separated list of valid hostnames (max 256 entries; no newlines; each: alphanumeric, '.', '_', '-'; first char alphanumeric; max 253)"),

    /**
     * Comma-separated list of {@link #HOSTNAME}-or-{@link #IP_OR_CIDR} entries.
     * Used for peer-address fields that accept either a FQDN or an IP literal
     * (e.g. {@code IpsecVpnTunnel.right}). LF and CR are rejected because the
     * field feeds into a single-line ipsec.conf directive -- a newline would
     * inject a {@code leftupdown=} directive in the same conn block.
     * Magic values like {@code %any} can be passed through the
     * {@link SafeCheck#allow()} allowlist.
     *
     * <p><b>Limits:</b> total string length &le; 8192 chars; max 256 entries.
     * Each entry validates as {@link #HOSTNAME} or {@link #IP_OR_CIDR}.</p>
     */
    PEER_LIST(
        null,  // validated programmatically, see validate()
        8192,
        "must be a comma-separated list of hostnames or IPv4/IPv6 addresses/CIDRs (max 256 entries; no newlines)"),

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
     * Username or email-address shape for credentials like PPPoE usernames
     * and dynamic-DNS login IDs. Pattern {@code ^[A-Za-z0-9][A-Za-z0-9._%+@-]*$}
     * -- the conservative subset that covers both plain usernames
     * ({@code alice}, {@code admin}) and email-format usernames common in ISP
     * PPPoE deployments ({@code user@isp.com}, {@code alice@bt.com}). Length
     * cap 254 matches the SMTP path limit.
     *
     * <p><b>Why not {@link #ALPHANUM}?</b> {@code ALPHANUM} rejects {@code @}
     * and {@code +}, which appear in real-world PPPoE usernames (BT, CenturyLink,
     * German DTAG). Production-data sweep found {@code @}-format usernames in
     * approximately 32% of sampled appliances with PPPoE configured -- using
     * {@code ALPHANUM} would produce widespread migration failures.</p>
     *
     * <p><b>Why not {@link #EMAIL}?</b> {@code EMAIL} requires a full
     * {@code user@domain.tld} form with a 2+ letter TLD. Plain usernames like
     * {@code alice} or {@code admin} would be rejected.</p>
     *
     * <p><b>RCE safety:</b> all accepted characters ({@code . _ % + @ -}) are
     * inert in every current sink. {@code pppoe_manager.py} writes the value
     * to both {@code /etc/ppp/peers/*} (as {@code user "value"} -- double-quoted)
     * and {@code pap-secrets}/{@code chap-secrets}. The peers file has
     * {@code connect=}/{@code pty=} exec directives, so {@code \n} and {@code "}
     * are the dangerous characters there; both are excluded from this type's
     * character set, closing the newline-injection and double-quote-escape
     * vectors. {@code ddclient.conf} {@code login=} is an unquoted INI value
     * token -- {@code \n} would inject a {@code cmd=} exec directive, also
     * excluded. Shell metacharacters ({@code $ ` ; | < > " ' \\})
     * remain rejected.</p>
     */
    USERNAME_OR_EMAIL(
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._%+@-]*$"),
        254,
        "must be a username or email address (letters, digits, '.', '_', '%', '+', '@', '-'; first char alphanumeric; max 254)"),

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
     *
     * <p><b>Sink contract:</b> values of this type are consumed by the
     * mail API (JavaMail) only -- they are <b>never</b> interpolated into
     * a shell command line. Shell metacharacters admitted by this type
     * ({@code $ ` ; | < > " ' \}) are inert in a mail-API context but
     * would execute if the value ever reached a shell. Any future sink
     * that consumes an {@code EMAIL_TEMPLATE} value must use
     * {@code execCommand(argv)} form or {@code shlex.quote()} -- the same
     * contract required for {@link #OPAQUE_SECRET}.</p>
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
     * envelope with matching label, and rejects control characters
     * other than {@code \r}, {@code \n}, {@code \t}. The body is
     * <i>not</i> constrained to a strict base64 alphabet, so real-world
     * exports work: openssl's {@code x509 -text} preamble, PKCS12
     * {@code Bag Attributes:} headers, encrypted-key
     * {@code Proc-Type:}/{@code DEK-Info:} headers, and benign
     * commentary between blocks are all accepted.</p>
     *
     * <p><b>RCE contract:</b> the PEM payload is written to a file
     * with {@code FileOutputStream} and then read by openssl - it is
     * never interpolated into a shell command line, so body characters
     * have no path to a shell. The control-char check still blocks NUL
     * (which truncates C strings) and the BEL/ESC class that can
     * corrupt downstream log handling. Cryptographic validity is left
     * to the openssl sink.</p>
     */
    PEM(
        null, // validated programmatically, see validate()
        16384,
        "must contain at least one -----BEGIN ...----- / -----END ...----- envelope with matching label and no control characters other than CR/LF/TAB (max 16 KB)"),

    /**
     * X.500 distinguished name as composed by the certificate UI:
     * {@code /CN=Foo/C=US/ST=CA/L=City/O=Org} etc. Each RDN is a 1-4
     * letter type label, an {@code =}, and a value drawn from the
     * subset {@code [\p{L}\p{N} ._@:+()*'&,-]} - Unicode letters and
     * digits, spaces, and the safe punctuation needed by real-world
     * subjects ({@code O'Brien}, {@code Smith & Co}, {@code Foo, Inc.},
     * names with accented characters). No quoted RDNs and no embedded
     * slashes inside values - both legal in X.500 but neither used by
     * the UI dialog. {@code *} is permitted to support wildcard CNs
     * (e.g. {@code /CN=*.example.com}); apostrophe, ampersand and
     * comma are safe because the value is passed argv-form to openssl
     * and the helper script uses {@code "$2"}-quoted parameter
     * expansion, neither of which re-interprets these as shell metas.
     * Shell metacharacters {@code $ ` ; | < > " \\} and newlines remain
     * rejected. Length capped at 512.
     */
    CERT_SUBJECT(
        Pattern.compile("^/[A-Za-z]{1,4}=[\\p{L}\\p{N} ._@:+()*'&,-]+(/[A-Za-z]{1,4}=[\\p{L}\\p{N} ._@:+()*'&,-]+)*$"),
        512,
        "each field in the certificate subject must be non-empty and use only letters, digits, spaces, or . _ @ : + ( ) * ' & , - (e.g. /CN=Host/C=US/ST=California/L=City/O=Org or /CN=*.example.com; max 512 chars)"),

    /**
     * Natural-language proper name - a person's name, an organization name,
     * a CA common name, or any other single-segment free-form name field
     * that needs to accommodate real-world punctuation. Examples:
     * {@code O'Brien}, {@code Smith & Co}, {@code Foo, Inc.},
     * {@code Sao Paulo Org}, {@code Acme Root CA}.
     *
     * <p>Charset: Unicode letters/digits, spaces, and the safe punctuation
     * {@code . _ - @ : + ( ) * ' & ,}. Shell metacharacters
     * ({@code $ ; | < > " \ ` }) and newlines remain rejected. The path
     * separator {@code /} is intentionally <i>not</i> in the charset:
     * NATURAL_NAME values are single segments (one CA name, one person's
     * name) and several sinks ({@code ut-rootgen}, cert-store directory
     * layout) use the value as a single filesystem path component.
     * Path traversal sequences ({@code ..}) are also rejected
     * programmatically - see {@link #validate(String)}.</p>
     *
     * <p><b>Sink contract:</b> safe wherever the value is consumed
     * argv-form (openssl, exec arrays), URL-encoded (URIBuilder), or as
     * JSON HTTP-body content. If the value is interpolated into a shell
     * command - even argv-passed and then re-expanded inside a script -
     * <b>the receiving shell variable must be double-quoted</b>
     * ({@code "$VAR"}), otherwise the allowed {@code &amp;}, {@code *},
     * space, and {@code (} {@code )} characters become shell-active.
     * This is the contract that {@code ut-rootgen} satisfies for the CA
     * directory name.</p>
     */
    NATURAL_NAME(
        Pattern.compile("^[\\p{L}\\p{N} ._@:+()*'&,-]{1,256}$"),
        256,
        "must contain only letters, digits, spaces, and the safe punctuation . _ - @ : + ( ) * ' & , (no '..' or '/'; max 256 chars)"),

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
     * OAuth 2.0 authorization code / token shape. Used for values received
     * from third-party identity providers (Google Drive, etc.) that are
     * then submitted back to the provider's token endpoint as
     * {@code application/x-www-form-urlencoded} body content.
     *
     * <p>Real-world codes include the base64url alphabet
     * ({@code A-Z a-z 0-9 _ -}), the standard-base64 alphabet
     * ({@code / + =} padding), the JWT segment separator {@code .},
     * and occasionally {@code %} from upstream percent-encoding. Length
     * cap 4096 covers JWT-shaped responses and long Google "4/..." codes
     * with margin.</p>
     *
     * <p><b>Leading-char rule:</b> first character is restricted to
     * {@code [A-Za-z0-9]} (no leading {@code -} or {@code /}) to close
     * argv-option-injection in case any future sink shells the value.</p>
     *
     * <p><b>Sink contract:</b> the value is URL-encoded into a form body
     * and POSTed to the provider - it never reaches a shell. The
     * allowlist still blocks NUL / CR / LF and other control bytes that
     * could smuggle header lines into a downstream HTTP client.</p>
     */
    OAUTH_CODE(
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._\\-/=+%]*$"),
        4096,
        "must be an OAuth code/token (letters, digits, '.', '_', '-', '/', '=', '+', '%'; first char alphanumeric; max 4096 chars)"),

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
     *                       programmatically (IP_OR_CIDR, IP_OR_CIDR_LIST,
     *                       HOSTNAME_LIST, PEER_LIST, URL, OPAQUE_SECRET).
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
     * @return the maximum permitted length of an accepted value for this type.
     */
    public int maxLength()
    {
        return maxLength;
    }

    /**
     * Returns a short human-readable reason explaining WHY {@code value}
     * would be rejected by {@link #validate(String)}. Designed for
     * inclusion in user-facing error messages.
     *
     * <p><b>Never echoes the offending value or any character from it.</b>
     * The reason is a category label and (for most types) a 1-based
     * position - never the character itself. This preserves the
     * credential-leak protection that the SafeCheckValidator error path
     * relies on.</p>
     *
     * <p>{@link #OPAQUE_SECRET} always returns a generic phrase with no
     * position info, to prevent character-class disclosure to anyone
     * who can submit values and watch the error channel.</p>
     *
     * @param value the value that {@link #validate(String)} returned false for;
     *              {@code null}/empty returns the empty string (those are always accepted).
     * @return a short reason phrase suitable for inclusion in error messages;
     *         empty if the value is null/empty (which would not have been rejected).
     */
    public String describeRejection(String value)
    {
        if (value == null || value.isEmpty()) return "";

        // Length is the cheapest and most informative check.
        if (value.length() > maxLength) {
            return "value is " + value.length() + " characters (maximum allowed is " + maxLength + ")";
        }

        switch (this) {
            case OPAQUE_SECRET:
                // Generic only - do not reveal which character class triggered.
                return "value contains a disallowed character";
            case EMAIL_TEMPLATE:
                return classifyControlCharAllowCrLfTab(value);
            case REGEX_PATTERN:
                return classifyControlCharAllowTabOnly(value);
            case PEM:
                return describePemRejection(value);
            case IP_OR_CIDR:
                return "value is not a valid IPv4 or IPv6 literal (with optional /CIDR prefix)";
            case IP_OR_CIDR_LIST:
                return describeIpListRejection(value);
            case HOSTNAME_LIST:
                return describeHostnameListRejection(value);
            case PEER_LIST:
                return describePeerListRejection(value);
            case URL:
                return "value is not a valid http(s) URL with a host and no embedded credentials";
            case SAN_LIST:
                return "value contains an entry that is not a valid SAN "
                     + "(expected DNS:hostname, IP:address, bare hostname, or bare IPv4)";
            case FILENAME:
                if (value.indexOf('/') >= 0) return "value contains '/' which is not allowed in a filename";
                if (value.contains("..")) return "value contains '..' which is not allowed in a filename";
                return describeRegexRejection(value);
            case FILE_PATH:
                if (value.contains("..")) return "value contains '..' which is not allowed in a path";
                if (!value.startsWith("/")) return "value must start with '/' (absolute path required)";
                return describeRegexRejection(value);
            case NATURAL_NAME:
                if (value.contains("..")) return "value contains '..' which is not allowed in a natural name";
                return describeRegexRejection(value);
            default:
                return describeRegexRejection(value);
        }
    }

    /**
     * Regex-type rejection classifier. Distinguishes
     *   (a) a char that is invalid everywhere (classify by category at the
     *       position it first appears), from
     *   (b) a body-valid char that is forbidden specifically in the leading
     *       position (report leading-position restriction).
     *
     * Never returns the character itself, only its category and 1-based
     * position.
     *
     * @param value the value that failed regex validation; must be non-null and non-empty
     * @return a human-readable reason phrase; never null
     */
    private String describeRegexRejection(String value)
    {
        char first = value.charAt(0);
        // If the leading char is invalid even in the body charset, it is
        // invalid everywhere - classify it by category. This gives a much
        // crisper message than "not allowed in first position" for cases
        // like a leading space in HOSTNAME or a leading shell-meta in
        // SIMPLE_TEXT (which has no leading-only restriction at all).
        if (!isBodyCharAllowed(first)) {
            return classifyDisallowedChar(first, 0);
        }
        // Body-valid but leading-restricted (e.g. '-', '.', or '_' as the
        // first char of a HOSTNAME).
        if (!isLeadingCharAllowed(first)) {
            return "value starts with a character not allowed in the first position";
        }
        // Walk body chars; return on first disallowed.
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isBodyCharAllowed(c)) {
                return classifyDisallowedChar(c, i);
            }
        }
        // All per-char checks passed but the full regex still failed.
        // Possible for shape-rigid types (BASE64_KEY, MAC_ADDRESS, EMAIL).
        return "value does not match the required format";
    }

    /**
     * Per-type leading-character predicate. Mirrors the regex's
     * leading-position character class. Used by
     * {@link #describeRegexRejection(String)} so the error can name
     * whether the failure is at the first position.
     *
     * @param c the character at position 0 of the candidate value
     * @return {@code true} if {@code c} is permitted in the leading position for this type
     */
    private boolean isLeadingCharAllowed(char c)
    {
        switch (this) {
            case HOSTNAME:
            case INTERFACE:
            case FILENAME:
            case EMAIL:
            case OAUTH_CODE:
                return isAsciiAlnum(c);
            case ALPHANUM:
                return isAsciiAlnum(c) || c == '_';
            case FILE_PATH:
                return c == '/';
            case CERT_SUBJECT:
                return c == '/';
            case MAC_ADDRESS:
                return isAsciiHex(c);
            case BASE64_KEY:
                return isAsciiAlnum(c) || c == '+' || c == '/';
            case USERNAME_OR_EMAIL:
                return isAsciiAlnum(c);
            case SIMPLE_TEXT:
            case NATURAL_NAME:
                // No leading-char restriction beyond the body charset.
                return isBodyCharAllowed(c);
            default:
                return true;
        }
    }

    /**
     * Per-type body-character predicate. Mirrors the regex's body
     * character class. Used by {@link #describeRegexRejection(String)} to
     * find the first disallowed character.
     *
     * @param c the character to test
     * @return {@code true} if {@code c} is permitted anywhere in the body of a value of this type
     */
    private boolean isBodyCharAllowed(char c)
    {
        switch (this) {
            case HOSTNAME:
            case FILENAME:
                return isAsciiAlnum(c) || c == '.' || c == '_' || c == '-';
            case INTERFACE:
                return isAsciiAlnum(c) || c == '.' || c == '_' || c == ':' || c == '-';
            case ALPHANUM:
                return isAsciiAlnum(c) || c == '.' || c == '_' || c == '-';
            case SIMPLE_TEXT:
                // [\p{L}\p{N}\p{Zs}._@:,/+()&-]
                if (Character.isLetter(c) || Character.isDigit(c)) return true;
                if (Character.getType(c) == Character.SPACE_SEPARATOR) return true;
                return ".-_@:,/+()&".indexOf(c) >= 0;
            case EMAIL:
                // Local part is [A-Za-z0-9._%+-], domain adds nothing more
                // dangerous; classify at char level only.
                return isAsciiAlnum(c) || ".-_%+@".indexOf(c) >= 0;
            case USERNAME_OR_EMAIL:
                return isAsciiAlnum(c) || "._-%+@".indexOf(c) >= 0;
            case OAUTH_CODE:
                return isAsciiAlnum(c) || "._-/=+%".indexOf(c) >= 0;
            case FILE_PATH:
                return isAsciiAlnum(c) || c == '.' || c == '_' || c == '/' || c == '-';
            case BASE64_KEY:
                return isAsciiAlnum(c) || c == '+' || c == '/' || c == '=';
            case MAC_ADDRESS:
                return isAsciiHex(c) || c == ':' || c == '-';
            case NATURAL_NAME:
                if (Character.isLetter(c) || Character.isDigit(c)) return true;
                return " ._@:+()*'&,-".indexOf(c) >= 0;
            case CERT_SUBJECT:
                if (Character.isLetter(c) || Character.isDigit(c)) return true;
                return " ._@:+()*'&,-=/".indexOf(c) >= 0;
            default:
                return true;
        }
    }

    /**
     * @param c the character to test
     * @return {@code true} if {@code c} is an ASCII letter or digit
     */
    private static boolean isAsciiAlnum(char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /**
     * @param c the character to test
     * @return {@code true} if {@code c} is an ASCII hexadecimal digit (0-9, A-F, a-f)
     */
    private static boolean isAsciiHex(char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    /**
     * Category-labels a disallowed character by class. 1-based position
     * for human-friendly output. Never returns the character itself.
     *
     * @param c the disallowed character
     * @param zeroBasedPos zero-based index of {@code c} in the original value
     * @return a human-readable category phrase including the 1-based position; never null
     */
    private static String classifyDisallowedChar(char c, int zeroBasedPos)
    {
        int pos = zeroBasedPos + 1;
        if (Character.getType(c) == Character.CONTROL) {
            return "value contains a control character at position " + pos;
        }
        if (Character.isWhitespace(c)) {
            return "value contains a whitespace character at position " + pos;
        }
        if (c == '"' || c == '\'' || c == '\\') {
            return "value contains a quote or backslash at position " + pos;
        }
        if ("$`;|<>&".indexOf(c) >= 0) {
            return "value contains a shell metacharacter at position " + pos;
        }
        if (c > 127) {
            return "value contains a non-ASCII character at position " + pos;
        }
        return "value contains a disallowed character at position " + pos;
    }

    /**
     * Classifies control-character rejection for EMAIL_TEMPLATE, which
     * permits CR/LF/TAB and rejects every other Cc-class character.
     *
     * @param value the candidate string; assumed non-null and non-empty
     * @return a human-readable reason phrase identifying the offending position; never null
     */
    private static String classifyControlCharAllowCrLfTab(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') continue;
            if (Character.getType(c) == Character.CONTROL) {
                return "value contains a control character at position " + (i + 1)
                     + " (NUL, ESC, BEL, etc.)";
            }
        }
        return "value does not match the required format";
    }

    /**
     * Classifies control-character rejection for REGEX_PATTERN, which
     * permits TAB only and rejects every other Cc-class character
     * (including CR/LF).
     *
     * @param value the candidate string; assumed non-null and non-empty
     * @return a human-readable reason phrase identifying the offending position; never null
     */
    private static String classifyControlCharAllowTabOnly(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\t') continue;
            if (Character.getType(c) == Character.CONTROL) {
                return "value contains a control character at position " + (i + 1)
                     + " (NUL, CR, LF, ESC, BEL, etc.; only TAB is allowed)";
            }
        }
        return "value does not match the required format";
    }

    /**
     * IP_OR_CIDR_LIST rejection classifier. Distinguishes "too many entries"
     * from "an entry is not a valid IP/CIDR".
     *
     * @param value the candidate list string; assumed non-null and non-empty
     * @return a human-readable reason phrase; never null
     */
    private static String describeIpListRejection(String value)
    {
        String[] entries = value.split("[,\\r\\n]+", -1);
        if (entries.length > 256) {
            return "list has " + entries.length + " entries (maximum allowed is 256)";
        }
        for (int i = 0; i < entries.length; i++) {
            String trimmed = entries[i].trim();
            if (trimmed.isEmpty()) continue;
            if (!validateIpOrCidr(trimmed)) {
                return "entry " + (i + 1) + " is not a valid IPv4/IPv6 address or CIDR";
            }
        }
        return "value does not match the required list format";
    }

    /**
     * HOSTNAME_LIST rejection classifier. Checks for disallowed newlines,
     * entry-count overflow, and invalid individual hostname entries.
     *
     * @param value the candidate list string; assumed non-null and non-empty
     * @return a human-readable reason phrase; never null
     */
    private static String describeHostnameListRejection(String value)
    {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "list contains a newline character; only comma-separated entries are allowed";
        }
        String[] entries = value.split(",", -1);
        if (entries.length > 256) {
            return "list has " + entries.length + " entries (maximum allowed is 256)";
        }
        for (int i = 0; i < entries.length; i++) {
            String trimmed = entries[i].trim();
            if (trimmed.isEmpty()) continue;
            if (!HOSTNAME.validate(trimmed)) {
                return "entry " + (i + 1) + " is not a valid hostname";
            }
        }
        return "value does not match the required hostname list format";
    }

    /**
     * PEER_LIST rejection classifier. Checks for disallowed newlines,
     * entry-count overflow, and invalid individual peer entries.
     *
     * @param value the candidate list string; assumed non-null and non-empty
     * @return a human-readable reason phrase; never null
     */
    private static String describePeerListRejection(String value)
    {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "list contains a newline character; only comma-separated entries are allowed";
        }
        String[] entries = value.split(",", -1);
        if (entries.length > 256) {
            return "list has " + entries.length + " entries (maximum allowed is 256)";
        }
        for (int i = 0; i < entries.length; i++) {
            String trimmed = entries[i].trim();
            if (trimmed.isEmpty()) continue;
            if (!HOSTNAME.validate(trimmed) && !IP_OR_CIDR.validate(trimmed)) {
                return "entry " + (i + 1) + " is not a valid hostname or IP/CIDR";
            }
        }
        return "value does not match the required peer list format";
    }

    /**
     * PEM rejection classifier. Distinguishes control-char rejection
     * from missing/mismatched envelope.
     *
     * @param value the candidate PEM string; assumed non-null and non-empty
     * @return a human-readable reason phrase; never null
     */
    private static String describePemRejection(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') continue;
            if (Character.getType(c) == Character.CONTROL) {
                return "value contains a control character at position " + (i + 1)
                     + " (only CR, LF, TAB are allowed)";
            }
        }
        return "value is missing a valid -----BEGIN <LABEL>-----/-----END <LABEL>----- envelope "
             + "with matching labels";
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
            case HOSTNAME_LIST:
                return validateHostnameList(value);
            case PEER_LIST:
                return validatePeerList(value);
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
            case NATURAL_NAME:
                // Block path-traversal sequences. The charset does not
                // include '/', but ".." in a single segment must still be
                // rejected because sinks like ut-rootgen use the value as
                // a filesystem path component.
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

    /**
     * Comma-separated HOSTNAME list. LF and CR are rejected entirely --
     * every current sink for HOSTNAME_LIST writes the value to a single-line
     * config slot; a newline would inject a new directive.
     *
     * @param value the candidate list; assumed non-null and non-empty
     * @return {@code true} if every non-blank entry is a valid HOSTNAME and
     *         no newlines are present and the entry count does not exceed 256
     */
    private static boolean validateHostnameList(String value)
    {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return false;
        }
        String[] entries = value.split(",", -1);
        if (entries.length > 256) {
            return false;
        }
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!HOSTNAME.validate(trimmed)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Comma-separated list of HOSTNAME-or-IP_OR_CIDR entries. LF and CR are
     * rejected for the same single-line-sink reason as HOSTNAME_LIST.
     *
     * @param value the candidate list; assumed non-null and non-empty
     * @return {@code true} if every non-blank entry validates as either HOSTNAME
     *         or IP_OR_CIDR, no newlines are present, and the count is &lt;= 256
     */
    private static boolean validatePeerList(String value)
    {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return false;
        }
        String[] entries = value.split(",", -1);
        if (entries.length > 256) {
            return false;
        }
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!HOSTNAME.validate(trimmed) && !IP_OR_CIDR.validate(trimmed)) {
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

    /**
     * PEM envelope check. Requires at least one
     * {@code -----BEGIN <LABEL>-----} ... {@code -----END <LABEL>-----}
     * pair with matching label, and rejects control characters other
     * than CR/LF/TAB anywhere in the input. The body and inter-block
     * regions are otherwise unrestricted so real-world exports work
     * (openssl preambles, PKCS12 {@code Bag Attributes:} headers,
     * encrypted-key headers, benign commentary).
     *
     * <p>This is intentionally permissive: the payload is written to a
     * file and parsed by openssl - it never reaches a shell. The
     * control-character check still blocks NUL / BEL / ESC and similar
     * vectors. Cryptographic validity is left to the openssl sink.</p>
     *
     * @param value the candidate PEM string; assumed non-null and non-empty.
     * @return {@code true} if at least one well-formed envelope (matching
     *         BEGIN/END labels) is present and no disallowed control
     *         characters appear.
     */
    private static boolean validatePem(String value)
    {
        // Reject control chars except CR/LF/TAB. NUL truncates C strings;
        // BEL/ESC corrupt downstream log handling. Everything printable
        // is fine - openssl is the real parser.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') continue;
            if (Character.getType(c) == Character.CONTROL) return false;
        }

        // Require at least one BEGIN/END pair with matching label.
        java.util.regex.Matcher beginM = PEM_BEGIN.matcher(value);
        java.util.regex.Matcher endM   = PEM_END.matcher(value);
        int pos = 0;
        int blocks = 0;
        while (beginM.find(pos)) {
            int beginEnd = beginM.end();
            String label = beginM.group(1);
            if (!endM.find(beginEnd)) return false;
            if (!label.equals(endM.group(1))) return false;
            pos = endM.end();
            blocks++;
        }
        return blocks > 0;
    }
}
