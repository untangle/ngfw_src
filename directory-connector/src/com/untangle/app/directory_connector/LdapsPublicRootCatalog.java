/**
 * Directory Connector's read-only Debian public-root catalog.
 */
package com.untangle.app.directory_connector;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable startup snapshot of enabled, Debian-package-owned public CAs only. */
public final class LdapsPublicRootCatalog
{
    private final boolean healthy;
    private final List<X509Certificate> certificates;
    private final String packageVersion;
    private final String fingerprint;
    private final String loadedAt;
    private final int excludedCount;
    private final String failureCategory;

    /**
     * Create an immutable snapshot and record its load time.
     *
     * @param healthy Whether the entire selected root source was loaded successfully.
     * @param certificates Accepted public CA certificates, copied into this snapshot.
     * @param version Installed ca-certificates package version, or null when unavailable.
     * @param fingerprint Digest of sorted accepted certificate fingerprints, or null on failure.
     * @param excluded Number of disabled or unowned source entries excluded from the catalog.
     * @param failureCategory Bounded source-failure category, or null for a healthy snapshot.
     */
    private LdapsPublicRootCatalog(boolean healthy, List<X509Certificate> certificates, String version, String fingerprint, int excluded, String failureCategory)
    {
        this.healthy = healthy;
        this.certificates = Collections.unmodifiableList(new ArrayList<>(certificates));
        packageVersion = version;
        this.fingerprint = fingerprint;
        loadedAt = Instant.now().toString();
        excludedCount = excluded;
        this.failureCategory = failureCategory;
    }

    /**
     * Load the configured package-owned roots from the appliance's standard Debian sources.
     *
     * @return A complete immutable catalog, or an unhealthy empty catalog on source failure.
     */
    static LdapsPublicRootCatalog load()
    {
        return load(Paths.get("/etc/ca-certificates.conf"), Paths.get("/usr/share/ca-certificates"),
                    Paths.get("/var/lib/dpkg/info/ca-certificates.list"), Paths.get("/var/lib/dpkg/status"));
    }

    /**
     * Load only enabled, package-owned CA certificates from the supplied sources.
     * A failure rejects the entire generation rather than exposing partial trust.
     *
     * @param config Enabled/disabled certificate configuration file.
     * @param rootDirectory Root directory containing Debian certificate files.
     * @param manifest ca-certificates package file manifest.
     * @param status Debian package status database.
     * @return The complete catalog or an unhealthy empty snapshot.
     */
    static LdapsPublicRootCatalog load(Path config, Path rootDirectory, Path manifest, Path status)
    {
        String version = null;
        int excluded = 0;
        String failureCategory = "SOURCE_UNREADABLE";
        try {
            version = installedVersion(status);
            if (version == null) {
                failureCategory = "PACKAGE_UNAVAILABLE";
                throw new IllegalArgumentException("Public-root package is unavailable");
            }
            Path root = rootDirectory.toAbsolutePath().normalize();
            Path realRoot = root.toRealPath();
            Set<String> owned = new HashSet<>(Files.readAllLines(manifest, StandardCharsets.UTF_8));
            Map<String, X509Certificate> accepted = new java.util.TreeMap<>();
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) continue;
                if (entry.startsWith("!")) { excluded++; continue; }
                failureCategory = "INVALID_PATH";
                Path relative = Paths.get(entry);
                if (relative.isAbsolute() || !relative.normalize().equals(relative) || entry.contains("\\")) {
                    throw new IllegalArgumentException("Invalid public-root path");
                }
                Path file = root.resolve(relative).normalize();
                if (!file.startsWith(root) || file.equals(root)) throw new IllegalArgumentException("Invalid public-root path");
                if (!owned.contains(file.toString())) { excluded++; continue; }
                failureCategory = "INVALID_SOURCE_FILE";
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !file.toRealPath().startsWith(realRoot) ||
                    Files.size(file) > LdapsTrustAnchor.MAX_INPUT_BYTES) throw new IllegalArgumentException("Invalid public-root file");
                byte[] input = Files.readAllBytes(file);
                failureCategory = "INVALID_CERTIFICATE";
                X509Certificate certificate;
                if (input.length > 0 && input[0] == '-') {
                    certificate = LdapsTrustAnchor.fromInput("CA", "public root", new String(input, StandardCharsets.US_ASCII)).certificate();
                } else {
                    certificate = LdapsTrustAnchor.parseDer(input);
                    LdapsTrustAnchor.fromInput("CA", "public root", java.util.Base64.getEncoder().encodeToString(input));
                }
                accepted.put(LdapsTrustAnchor.fingerprint(certificate.getEncoded()), certificate);
            }
            failureCategory = "EMPTY_CATALOG";
            if (accepted.isEmpty()) throw new IllegalArgumentException("Public-root catalog is empty");
            failureCategory = "CATALOG_BUILD_FAILED";
            String joined = String.join("\n", accepted.keySet());
            return new LdapsPublicRootCatalog(true, new ArrayList<>(accepted.values()), version,
                LdapsTrustAnchor.fingerprint(joined.getBytes(StandardCharsets.US_ASCII)), excluded, null);
        } catch (Exception e) {
            // Never use a partial catalog, JVM defaults, or a Trust-All substitute.
            if (e instanceof java.io.IOException || e instanceof SecurityException) failureCategory = "SOURCE_UNREADABLE";
            return new LdapsPublicRootCatalog(false, Collections.emptyList(), version, null, excluded, failureCategory);
        }
    }

    /**
     * Read the installed ca-certificates package version without invoking a package command.
     *
     * @param status Debian package status database.
     * @return Installed package version, or null if the package is absent or not installed.
     * @throws Exception If the package database cannot be read.
     */
    private static String installedVersion(Path status) throws Exception
    {
        try (BufferedReader reader = Files.newBufferedReader(status, StandardCharsets.UTF_8)) {
            boolean target = false;
            boolean installed = false;
            String version = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (target) return installed ? version : null;
                    installed = false;
                    version = null;
                } else if (line.startsWith("Package: ")) {
                    target = line.equals("Package: ca-certificates");
                } else if (target && line.equals("Status: install ok installed")) {
                    installed = true;
                } else if (target && line.startsWith("Version: ")) {
                    version = line.substring("Version: ".length()).trim();
                }
            }
            return target && installed ? version : null;
        }
    }

    /**
     * Report whether the selected root generation loaded completely.
     *
     * @return True when this snapshot contains a successfully loaded catalog.
     */
    public boolean isHealthy() { return healthy; }

    /**
     * Reject an unavailable catalog only when the record selects public roots.
     *
     * @param server Record whose public-root dependency is checked.
     */
    void requireAvailableFor(ActiveDirectoryServer server)
    {
        if (server.getIncludePublicRoots() && !healthy) {
            throw new IllegalStateException("LDAPS public-root catalog is unavailable");
        }
    }

    /**
     * Obtain the accepted certificates, rejecting an unhealthy generation.
     *
     * @return The unmodifiable list of public CA certificates.
     */
    List<X509Certificate> certificates()
    {
        if (!healthy) throw new IllegalStateException("LDAPS public-root catalog is unavailable");
        return certificates;
    }

    /**
     * Describe the loaded generation without returning certificate contents.
     *
     * @return Health, counts, package version, load time, catalog fingerprint, and a bounded failure category.
     */
    public Map<String, Object> describe()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthy", healthy);
        result.put("acceptedCount", certificates.size());
        result.put("excludedCount", excludedCount);
        result.put("packageVersion", packageVersion);
        result.put("loadedAt", loadedAt);
        result.put("sha256", fingerprint);
        result.put("failureCategory", failureCategory);
        return result;
    }
}
