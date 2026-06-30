import os
import tempfile
import unittest


MAX_VERSION_FILES = 20
DEVICES_FILE_NAME = "devices.js"

VERSION_PATTERN = "{base}-version-{timestamp}{ext}"

TIMESTAMPS = [
    "2026-01-01-000000.000",
    "2026-01-02-000000.000",
    "2026-01-03-000000.000",
    "2026-01-04-000000.000",
    "2026-01-05-000000.000",
    "2026-01-06-000000.000",
    "2026-01-07-000000.000",
    "2026-01-08-000000.000",
    "2026-01-09-000000.000",
    "2026-01-10-000000.000",
    "2026-01-11-000000.000",
    "2026-01-12-000000.000",
    "2026-01-13-000000.000",
    "2026-01-14-000000.000",
    "2026-01-15-000000.000",
    "2026-01-16-000000.000",
    "2026-01-17-000000.000",
    "2026-01-18-000000.000",
    "2026-01-19-000000.000",
    "2026-01-20-000000.000",
    "2026-01-21-000000.000",
    "2026-01-22-000000.000",
    "2026-01-23-000000.000",
    "2026-01-24-000000.000",
    "2026-01-25-000000.000",
]


def should_prune(file_name):
    """Check if this settings file is subject to version pruning."""
    return file_name.endswith(DEVICES_FILE_NAME)


def prune_old_versions(file_name, max_versions=MAX_VERSION_FILES):
    """
    Python equivalent of SettingsManagerImpl._pruneOldVersions().
    Prunes old versioned settings files, keeping only the most recent max_versions.
    Only called for devices.js (checked by caller via should_prune()).
    """
    base_file = os.path.basename(file_name)
    parent_dir = os.path.dirname(file_name)

    if not parent_dir or not os.path.isdir(parent_dir):
        return 0

    prefix = base_file + "-version-"

    version_files = sorted(
        f for f in os.listdir(parent_dir)
        if f.startswith(prefix)
    )

    if len(version_files) <= max_versions:
        return 0

    delete_count = len(version_files) - max_versions
    for f in version_files[:delete_count]:
        os.remove(os.path.join(parent_dir, f))

    return delete_count


def create_version_files(directory, base_name, ext, timestamps):
    """Helper to create mock versioned settings files."""
    created = []
    for ts in timestamps:
        name = VERSION_PATTERN.format(base=base_name, timestamp=ts, ext=ext)
        path = os.path.join(directory, name)
        with open(path, 'w') as f:
            f.write("test content for " + ts)
        created.append(name)
    return created


class TestSettingsVersionPruning(unittest.TestCase):

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.base_name = "devices.js"
        self.file_name = os.path.join(self.tmpdir, self.base_name)

    def tearDown(self):
        for f in os.listdir(self.tmpdir):
            os.remove(os.path.join(self.tmpdir, f))
        os.rmdir(self.tmpdir)

    def _list_version_files(self):
        prefix = self.base_name + "-version-"
        return sorted(
            f for f in os.listdir(self.tmpdir)
            if f.startswith(prefix)
        )

    def test_prune_over_limit(self):
        """When version count exceeds MAX_VERSION_FILES, oldest are deleted."""
        create_version_files(self.tmpdir, self.base_name, ".js", TIMESTAMPS[:25])
        self.assertEqual(len(self._list_version_files()), 25)

        deleted = prune_old_versions(self.file_name)

        remaining = self._list_version_files()
        self.assertEqual(len(remaining), MAX_VERSION_FILES)
        self.assertEqual(deleted, 5)
        for f in remaining:
            self.assertNotIn("2026-01-01", f)
            self.assertNotIn("2026-01-02", f)
            self.assertNotIn("2026-01-03", f)
            self.assertNotIn("2026-01-04", f)
            self.assertNotIn("2026-01-05", f)

    def test_no_prune_under_limit(self):
        """When version count is under MAX_VERSION_FILES, nothing is deleted."""
        create_version_files(self.tmpdir, self.base_name, ".js", TIMESTAMPS[:5])
        self.assertEqual(len(self._list_version_files()), 5)

        deleted = prune_old_versions(self.file_name)

        self.assertEqual(len(self._list_version_files()), 5)
        self.assertEqual(deleted, 0)

    def test_no_prune_at_limit(self):
        """When version count equals MAX_VERSION_FILES, nothing is deleted."""
        create_version_files(self.tmpdir, self.base_name, ".js", TIMESTAMPS[:MAX_VERSION_FILES])
        self.assertEqual(len(self._list_version_files()), MAX_VERSION_FILES)

        deleted = prune_old_versions(self.file_name)

        self.assertEqual(len(self._list_version_files()), MAX_VERSION_FILES)
        self.assertEqual(deleted, 0)

    def test_empty_directory(self):
        """No version files — should handle gracefully."""
        deleted = prune_old_versions(self.file_name)
        self.assertEqual(deleted, 0)

    def test_symlink_preserved(self):
        """The base symlink should not be touched during pruning."""
        create_version_files(self.tmpdir, self.base_name, ".js", TIMESTAMPS[:25])
        latest = VERSION_PATTERN.format(
            base=self.base_name, timestamp=TIMESTAMPS[24], ext=".js"
        )
        os.symlink(
            "./" + latest,
            self.file_name
        )
        self.assertTrue(os.path.islink(self.file_name))

        prune_old_versions(self.file_name)

        self.assertTrue(os.path.islink(self.file_name))
        self.assertTrue(os.path.exists(self.file_name))
        remaining = self._list_version_files()
        self.assertEqual(len(remaining), MAX_VERSION_FILES)
        self.assertIn(latest, remaining)

    def test_mixed_settings_files(self):
        """Version files for different settings should not interfere."""
        create_version_files(self.tmpdir, self.base_name, ".js", TIMESTAMPS[:25])
        create_version_files(self.tmpdir, "network.js", ".js", TIMESTAMPS[:10])

        prune_old_versions(self.file_name)

        devices_remaining = self._list_version_files()
        self.assertEqual(len(devices_remaining), MAX_VERSION_FILES)

        network_prefix = "network.js-version-"
        network_remaining = [
            f for f in os.listdir(self.tmpdir)
            if f.startswith(network_prefix)
        ]
        self.assertEqual(len(network_remaining), 10)

    def test_newest_files_kept(self):
        """Verify that the newest files are the ones kept after pruning."""
        create_version_files(self.tmpdir, self.base_name, ".js", TIMESTAMPS[:25])

        prune_old_versions(self.file_name)

        remaining = self._list_version_files()
        expected_timestamps = TIMESTAMPS[5:25]
        for ts in expected_timestamps:
            expected_name = VERSION_PATTERN.format(
                base=self.base_name, timestamp=ts, ext=".js"
            )
            self.assertIn(expected_name, remaining)

    def test_invalid_directory(self):
        """Non-existent directory should not raise."""
        deleted = prune_old_versions("/nonexistent/path/devices.js")
        self.assertEqual(deleted, 0)

    def test_only_devices_js_eligible(self):
        """Only devices.js should be eligible for pruning."""
        self.assertTrue(should_prune("/usr/share/untangle/settings/untangle-vm/devices.js"))
        self.assertFalse(should_prune("/usr/share/untangle/settings/untangle-vm/network.js"))
        self.assertFalse(should_prune("/usr/share/untangle/settings/untangle-vm/admin.js"))
        self.assertFalse(should_prune("/usr/share/untangle/settings/untangle-vm/events.js"))

    def test_non_devices_not_pruned(self):
        """Non-devices.js files should not be pruned even if over limit."""
        create_version_files(self.tmpdir, "network.js", ".js", TIMESTAMPS[:25])
        network_file = os.path.join(self.tmpdir, "network.js")

        self.assertFalse(should_prune(network_file))

        network_prefix = "network.js-version-"
        network_files = [
            f for f in os.listdir(self.tmpdir)
            if f.startswith(network_prefix)
        ]
        self.assertEqual(len(network_files), 25)


if __name__ == '__main__':
    unittest.main()
