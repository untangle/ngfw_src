/**
 * Directory Connector LDAPS settings validation and migration helpers.
 */
package com.untangle.app.directory_connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Directory Connector-only trust settings validation; contains no network or global-store writes. */
final class LdapsSettings
{
    /** Prevent construction of this settings-validation utility. */
    private LdapsSettings() { }

    /**
     * Reject absent settings and schema versions outside the supported range.
     *
     * @param settings Settings whose schema version must be supported.
     */
    static void requireSupportedVersion(DirectoryConnectorSettings settings)
    {
        if (settings == null || settings.getVersion() < 1 ||
            settings.getVersion() > DirectoryConnectorSettings.CURRENT_SETTINGS_VERSION) {
            throw new IllegalArgumentException("Unsupported Directory Connector settings version");
        }
    }

    /**
     * Called only by the startup migration, after the existing v1-v4 conversions.
     * Assign record identities and explicitly preserve existing LDAPS behavior.
     *
     * @param settings Version 4 settings to migrate in place.
     */
    static void migrateV4(DirectoryConnectorSettings settings)
    {
        if (settings.getVersion() != 4) throw new IllegalArgumentException("Expected Directory Connector settings version 4");
        for (ActiveDirectoryServer server : settings.getActiveDirectorySettings().getServers()) {
            if (server == null) throw new IllegalArgumentException("Missing AD server");
            server.setRecordId(UUID.randomUUID().toString());
            server.setCertificateValidationMode(server.getLDAPSecure() ? LdapsValidationMode.LEGACY_INSECURE.name() : null);
            server.setIncludePublicRoots(false);
            server.setLdapsTrustAnchors(Collections.emptyList());
        }
        settings.setVersion(DirectoryConnectorSettings.CURRENT_SETTINGS_VERSION);
    }

    /**
     * Persisted startup records may retain legacy; ordinary writes must match a published record ID.
     * Validate a complete independent candidate without changing the input or published settings.
     *
     * @param input Current-schema candidate settings.
     * @param previous Published settings used to authorize retained legacy identities, or null when none exist.
     * @param startup Whether the candidate was loaded from persisted settings during initialization.
     * @return A validated settings copy with canonical trust anchors and assigned record identities.
     */
    static DirectoryConnectorSettings normalize(DirectoryConnectorSettings input, DirectoryConnectorSettings previous, boolean startup)
    {
        if (input == null) throw new IllegalArgumentException("Directory Connector settings are required");
        requireSupportedVersion(input);
        if (input.getVersion() != DirectoryConnectorSettings.CURRENT_SETTINGS_VERSION) {
            throw new IllegalArgumentException("Current Directory Connector settings version is required");
        }
        if (input.getActiveDirectorySettings() == null) throw new IllegalArgumentException("Must provide settings for ActiveDirectory");
        if (input.getRadiusSettings() == null) throw new IllegalArgumentException("Must provide settings for RADIUS");
        if (input.getGoogleSettings() == null) throw new IllegalArgumentException("Must provide settings for Google");
        DirectoryConnectorSettings candidate = input.copy();
        Map<String, ActiveDirectoryServer> existing = new HashMap<>();
        if (previous != null) {
            for (ActiveDirectoryServer server : previous.getActiveDirectorySettings().getServers()) existing.put(server.getRecordId(), server);
        }
        Set<String> ids = new HashSet<>();
        int recordPosition = 0;
        for (ActiveDirectoryServer server : candidate.getActiveDirectorySettings().getServers()) {
            recordPosition++;
            if (server == null) throw new IllegalArgumentException("Missing AD server");
            String id = server.getRecordId();
            ActiveDirectoryServer old = existing.get(id);
            if (id == null) {
                if (startup) throw new IllegalArgumentException("Persisted AD record ID is required");
                server.setRecordId(UUID.randomUUID().toString());
            } else {
                try {
                    if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException();
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid AD record ID");
                }
                if (!startup && old == null) throw new IllegalArgumentException("Unknown AD record ID");
            }
            if (!ids.add(server.getRecordId())) throw new IllegalArgumentException("Duplicate AD record ID");
            if (server.getAzure() && !server.getLDAPSecure() && !startup &&
                (old == null || !old.getAzure() || old.getLDAPSecure())) {
                throw new IllegalArgumentException("Azure directory records require LDAPS");
            }
            if (!server.getLDAPSecure()) {
                server.setCertificateValidationMode(null);
                server.setIncludePublicRoots(false);
                server.setLdapsTrustAnchors(Collections.emptyList());
                continue;
            }
            LdapsValidationMode mode = LdapsValidationMode.parse(server.getCertificateValidationMode());
            if (mode == LdapsValidationMode.LEGACY_INSECURE && !startup &&
                (old == null || !old.getLDAPSecure() ||
                 !LdapsValidationMode.LEGACY_INSECURE.name().equals(old.getCertificateValidationMode()))) {
                throw new IllegalArgumentException("Only an existing legacy LDAPS record may retain Trust All");
            }
            normalizeAnchors(server, recordPosition);
            if (mode == LdapsValidationMode.LEGACY_INSECURE) server.setIncludePublicRoots(false);
            if (server.getEnabled() && mode == LdapsValidationMode.PKI &&
                !server.getIncludePublicRoots() && server.getLdapsTrustAnchors().isEmpty()) {
                throw new IllegalArgumentException("Enabled PKI records require a trust source");
            }
        }
        return candidate;
    }

    /**
     * Canonicalize and deduplicate trust certificates while enforcing per-record limits.
     *
     * @param server Candidate server whose trust-anchor list is replaced after validation.
     * @param recordPosition One-based position of the AD record, used only for safe input diagnostics.
     */
    private static void normalizeAnchors(ActiveDirectoryServer server, int recordPosition)
    {
        List<LdapsTrustAnchor> anchors = server.getLdapsTrustAnchors();
        String record = "AD record " + recordPosition;
        if (anchors.size() > LdapsTrustAnchor.MAX_ANCHORS) throw new IllegalArgumentException(record + ": too many LDAPS trust certificates");
        List<LdapsTrustAnchor> canonical = new ArrayList<>();
        int total = 0;
        Set<String> seen = new HashSet<>();
        int anchorPosition = 0;
        for (LdapsTrustAnchor anchor : anchors) {
            String position = record + ", trust anchor " + (++anchorPosition) + ": ";
            if (anchor == null) throw new IllegalArgumentException(position + "missing LDAPS trust certificate");
            LdapsTrustAnchor parsed;
            try {
                parsed = anchor.canonicalize();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(position + e.getMessage());
            }
            total += java.util.Base64.getDecoder().decode(parsed.getCertificateDerBase64()).length;
            if (total > LdapsTrustAnchor.MAX_RECORD_DER_BYTES) throw new IllegalArgumentException(position + "LDAPS trust certificates exceed record size limit");
            if (seen.add(parsed.getCertificateDerBase64())) canonical.add(parsed);
        }
        server.setLdapsTrustAnchors(canonical);
    }

}
