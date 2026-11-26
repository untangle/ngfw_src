/**
 * Directory Connector's self-contained LDAPS public-certificate trust records.
 */
package com.untangle.app.directory_connector;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONString;

/** A public certificate explicitly enrolled by an administrator, never a private key. */
@SuppressWarnings("serial")
public class LdapsTrustAnchor implements java.io.Serializable, JSONString
{
    /** Maximum number of submitted trust certificates in one AD record. */
    public static final int MAX_ANCHORS = 16;
    /** Maximum UTF-8 upload size or stored Base64 length for one certificate. */
    public static final int MAX_INPUT_BYTES = 128 * 1024;
    /** Maximum decoded DER size for one certificate. */
    public static final int MAX_DER_BYTES = 64 * 1024;
    /** Maximum combined decoded DER size for one AD record. */
    public static final int MAX_RECORD_DER_BYTES = 512 * 1024;
    private static final Pattern PEM = Pattern.compile(
        "-----BEGIN CERTIFICATE-----\\s+([A-Za-z0-9+/=\\r\\n \\t]+)-----END CERTIFICATE-----");

    private String kind;
    private String label;
    private String certificateDerBase64;

    /** Construct an empty record for settings deserialization. */
    public LdapsTrustAnchor() { }

    /**
     * Copy a trust record without sharing mutable settings objects.
     *
     * @param other Trust record to copy.
     */
    public LdapsTrustAnchor(LdapsTrustAnchor other)
    {
        kind = other.kind;
        label = other.label;
        certificateDerBase64 = other.certificateDerBase64;
    }

    /** @return The enrolled certificate kind: CA or EXACT_SELF_SIGNED_SERVER. */
    public String getKind() { return kind; }
    /** @param value Certificate kind to validate at the settings boundary. */
    public void setKind(String value) { kind = value; }
    /** @return The administrator's display label. */
    public String getLabel() { return label; }
    /** @param value Display label to validate at the settings boundary. */
    public void setLabel(String value) { label = value; }
    /** @return The public certificate encoded as Base64 DER. */
    public String getCertificateDerBase64() { return certificateDerBase64; }
    /** @param value Base64 DER certificate to validate at the settings boundary. */
    public void setCertificateDerBase64(String value) { certificateDerBase64 = value; }

    /**
     * Accept one PEM certificate or Base64 DER at the upload boundary.
     * Reject malformed, multiple, oversized, or unsuitable certificate inputs.
     *
     * @param kind Required enrollment kind.
     * @param label Administrator's display label.
     * @param input One PEM certificate or Base64 DER value.
     * @return A validated trust record with canonical Base64 DER.
     */
    public static LdapsTrustAnchor fromInput(String kind, String label, String input)
    {
        if (input == null) throw invalid("certificate is required");
        if (input.length() > MAX_INPUT_BYTES ||
            input.getBytes(StandardCharsets.UTF_8).length > MAX_INPUT_BYTES) {
            throw invalid("input size limit exceeded");
        }
        String encoded = input.trim();
        if (encoded.startsWith("-----")) {
            Matcher matcher = PEM.matcher(encoded);
            if (!matcher.matches()) throw invalid("expected exactly one PEM certificate");
            encoded = matcher.group(1).replaceAll("\\s", "");
        }
        LdapsTrustAnchor anchor = new LdapsTrustAnchor();
        anchor.kind = kind;
        anchor.label = label;
        anchor.certificateDerBase64 = encoded;
        return anchor.canonicalize();
    }

    /**
     * Validate the label and certificate's enrollment type and return a canonical copy.
     * The connection policy must check current certificate validity; settings loading does not check it.
     *
     * @return A canonical independent trust record.
     */
    LdapsTrustAnchor canonicalize()
    {
        if (label == null) throw invalid("label is required");
        if (label.codePointCount(0, label.length()) > 128) throw invalid("label length limit exceeded");
        X509Certificate certificate = certificate();
        String reason = "unsupported certificate type";
        try {
            if ("CA".equals(kind)) {
                boolean[] usage = certificate.getKeyUsage();
                reason = "CA certificate required";
                if (certificate.getBasicConstraints() < 0) throw invalid(reason);
                reason = "CA certificate must allow certificate signing";
                if (usage != null && (usage.length <= 5 || !usage[5])) throw invalid(reason);
            } else if ("EXACT_SELF_SIGNED_SERVER".equals(kind)) {
                reason = "self-signed server certificate required";
                if (certificate.getBasicConstraints() >= 0 ||
                    !certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) throw invalid(reason);
                reason = "self-signed certificate signature is invalid";
                certificate.verify(certificate.getPublicKey());
                // Check current validity when building/using the Jira 2 connection policy.
                // An enrolled certificate expiring must not invalidate RADIUS/API settings at startup.
                reason = "server authentication usage is required";
                List<String> eku = certificate.getExtendedKeyUsage();
                if (eku != null && !eku.contains("1.3.6.1.5.5.7.3.1") && !eku.contains("2.5.29.37.0")) throw invalid(reason);
                reason = "server key usage is not allowed";
                boolean[] usage = certificate.getKeyUsage();
                if (usage != null && !usage[0] && (usage.length <= 2 || !usage[2])) throw invalid(reason);
            } else {
                throw invalid(reason);
            }
            reason = "cannot encode certificate";
            LdapsTrustAnchor canonical = new LdapsTrustAnchor(this);
            canonical.certificateDerBase64 = Base64.getEncoder().encodeToString(certificate.getEncoded());
            return canonical;
        } catch (Exception e) {
            throw invalid(reason);
        }
    }

    /**
     * Decode exactly one size-bounded X.509 certificate from this record.
     *
     * @return The parsed public certificate.
     */
    X509Certificate certificate()
    {
        if (certificateDerBase64 == null) throw invalid("certificate is required");
        if (certificateDerBase64.length() > MAX_INPUT_BYTES) throw invalid("encoded size limit exceeded");
        byte[] der;
        try {
            der = Base64.getDecoder().decode(certificateDerBase64);
        } catch (IllegalArgumentException e) {
            throw invalid("malformed Base64 certificate");
        }
        if (der.length == 0) throw invalid("certificate is required");
        if (der.length > MAX_DER_BYTES) throw invalid("decoded size limit exceeded");
        try {
            return parseDer(der);
        } catch (Exception e) {
            throw invalid("malformed X.509 certificate");
        }
    }

    /**
     * Parse canonical certificate DER, rejecting trailing bytes or alternate encodings.
     *
     * @param der Complete DER encoding of one X.509 certificate.
     * @return The parsed certificate.
     * @throws Exception If parsing fails or the encoding is not exactly one canonical certificate.
     */
    static X509Certificate parseDer(byte[] der) throws Exception
    {
        ByteArrayInputStream input = new ByteArrayInputStream(der);
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        if (input.available() != 0 || !Arrays.equals(der, certificate.getEncoded())) throw invalid("expected exactly one DER certificate");
        return certificate;
    }

    /**
     * Derived information is returned separately and is never trusted as settings input.
     *
     * @return Subject, issuer, validity timestamps, fingerprint, and enrollment kind.
     */
    public Map<String, String> describe()
    {
        X509Certificate certificate = canonicalize().certificate();
        Map<String, String> metadata = new LinkedHashMap<>();
        try {
            metadata.put("subject", certificate.getSubjectX500Principal().getName());
            metadata.put("issuer", certificate.getIssuerX500Principal().getName());
            metadata.put("notBefore", certificate.getNotBefore().toInstant().toString());
            metadata.put("notAfter", certificate.getNotAfter().toInstant().toString());
            metadata.put("sha256", fingerprint(certificate.getEncoded()));
            metadata.put("kind", kind);
            return metadata;
        } catch (Exception e) {
            throw invalid("cannot read certificate metadata");
        }
    }

    /**
     * Compute a stable lowercase hexadecimal SHA-256 fingerprint.
     *
     * @param bytes Bytes to fingerprint.
     * @return The 64-character hexadecimal fingerprint.
     * @throws Exception If the SHA-256 digest cannot be computed.
     */
    static String fingerprint(byte[] bytes) throws Exception
    {
        StringBuilder result = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format("%02x", b & 0xff));
        return result.toString();
    }

    /**
     * Create a validation error without including submitted certificate contents.
     *
     * @param reason A fixed diagnostic reason that contains no submitted certificate content.
     * @return A trust-record validation exception with a bounded reason.
     */
    private static IllegalArgumentException invalid(String reason)
    {
        return new IllegalArgumentException("Invalid LDAPS trust certificate: " + reason);
    }

    /** @return This trust record serialized as settings JSON. */
    public String toJSONString() { return new JSONObject(this).toString(); }
}
