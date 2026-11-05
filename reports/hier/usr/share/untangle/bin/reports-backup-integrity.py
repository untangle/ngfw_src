#!/usr/bin/python3

"""Create and verify authenticated Reports backup packages."""

import argparse
import datetime
import hashlib
import hmac
import json
import os
import re
import socket
import stat
import struct
import subprocess
import sys
import tempfile


MAGIC = b"NGFWRBK1"
HEADER = struct.Struct("!8sIQ")
TAG_SIZE = hashlib.sha256().digest_size
DOMAIN = b"untangle-reports-backup-v1\0"
FORMAT = "ngfw-reports-backup"
PACKAGE_VERSION = 1
ARCHIVE_FORMAT = "postgresql-custom"
MAX_MANIFEST_BYTES = 16 * 1024
KEY_CONFIG = "/etc/untangle/reports-backup-integrity.json"
PASSWORD_MANAGER = "/usr/bin/password-manager"
COPY_BUFFER_BYTES = 1024 * 1024

MANIFEST_FIELDS = {
    "archive_format",
    "archive_sha256",
    "archive_size",
    "created_utc",
    "format",
    "key_id",
    "package_version",
    "source_appliance",
    "source_pg_dump_version",
}


class IntegrityError(Exception):
    pass


def _log(message):
    """Write a timestamped, secret-free integrity-flow message to stderr."""
    timestamp = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    print(f"[{timestamp}] Reports backup integrity: {message}", file=sys.stderr, flush=True)


def _reject_constant(_value):
    """Reject nonstandard JSON constants such as NaN and Infinity."""
    raise IntegrityError("manifest contains an invalid JSON constant")


def _unique_object(pairs):
    """Build a JSON object while rejecting duplicate keys."""
    result = {}
    for key, value in pairs:
        if key in result:
            raise IntegrityError("manifest contains a duplicate field")
        result[key] = value
    return result


def _canonical_manifest(fields):
    """Serialize manifest fields using the package's canonical JSON encoding."""
    return json.dumps(
        fields, sort_keys=True, separators=(",", ":"), ensure_ascii=True
    ).encode("ascii")


def _validate_manifest(fields):
    """Validate the manifest schema, field types, sizes, and supported formats."""
    if type(fields) is not dict or set(fields) != MANIFEST_FIELDS:
        raise IntegrityError("manifest fields are invalid")
    if fields["format"] != FORMAT or type(fields["format"]) is not str:
        raise IntegrityError("unsupported Reports backup package format")
    if type(fields["package_version"]) is not int or fields["package_version"] != PACKAGE_VERSION:
        raise IntegrityError("unsupported Reports backup package version")
    if fields["archive_format"] != ARCHIVE_FORMAT or type(fields["archive_format"]) is not str:
        raise IntegrityError("unsupported Reports archive format")
    if type(fields["archive_size"]) is not int or fields["archive_size"] < 1:
        raise IntegrityError("archive size is invalid")
    if type(fields["archive_sha256"]) is not str or not re.fullmatch(r"[0-9a-f]{64}", fields["archive_sha256"]):
        raise IntegrityError("archive digest is invalid")
    if type(fields["key_id"]) is not str or not re.fullmatch(r"[A-Za-z0-9._-]{1,64}", fields["key_id"]):
        raise IntegrityError("key identifier is invalid")
    if type(fields["source_appliance"]) is not str or not re.fullmatch(r"[A-Za-z0-9_.-]{1,255}", fields["source_appliance"]):
        raise IntegrityError("source appliance metadata is invalid")
    if type(fields["source_pg_dump_version"]) is not str or not re.fullmatch(r"pg_dump \(PostgreSQL\) [A-Za-z0-9()._ +:-]{1,120}", fields["source_pg_dump_version"]):
        raise IntegrityError("PostgreSQL backup metadata is invalid")
    if type(fields["created_utc"]) is not str or not re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", fields["created_utc"]):
        raise IntegrityError("backup timestamp is invalid")
    try:
        datetime.datetime.strptime(fields["created_utc"], "%Y-%m-%dT%H:%M:%SZ")
    except ValueError:
        raise IntegrityError("backup timestamp is invalid")
    return fields


def _load_key(expected_key_id=None):
    """Load a protected local key and decrypt the active or requested key ID."""
    flags = os.O_RDONLY
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    try:
        fd = os.open(KEY_CONFIG, flags)
    except OSError:
        raise IntegrityError("Reports backup integrity key is not provisioned")
    try:
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode) or info.st_uid != 0 or info.st_mode & 0o077:
            raise IntegrityError("Reports backup integrity key configuration is not protected")
        if info.st_size <= 0 or info.st_size > 16 * 1024:
            raise IntegrityError("Reports backup integrity key configuration is invalid")
        with os.fdopen(fd, "rb") as config_file:
            fd = -1
            raw = config_file.read(16 * 1024 + 1)
    finally:
        if fd >= 0:
            os.close(fd)

    try:
        config = json.loads(raw.decode("ascii"), object_pairs_hook=_unique_object,
                            parse_constant=_reject_constant)
    except (UnicodeError, ValueError):
        raise IntegrityError("Reports backup integrity key configuration is invalid")
    if type(config) is not dict or set(config) != {"active_key_id", "keys"}:
        raise IntegrityError("Reports backup integrity key configuration is invalid")
    active_key_id = config["active_key_id"]
    keys = config["keys"]
    if type(active_key_id) is not str or not re.fullmatch(r"[A-Za-z0-9._-]{1,64}", active_key_id):
        raise IntegrityError("Reports backup integrity key identifier is invalid")
    if type(keys) is not dict or not (1 <= len(keys) <= 8) or active_key_id not in keys:
        raise IntegrityError("Reports backup integrity key configuration is invalid")
    if any(type(key_id) is not str or not re.fullmatch(r"[A-Za-z0-9._-]{1,64}", key_id)
           for key_id in keys):
        raise IntegrityError("Reports backup integrity key identifier is invalid")
    if any(type(value) is not str or not value or "\n" in value or "\r" in value
           for value in keys.values()):
        raise IntegrityError("Reports backup integrity key configuration is invalid")
    key_id = active_key_id if expected_key_id is None else expected_key_id
    if key_id not in keys:
        raise IntegrityError("backup uses an unknown Reports integrity key")
    encrypted_key = keys[key_id]
    try:
        completed = subprocess.run(
            [PASSWORD_MANAGER, "-d", encrypted_key],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            check=False,
            timeout=30,
        )
    except (OSError, subprocess.SubprocessError):
        raise IntegrityError("Reports backup integrity key could not be decrypted")
    if completed.returncode != 0:
        raise IntegrityError("Reports backup integrity key could not be decrypted")
    try:
        # password-manager writes a status line followed by the result, matching
        # PasswordUtil.getDecryptPassword()'s established output contract.
        lines = completed.stdout.decode("ascii").splitlines()
        encoded_key = lines[1].strip()
        if not re.fullmatch(r"[0-9a-fA-F]{64,128}", encoded_key) or len(encoded_key) % 2:
            raise ValueError()
        key = bytearray.fromhex(encoded_key)
    except (UnicodeError, IndexError, ValueError):
        raise IntegrityError("Reports backup integrity key has an invalid encoding")
    if len(key) < 32:
        raise IntegrityError("Reports backup integrity key is too short")
    return key_id, key


def _zero_key(key):
    """Overwrite the mutable in-memory buffer holding an HMAC key."""
    for index in range(len(key)):
        key[index] = 0


def _open_regular(path):
    """Open a regular file without following a final-component symlink."""
    flags = os.O_RDONLY
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    try:
        fd = os.open(path, flags)
    except OSError:
        raise IntegrityError("backup input cannot be opened")
    info = os.fstat(fd)
    if not stat.S_ISREG(info.st_mode):
        os.close(fd)
        raise IntegrityError("backup input must be a regular file")
    return os.fdopen(fd, "rb")


def _hash_stream(source, output=None):
    """Hash a stream of any size and optionally copy it to another stream."""
    digest = hashlib.sha256()
    size = 0
    while True:
        chunk = source.read(COPY_BUFFER_BYTES)
        if not chunk:
            break
        size += len(chunk)
        digest.update(chunk)
        if output is not None:
            output.write(chunk)
    return digest.hexdigest(), size


def _source_metadata():
    """Return sanitized source appliance and pg_dump version metadata."""
    hostname = socket.gethostname()
    hostname = re.sub(r"[^A-Za-z0-9_.-]", "_", hostname)[:255] or "unknown"
    try:
        completed = subprocess.run(
            ["pg_dump", "--version"], stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, check=True, timeout=10,
        )
        version = completed.stdout.decode("ascii").strip()
    except (OSError, UnicodeError, subprocess.SubprocessError):
        raise IntegrityError("could not read PostgreSQL backup metadata")
    return hostname, version


def _atomic_temp(destination):
    """Create a mode-restricted temporary output beside its final destination."""
    parent = os.path.dirname(os.path.abspath(destination))
    basename = os.path.basename(destination)
    if not basename:
        raise IntegrityError("output path is invalid")
    try:
        fd, temporary = tempfile.mkstemp(prefix="." + basename + ".", suffix=".tmp", dir=parent)
    except OSError:
        raise IntegrityError("could not create private Reports backup output")
    os.fchmod(fd, 0o600)
    return fd, temporary


def create_package(archive_path, package_path):
    """Authenticate an unchanged custom-format archive into an atomic package."""
    _log("create: validating output path and loading local key")
    if not package_path.lower().endswith(".rbackup"):
        raise IntegrityError("package output must end with .rbackup")
    if os.path.abspath(archive_path) == os.path.abspath(package_path):
        raise IntegrityError("archive and package paths must be different")
    key_id, key = _load_key()
    _log(f"create: local key loaded (key ID: {key_id})")
    try:
        _log("create: opening and hashing custom-format archive")
        with _open_regular(archive_path) as archive:
            digest, size = _hash_stream(archive)
            if size == 0:
                raise IntegrityError("Reports archive is empty")
            _log(f"create: archive hash complete ({size} bytes)")
            hostname, pg_dump_version = _source_metadata()
            fields = {
                "archive_format": ARCHIVE_FORMAT,
                "archive_sha256": digest,
                "archive_size": size,
                "created_utc": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
                "format": FORMAT,
                "key_id": key_id,
                "package_version": PACKAGE_VERSION,
                "source_appliance": hostname,
                "source_pg_dump_version": pg_dump_version,
            }
            manifest = _canonical_manifest(_validate_manifest(fields))
            if len(manifest) > MAX_MANIFEST_BYTES:
                raise IntegrityError("Reports backup manifest exceeds the size limit")
            tag = hmac.new(key, DOMAIN + manifest, hashlib.sha256).digest()
            archive.seek(0)
            _log("create: writing authenticated package")
            fd, temporary = _atomic_temp(package_path)
            try:
                with os.fdopen(fd, "wb") as package:
                    package.write(HEADER.pack(MAGIC, len(manifest), size))
                    package.write(manifest)
                    package.write(tag)
                    copied_digest, copied_size = _hash_stream(archive, package)
                    if copied_digest != digest or copied_size != size:
                        raise IntegrityError("Reports archive changed while it was packaged")
                    package.flush()
                    os.fsync(package.fileno())
                os.replace(temporary, package_path)
                directory_fd = os.open(os.path.dirname(os.path.abspath(package_path)), os.O_RDONLY)
                try:
                    os.fsync(directory_fd)
                finally:
                    os.close(directory_fd)
                _log("create: package published successfully")
            except Exception:
                try:
                    os.unlink(temporary)
                except OSError:
                    pass
                raise
    finally:
        _zero_key(key)


def _read_manifest(raw):
    """Parse a manifest strictly and require its canonical byte representation."""
    try:
        manifest_text = raw.decode("ascii")
        fields = json.loads(manifest_text, object_pairs_hook=_unique_object,
                            parse_constant=_reject_constant)
    except (UnicodeError, ValueError):
        raise IntegrityError("Reports backup manifest is malformed")
    _validate_manifest(fields)
    if _canonical_manifest(fields) != raw:
        raise IntegrityError("Reports backup manifest is not canonical")
    return fields


def verify_package(package_path, archive_path):
    """Authenticate a package and atomically materialize its verified archive."""
    _log("verify: opening package")
    if not archive_path.lower().endswith(".pgdump"):
        raise IntegrityError("verified archive output must end with .pgdump")
    if os.path.lexists(archive_path):
        raise IntegrityError("verified archive output already exists")

    with _open_regular(package_path) as package:
        _log("verify: package opened; validating fixed envelope")
        package_stat = os.fstat(package.fileno())
        if package_stat.st_size < HEADER.size + TAG_SIZE:
            raise IntegrityError("Reports backup package is truncated")
        header = package.read(HEADER.size)
        try:
            magic, manifest_size, archive_size = HEADER.unpack(header)
        except struct.error:
            raise IntegrityError("Reports backup package header is malformed")
        if magic != MAGIC or not (1 <= manifest_size <= MAX_MANIFEST_BYTES):
            raise IntegrityError("unsupported or malformed Reports backup package")
        if archive_size < 1:
            raise IntegrityError("Reports archive size is invalid")
        expected_total = HEADER.size + manifest_size + TAG_SIZE + archive_size
        if package_stat.st_size != expected_total:
            raise IntegrityError(
                f"Reports backup package length is invalid (expected {expected_total}, found {package_stat.st_size})")
        manifest_bytes = package.read(manifest_size)
        if len(manifest_bytes) != manifest_size:
            raise IntegrityError("Reports backup manifest is truncated")
        fields = _read_manifest(manifest_bytes)
        if fields["archive_size"] != archive_size:
            raise IntegrityError(
                f"manifest archive length does not match package (manifest {fields['archive_size']}, envelope {archive_size})")
        _log(f"verify: envelope and canonical manifest valid (key ID: {fields['key_id']})")
        tag = package.read(TAG_SIZE)
        if len(tag) != TAG_SIZE:
            raise IntegrityError("Reports backup authenticator is truncated")

        _log(f"verify: loading local verification key (key ID: {fields['key_id']})")
        key_id, key = _load_key(fields["key_id"])
        _log(f"verify: local key decrypted successfully (key ID: {key_id})")
        try:
            expected_tag = hmac.new(key, DOMAIN + manifest_bytes, hashlib.sha256).digest()
            if not hmac.compare_digest(expected_tag, tag):
                _log("verify: HMAC comparison failed")
                raise IntegrityError("Reports backup authentication failed")
            _log("verify: HMAC comparison passed")

            _log("verify: streaming archive to private staging and checking digest")
            fd, temporary = _atomic_temp(archive_path)
            try:
                with os.fdopen(fd, "wb") as verified:
                    digest, size = _hash_stream(package, verified)
                    if size != archive_size or digest != fields["archive_sha256"]:
                        raise IntegrityError("Reports archive digest or length does not match")
                    _log("verify: archive length and digest match")
                    verified.flush()
                    os.fsync(verified.fileno())
                os.chmod(temporary, 0o400)
                os.replace(temporary, archive_path)
                directory_fd = os.open(os.path.dirname(os.path.abspath(archive_path)), os.O_RDONLY)
                try:
                    os.fsync(directory_fd)
                finally:
                    os.close(directory_fd)
                _log("verify: verified archive published successfully")
            except Exception:
                try:
                    os.unlink(temporary)
                except OSError:
                    pass
                raise
        finally:
            _zero_key(key)


def main():
    """Parse the create or verify command and return its process exit status."""
    parser = argparse.ArgumentParser(description="Create or verify a Reports backup integrity package")
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("create", help="authenticate and package a custom-format archive")
    create.add_argument("--archive", required=True)
    create.add_argument("--output", required=True)
    verify = commands.add_parser("verify", help="authenticate a package and materialize its archive")
    verify.add_argument("--package", required=True)
    verify.add_argument("--output", required=True)
    args = parser.parse_args()

    try:
        if args.command == "create":
            create_package(args.archive, args.output)
        else:
            verify_package(args.package, args.output)
    except IntegrityError as error:
        _log(f"{args.command}: failed: {error}")
        print("ERROR: " + str(error), file=sys.stderr)
        return 2
    except OSError:
        _log(f"{args.command}: failed due to an I/O error")
        print("ERROR: I/O error while processing Reports backup package", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
