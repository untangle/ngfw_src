#!/usr/bin/env python3
"""
SafeType production-data sweep tool for NGFW-15768.

Walks settings JSON (from a .backup archive, a directory of backups, an
already-extracted settings tree, or live RPC getSettings calls), identifies
every annotated @SafeCheck field via the javaClass discriminator, and
validates each value against the production SafeCheckValidator via the
safeCheckTool.validate(...) RPC method.

See docs/ngfw-15768-production-data-sweep-plan.md for the operational
sweep methodology.
"""

import argparse
import csv
import datetime
import hashlib
import json
import logging
import re
import sys
import tarfile
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Set, Tuple

logger = logging.getLogger("safecheck-sweep")

# ---------------------------------------------------------------------------
# Inventory scanner: parse @SafeCheck annotations from Java sources
# ---------------------------------------------------------------------------

@dataclass
class FieldRule:
    field_name: str
    safetypes: List[str]
    allow: List[str]
    is_generic_mirror: bool = False


PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
IMPORT_RE = re.compile(r"^\s*import\s+(?:static\s+)?([\w.*]+)\s*;", re.MULTILINE)

# Tolerant @SafeCheck-on-field scanner.
# Matches: @SafeCheck(...) [other-annotations]* [modifiers] String fieldName
# Uses DOTALL so multi-line annotation arg lists work.
SAFECHECK_FIELD_RE = re.compile(
    r"@SafeCheck\s*(?:\(\s*(?P<args>.*?)\s*\))?"      # @SafeCheck() or @SafeCheck(args)
    r"(?P<between>(?:\s*@\w+(?:\s*\([^)]*\))?)*)"     # zero or more interleaved annotations
    r"\s*(?:public|protected|private)?"
    r"(?:\s+(?:static|final|transient|volatile))*"
    r"\s+String\s+(?P<field>\w+)\s*[;=]",
    re.DOTALL,
)

CLASS_DECL_RE = re.compile(
    r"\b(?:public\s+|protected\s+|private\s+|static\s+|final\s+|abstract\s+)*"
    r"(?:class|interface|enum)\s+(\w+)"
    r"(?:\s+extends\s+(\w+))?"
)

SAFETYPE_NAME_RE = re.compile(r"SafeType\.(\w+)")
ALLOW_LITERAL_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')


def parse_annotation_args(args: str) -> Tuple[List[str], List[str]]:
    """Parse the inside of @SafeCheck(...) into (safetypes, allow_list)."""
    if not args:
        return ([], [])
    safetypes = SAFETYPE_NAME_RE.findall(args)
    allow: List[str] = []
    m = re.search(r"allow\s*=\s*\{([^}]*)\}", args, re.DOTALL)
    if m:
        allow = [s.encode("utf-8").decode("unicode_escape")
                 for s in ALLOW_LITERAL_RE.findall(m.group(1))]
    return (safetypes, allow)


def scan_java_source_for_safecheck(source_root: Path) -> Dict[str, List[FieldRule]]:
    """Walk source_root for .java files, parse @SafeCheck on String fields.

    Returns inventory keyed by '$'-joined FQN, including parent-class fields
    walked via simple-name extends resolution.
    """
    # Map[FQN, List[FieldRule]] for fields declared in each class
    own_fields: Dict[str, List[FieldRule]] = {}
    # Map[FQN, parent_FQN_or_None] for inheritance resolution
    parent_of: Dict[str, Optional[str]] = {}
    # Map[FQN, set of simple imported names] for parent FQN lookup
    imports_of: Dict[str, Set[str]] = {}
    # Map[FQN, package] for same-package parent fallback
    package_of: Dict[str, str] = {}

    for java_path in source_root.rglob("*.java"):
        # Skip vendor / build trees
        path_str = str(java_path)
        if "/build/" in path_str or "/downloads/" in path_str or "/dist/" in path_str:
            continue
        try:
            src = java_path.read_text(encoding="utf-8", errors="replace")
        except (OSError, UnicodeDecodeError) as ex:
            logger.debug("skip %s: %s", java_path, ex)
            continue
        if "@SafeCheck" not in src:
            continue

        pkg_m = PACKAGE_RE.search(src)
        package = pkg_m.group(1) if pkg_m else ""
        imports = {imp.rsplit(".", 1)[-1]: imp for imp in IMPORT_RE.findall(src)
                   if not imp.endswith(".*")}

        _scan_classes_in_file(src, package, imports,
                              own_fields, parent_of, imports_of, package_of)

    # Merge parent annotations into subclasses
    inventory: Dict[str, List[FieldRule]] = {}
    for fqn in own_fields.keys() | parent_of.keys():
        merged: List[FieldRule] = []
        seen_names: Set[str] = set()
        cur: Optional[str] = fqn
        visited: Set[str] = set()
        while cur and cur not in visited:
            visited.add(cur)
            for rule in own_fields.get(cur, []):
                if rule.field_name in seen_names:
                    continue
                seen_names.add(rule.field_name)
                merged.append(rule)
            parent_simple = parent_of.get(cur)
            if parent_simple is None:
                break
            cur = _resolve_parent_fqn(parent_simple, imports_of.get(fqn, set()),
                                      package_of.get(fqn, ""), own_fields.keys())
        if merged:
            inventory[fqn] = merged

    # Mark Generic-mirror duplicates
    for fqn, rules in inventory.items():
        if fqn.endswith("Generic"):
            for r in rules:
                r.is_generic_mirror = True
    return inventory


def _scan_classes_in_file(src: str, package: str, imports: Dict[str, str],
                          own_fields: Dict[str, List[FieldRule]],
                          parent_of: Dict[str, Optional[str]],
                          imports_of: Dict[str, Set[str]],
                          package_of: Dict[str, str]) -> None:
    """Track class/inner-class declarations + collect @SafeCheck fields per class."""
    # Walk tokens, maintain class-name stack via brace depth.
    class_stack: List[str] = []  # simple class names in nesting order
    brace_depth = 0
    class_brace_depths: List[int] = []  # brace depth when each class opened

    i = 0
    while i < len(src):
        c = src[i]
        if c == "{":
            brace_depth += 1
            i += 1
            continue
        if c == "}":
            brace_depth -= 1
            if class_stack and class_brace_depths and brace_depth < class_brace_depths[-1]:
                class_stack.pop()
                class_brace_depths.pop()
            i += 1
            continue
        # Skip strings and comments
        if c == '"':
            j = i + 1
            while j < len(src) and src[j] != '"':
                if src[j] == '\\':
                    j += 2
                else:
                    j += 1
            i = j + 1
            continue
        if c == '/' and i + 1 < len(src):
            if src[i + 1] == '/':
                end = src.find('\n', i)
                i = end + 1 if end != -1 else len(src)
                continue
            if src[i + 1] == '*':
                end = src.find('*/', i + 2)
                i = end + 2 if end != -1 else len(src)
                continue

        # Class declaration?
        class_m = CLASS_DECL_RE.match(src, i)
        if class_m and (i == 0 or not src[i - 1].isalnum()):
            cls_simple = class_m.group(1)
            parent_simple = class_m.group(2)
            class_stack.append(cls_simple)
            class_brace_depths.append(brace_depth + 1)  # opens at next '{'
            cur_fqn = _build_fqn(package, class_stack)
            parent_of[cur_fqn] = parent_simple
            imports_of[cur_fqn] = set(imports.keys())
            package_of[cur_fqn] = package
            i = class_m.end()
            continue

        # @SafeCheck on field?
        if src.startswith("@SafeCheck", i) and (i == 0 or not src[i - 1].isalnum() and src[i - 1] != '_'):
            ann_m = SAFECHECK_FIELD_RE.match(src, i)
            if ann_m and class_stack:
                safetypes, allow = parse_annotation_args(ann_m.group("args") or "")
                cur_fqn = _build_fqn(package, class_stack)
                own_fields.setdefault(cur_fqn, []).append(
                    FieldRule(ann_m.group("field"), safetypes, allow))
                i = ann_m.end()
                continue

        i += 1


def _build_fqn(package: str, class_stack: List[str]) -> str:
    """Join package + class nesting with '.' between package/class and '$' between inner classes."""
    if not class_stack:
        return package
    head = class_stack[0]
    tail = "$".join(class_stack[1:])
    base = f"{package}.{head}" if package else head
    return f"{base}${tail}" if tail else base


def _resolve_parent_fqn(parent_simple: str, imports: Set[str],
                        package: str, known_fqns: Iterable[str]) -> Optional[str]:
    """Resolve a parent class simple name to FQN via imports → same-package fallback."""
    # Imports map simple→full was lost; reconstruct from known_fqns + imports set
    # Try matching known FQNs that end with .<parent_simple>
    same_pkg = f"{package}.{parent_simple}" if package else parent_simple
    candidates = [fqn for fqn in known_fqns
                  if fqn == same_pkg or fqn.endswith("." + parent_simple)
                  or fqn.endswith("$" + parent_simple)]
    if not candidates:
        return None
    # Prefer same-package match
    for c in candidates:
        if c == same_pkg:
            return c
    return candidates[0]


# ---------------------------------------------------------------------------
# Backup extraction
# ---------------------------------------------------------------------------

def _safe_extractall(tar: tarfile.TarFile, dest: Path) -> None:
    """Defense-in-depth wrapper around extractall. Uses the 'data' filter
    on Python 3.12+ (rejects '..' path traversal, absolute paths, and
    other archive-side tricks). Falls back to plain extractall on older
    Python versions where the filter parameter doesn't exist.
    """
    if sys.version_info >= (3, 12):
        tar.extractall(dest, filter="data")
    else:
        tar.extractall(dest)


def extract_backup(backup_path: Path, dest: Path) -> Path:
    """Extract a .backup file's two-layer archive. Returns settings root inside dest."""
    with tarfile.open(backup_path, "r:gz") as outer:
        _safe_extractall(outer, dest)
    inner = next(dest.glob("files-*.tar.gz"), None)
    if inner is None:
        raise RuntimeError(f"no files-*.tar.gz inside {backup_path}")
    with tarfile.open(inner, "r:gz") as t:
        _safe_extractall(t, dest)
    return _find_settings_root(dest)


def _find_settings_root(base: Path) -> Path:
    """Find the .../usr/share/untangle/settings directory below base."""
    for p in base.rglob("settings"):
        if p.is_dir() and (p / "untangle-vm").is_dir():
            return p
    raise RuntimeError(f"no usr/share/untangle/settings dir found under {base}")


# ---------------------------------------------------------------------------
# Live file selection (apps.js → settings_<liveId>.js for each app)
# ---------------------------------------------------------------------------

JS_EXTENSIONS_TO_SKIP = (".png", ".crt", ".key", ".pem", ".pfx", ".jks", ".csr",
                        ".txt", ".old", ".attr")


# Historical-version filename pattern UVM writes on every save —
# e.g. "network.js-version-2025-08-20-161438.235.js". The current live
# file (network.js) is what we want; these are stale snapshots.
VERSIONED_JS_RE = re.compile(r"-version-\d{4}-\d{2}-\d{2}-")


def _is_live_uvm_file(p: Path) -> bool:
    """True if p is a current UVM settings file (not a historical -version- snapshot)."""
    return p.is_file() and not VERSIONED_JS_RE.search(p.name)


def select_settings_files(settings_root: Path) -> List[Path]:
    """Return list of .js files to walk: untangle-vm/*.js + each app's settings_<liveId>.js.

    Excludes the *-version-YYYY-MM-DD-*.js historical snapshots UVM
    writes on every save (mirroring ut-backup.sh:48's exclusion rule).
    Otherwise stale snapshots would be re-validated and inflate counts.
    """
    files: List[Path] = []
    uvm_dir = settings_root / "untangle-vm"
    if uvm_dir.is_dir():
        files.extend(sorted(p for p in uvm_dir.glob("*.js") if _is_live_uvm_file(p)))
    else:
        logger.warning("no untangle-vm/ under %s", settings_root)

    apps_js = uvm_dir / "apps.js"
    if not apps_js.is_file():
        logger.warning("apps.js missing; skipping per-app settings")
        return files

    try:
        apps_data = json.loads(apps_js.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError, UnicodeDecodeError) as ex:
        logger.warning("apps.js parse failed: %s; skipping per-app settings", ex)
        return files

    live_ids: Dict[str, int] = {}
    for entry in apps_data.get("apps", {}).get("list", []):
        name = entry.get("appName")
        app_id = entry.get("id")
        if name and app_id is not None:
            live_ids[name] = int(app_id)

    for app_name, app_id in live_ids.items():
        app_dir = settings_root / app_name
        if not app_dir.is_dir():
            logger.warning("app %s in apps.js but %s/ missing", app_name, app_name)
            continue
        live_file = app_dir / f"settings_{app_id}.js"
        if live_file.is_file():
            files.append(live_file)
        else:
            logger.warning("live file %s missing", live_file)
        for orphan in app_dir.glob("settings_*.js"):
            if orphan != live_file:
                logger.debug("skip orphan %s", orphan)
    return files


# ---------------------------------------------------------------------------
# JSON walker + per-value RPC validation
# ---------------------------------------------------------------------------

@dataclass
class SweepRow:
    """
    One row per (appliance_uid, pojo_class, field). Aggregates every
    instance of that field seen on the appliance: total count, rejected
    count, and the set of redacted shapes of rejected values. The
    appliance_uid field carries the actual UID from
    /usr/share/untangle/conf/uid (or the operator-supplied --uid),
    NOT a synthesized hash, so persisted rows trace back to the box.
    """
    appliance_uid: str
    pojo_class: str
    field: str
    safetypes: str
    allow_list: str
    is_generic_mirror: bool
    total_count: int = 0
    rejected_count: int = 0
    rejected_value_shapes: Set[str] = field(default_factory=set)


SECRET_FIELD_RE = re.compile(r"(?i).*(password|secret|passphrase|privatekey).*")


def redact(value: str, field_name: str, safetypes: List[str]) -> str:
    if SECRET_FIELD_RE.match(field_name) or "OPAQUE_SECRET" in safetypes:
        return "<secret>"
    out_chars = []
    for ch in value:
        if ch.isalpha():
            out_chars.append("a")
        elif ch.isdigit():
            out_chars.append("9")
        else:
            out_chars.append(ch)
    return "".join(out_chars)


class Walker:
    def __init__(self, appliance_uid: str, inventory: Dict[str, List[FieldRule]],
                 validator, rows: Dict[Tuple[str, str, str], SweepRow]):
        self.appliance_uid = appliance_uid
        self.inventory = inventory
        self.validator = validator
        # Aggregated by (appliance_uid, pojo_class, field). One SweepRow per
        # (class, field) per appliance — every instance of that field
        # encountered in the JSON contributes to its counters and shape set.
        self.rows = rows

    def walk_tree(self, root, root_path: str = "$") -> None:
        self._walk(root, root_path)

    def _walk(self, node, path: str) -> None:
        if isinstance(node, dict):
            java_class = node.get("javaClass")
            # java.* wrappers: descend into 'list' (or 'map'); don't look up in inventory
            if java_class and java_class.startswith("java."):
                if "list" in node and isinstance(node["list"], list):
                    for i, item in enumerate(node["list"]):
                        self._walk(item, f"{path}.list[{i}]")
                return
            if java_class and java_class in self.inventory:
                for rule in self.inventory[java_class]:
                    row_key = (self.appliance_uid, java_class, rule.field_name)
                    row = self.rows.get(row_key)
                    if row is None:
                        row = SweepRow(
                            appliance_uid=self.appliance_uid,
                            pojo_class=java_class,
                            field=rule.field_name,
                            safetypes="|".join(rule.safetypes) or "SIMPLE_TEXT",
                            allow_list=",".join(rule.allow),
                            is_generic_mirror=rule.is_generic_mirror,
                        )
                        self.rows[row_key] = row

                    value = node.get(rule.field_name)
                    if value is None or value == "":
                        continue
                    if not isinstance(value, str):
                        logger.warning("%s.%s: non-string JSON value %r — skipping",
                                       java_class, rule.field_name, type(value).__name__)
                        continue

                    result = self.validator.validate(value, rule.safetypes, rule.allow)
                    rejected = not result.startswith("OK")
                    shape = redact(value, rule.field_name, rule.safetypes)
                    row.total_count += 1
                    if rejected:
                        row.rejected_count += 1
                        row.rejected_value_shapes.add(shape)
            # Recurse into all dict children regardless of inventory hit
            for k, v in node.items():
                if k == "javaClass":
                    continue
                self._walk(v, f"{path}.{k}")
        elif isinstance(node, list):
            for i, item in enumerate(node):
                self._walk(item, f"{path}[{i}]")


# ---------------------------------------------------------------------------
# RPC client wrapper
# ---------------------------------------------------------------------------

class RpcValidator:
    """Thin wrapper around safeCheckTool.validate(...) RPC."""

    def __init__(self, uvm_context):
        self.tool = uvm_context.safeCheckTool()
        # Preflight: confirm the method exists on the target.
        try:
            probe = self.tool.validate("probe", ["ALPHANUM"], [])
        except Exception as ex:
            raise RuntimeError(
                "safeCheckTool RPC not available on target NGFW (need ngfw-release-17.5 "
                f"with NGFW-15768 applied): {ex}") from ex
        if probe != "OK":
            raise RuntimeError(f"safeCheckTool preflight failed: {probe!r}")

    def validate(self, value: str, safetypes: List[str], allow: List[str]) -> str:
        return self.tool.validate(value, safetypes, allow)


def build_rpc_validator(ngfw: str, username: Optional[str], password: Optional[str],
                        scheme: str):
    # Prepend the in-tree UVM client path so `from uvm import Uvm` resolves
    # when running outside the appliance (e.g. from a dev host). Disable
    # .pyc writes so we don't litter __pycache__ inside uvm/hier/ — the
    # build's text-filter rakefile chokes on binary .pyc files during copy.
    sys.dont_write_bytecode = True
    # Path is <repo_root>/uvm/hier/usr/lib/python3/dist-packages, where
    # repo_root is two parents up from this script (tools/safecheck-sweep/).
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent.parent
    uvm_pkg_dir = repo_root / "uvm" / "hier" / "usr" / "lib" / "python3" / "dist-packages"
    if uvm_pkg_dir.is_dir() and str(uvm_pkg_dir) not in sys.path:
        sys.path.insert(0, str(uvm_pkg_dir))
    from uvm import Uvm  # type: ignore

    is_localhost = ngfw in ("127.0.0.1", "localhost", "::1")
    if not is_localhost and not (username and password):
        raise SystemExit(
            f"--ngfw {ngfw} requires --username and --password (only localhost may omit them)")

    ctx = Uvm().getUvmContext(hostname=ngfw, username=username, password=password,
                              timeout=240, scheme=scheme)
    if ctx is None:
        if is_localhost and not username:
            raise SystemExit(
                "credential-less localhost RPC failed; re-invoke with --username/--password")
        raise SystemExit("RPC login failed")
    return RpcValidator(ctx)


# ---------------------------------------------------------------------------
# Live-mode JSON acquisition
# ---------------------------------------------------------------------------

# (rpc_path, expected_root_class_fqn) — verify at impl time per plan
LIVE_UVM_GETTERS = [
    ("networkManager.getNetworkSettings", "com.untangle.uvm.network.NetworkSettings"),
    ("systemManager.getSettings", "com.untangle.uvm.SystemSettings"),
    ("adminManager.getSettings", "com.untangle.uvm.AdminSettings"),
    ("mailSender.getSettings", "com.untangle.uvm.MailSettings"),
]


def acquire_live_trees(uvm_context) -> List[Tuple[str, dict]]:
    """Return list of (label, json_tree) from live RPC calls."""
    trees: List[Tuple[str, dict]] = []
    for path, _expected in LIVE_UVM_GETTERS:
        try:
            mgr_name, method = path.split(".")
            mgr = getattr(uvm_context, mgr_name)()
            tree = getattr(mgr, method)()
            trees.append((path, tree))
        except Exception as ex:
            logger.warning("live getter %s failed: %s", path, ex)
    # Per-app discovery via appManager.getAppsViews() → AppsView[] per policy.
    # Each view's `instances` is a LinkedList of AppSettings; each AppSettings has id + appName.
    try:
        app_mgr = uvm_context.appManager()
        views = app_mgr.getAppsViews()
        if views is None:
            logger.warning("getAppsViews returned None")
        else:
            seen_ids: Set[int] = set()
            for view in views:
                # Jabsorb LinkedList wire shape: {"javaClass": "...LinkedList", "list": [...]}
                instances_node = view.get("instances") if isinstance(view, dict) else None
                if isinstance(instances_node, dict):
                    instances = instances_node.get("list", [])
                elif isinstance(instances_node, list):
                    instances = instances_node
                else:
                    instances = []
                for inst in instances:
                    inst_id = inst.get("id") if isinstance(inst, dict) else None
                    if inst_id is None or inst_id in seen_ids:
                        continue
                    seen_ids.add(inst_id)
                    app_name = inst.get("appName", "?")
                    try:
                        app_obj = app_mgr.app(inst_id)
                        if app_obj is None:
                            continue
                        if hasattr(app_obj, "getSettings"):
                            settings = app_obj.getSettings()
                            if settings is not None:
                                trees.append((f"{app_name}[{inst_id}].getSettings", settings))
                    except Exception as ex:
                        logger.warning("app(%s/%s).getSettings failed: %s",
                                       app_name, inst_id, ex)
    except Exception as ex:
        logger.warning("appManager enumeration failed: %s", ex)
    return trees


# ---------------------------------------------------------------------------
# CSV emission
# ---------------------------------------------------------------------------

ROW_HEADER = ["pojo_class", "field", "safetypes", "allow_list",
              "rejected_value_shapes"]


@dataclass
class PersistentRow:
    """One row of the persistent results CSV, keyed externally by (pojo_class, field).

    rejected_value_shapes is keyed by appliance UID so the operator can
    trace which appliance contributed which redacted shape. Stored on
    disk as a JSON object {"uid-a": ["shape1", "shape2"], "uid-b": [...]}.
    """
    pojo_class: str
    field: str
    safetypes: str
    allow_list: str
    rejected_shapes_by_uid: Dict[str, Set[str]] = field(default_factory=dict)


LEGACY_HEADER_WITH_REASONS = ["pojo_class", "field", "safetypes", "allow_list",
                              "rejected_value_shapes", "reject_reasons"]


def load_persistent_csv(path: Path) -> Dict[Tuple[str, str], PersistentRow]:
    """Read an existing results CSV into a dict keyed by (pojo_class, field).

    Returns an empty dict if the file doesn't exist. Fails LOUDLY if the
    file's header doesn't match an expected schema (rather than silently
    treating it as empty and clobbering the user's data on next write).

    Migration:
      * Legacy header with 6 columns (pre-uid-keying, included
        reject_reasons): accepted; reject_reasons column dropped.
      * Legacy shape cell as a JSON array (pre-uid-keying): promoted to
        {"unknown": [...]} so prior findings are preserved with a marker
        that their source appliance was unrecorded.

    rejected_value_shapes is stored as a JSON object {uid: [shapes...]}
    so different appliances' contributions are visible per-row.
    """
    out: Dict[Tuple[str, str], PersistentRow] = {}
    if not path.is_file():
        return out
    with path.open("r", newline="", encoding="utf-8") as f:
        r = csv.reader(f)
        try:
            header = next(r)
        except StopIteration:
            return out
        if header == ROW_HEADER:
            legacy = False
        elif header == LEGACY_HEADER_WITH_REASONS:
            legacy = True
            logger.info("migrating legacy CSV (dropping reject_reasons column, "
                        "promoting array shapes to {\"unknown\": [...]})")
        else:
            raise SystemExit(
                f"Refusing to overwrite {path}: header {header!r} does not "
                f"match expected {ROW_HEADER!r} or legacy "
                f"{LEGACY_HEADER_WITH_REASONS!r}. Rename the file or pass --reset."
            )
        expected_cols = len(LEGACY_HEADER_WITH_REASONS if legacy else ROW_HEADER)
        for line_num, row in enumerate(r, start=2):
            if len(row) != expected_cols:
                logger.warning("%s line %d: skipping malformed row %r",
                               path, line_num, row)
                continue
            pojo, fld, st, al, shapes = row[:5]
            try:
                shapes_obj = json.loads(shapes) if shapes else {}
            except (json.JSONDecodeError, TypeError) as ex:
                raise SystemExit(
                    f"Failed to parse JSON in {path} line {line_num}: {ex}. "
                    f"Rename the file or pass --reset."
                )
            if isinstance(shapes_obj, list):
                # Pre-uid-keying schema: promote to {"unknown": [...]}.
                shapes_by_uid: Dict[str, Set[str]] = {"unknown": set(shapes_obj)}
            elif isinstance(shapes_obj, dict):
                shapes_by_uid = {k: set(v) for k, v in shapes_obj.items()}
            else:
                logger.warning("%s line %d: unexpected shape cell type %r; skipping",
                               path, line_num, type(shapes_obj).__name__)
                continue
            out[(pojo, fld)] = PersistentRow(
                pojo_class=pojo,
                field=fld,
                safetypes=st,
                allow_list=al,
                rejected_shapes_by_uid=shapes_by_uid,
            )
    return out


@dataclass
class CollapsedField:
    """Per-(class, field) aggregation across all appliances in one run.

    Keyed by appliance_uid so we know which appliance contributed which
    rejected shapes; merge_and_write unions this into the persistent CSV.
    """
    pojo_class: str
    field: str
    safetypes: str
    allow_list: str
    is_generic_mirror: bool
    total_count: int = 0
    rejected_count: int = 0
    rejected_shapes_by_uid: Dict[str, Set[str]] = field(default_factory=dict)


def collapse_by_field(rows: Dict[Tuple[str, str, str], SweepRow]
                      ) -> Dict[Tuple[str, str], CollapsedField]:
    """Group per-appliance SweepRows down to one entry per (pojo_class, field).

    rejected_shapes_by_uid retains the per-appliance breakdown so the
    persistent CSV can show which uid contributed which shape.
    safetypes / allow_list / is_generic_mirror are expected to be
    identical across appliances (they come from the source-code
    inventory) — first value wins.
    """
    out: Dict[Tuple[str, str], CollapsedField] = {}
    for (uid, cls, fld), r in rows.items():
        key = (cls, fld)
        agg = out.get(key)
        if agg is None:
            agg = CollapsedField(
                pojo_class=cls, field=fld,
                safetypes=r.safetypes, allow_list=r.allow_list,
                is_generic_mirror=r.is_generic_mirror,
            )
            out[key] = agg
        agg.total_count += r.total_count
        agg.rejected_count += r.rejected_count
        if r.rejected_value_shapes:
            agg.rejected_shapes_by_uid.setdefault(uid, set()).update(
                r.rejected_value_shapes)
    return out


def merge_and_write(persistent_path: Path,
                    current_rows: Dict[Tuple[str, str, str], SweepRow]) -> Tuple[int, int, int]:
    """Apply the accumulate-only merge rule, then atomically write.

    Persistent rows are NEVER auto-cleared. A clean run (no rejections)
    just leaves existing entries alone — the operator may be feeding
    intentionally-clean data in this run after an earlier run that found
    rejections, and we don't want that to erase the earlier evidence.

    For each (class, field) the current sweep produced rows for:
      * rejected_count > 0 → union new shapes/reasons into the row;
        refresh safetypes/allow_list from current source code.
      * rejected_count == 0 → no-op for that key.
    Use --reset to wipe the file explicitly.

    Returns (entries_after, cleared_count, updated_count). cleared_count
    is always 0 under this rule; kept for callsite compatibility.
    """
    existing = load_persistent_csv(persistent_path)
    collapsed = collapse_by_field(current_rows)

    cleared = 0
    updated = 0
    for key, cur in collapsed.items():
        if cur.rejected_count == 0:
            continue
        row = existing.get(key)
        if row is None:
            row = PersistentRow(
                pojo_class=cur.pojo_class, field=cur.field,
                safetypes=cur.safetypes, allow_list=cur.allow_list,
            )
            existing[key] = row
        else:
            # Refresh source-derived fields from latest run.
            row.safetypes = cur.safetypes
            row.allow_list = cur.allow_list
        # Union per-uid: extend each uid's set with new shapes seen this run.
        for uid, shapes in cur.rejected_shapes_by_uid.items():
            row.rejected_shapes_by_uid.setdefault(uid, set()).update(shapes)
        updated += 1

    # Atomic write: temp file in same dir, then rename.
    persistent_path.parent.mkdir(parents=True, exist_ok=True)
    tmp = persistent_path.with_suffix(persistent_path.suffix + ".tmp")
    with tmp.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, quoting=csv.QUOTE_MINIMAL)
        w.writerow(ROW_HEADER)
        for key in sorted(existing.keys()):
            r = existing[key]
            # JSON object {uid: [shapes...]} — preserves per-appliance
            # attribution and round-trip-safe under any redacted shape
            # (including '|', ',', '"', newlines).
            shapes_obj = {uid: sorted(shapes)
                          for uid, shapes in sorted(r.rejected_shapes_by_uid.items())}
            w.writerow([
                r.pojo_class, r.field, r.safetypes, r.allow_list,
                json.dumps(shapes_obj),
            ])
    tmp.replace(persistent_path)
    return (len(existing), cleared, updated)


def write_inventory(out_path: Path, inventory: Dict[str, List[FieldRule]]) -> None:
    with out_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, quoting=csv.QUOTE_MINIMAL)
        w.writerow(["pojo_class", "field", "safetypes", "allow_list", "is_generic_mirror"])
        for fqn in sorted(inventory.keys()):
            for r in inventory[fqn]:
                w.writerow([fqn, r.field_name,
                            "|".join(r.safetypes) or "SIMPLE_TEXT",
                            ",".join(r.allow), r.is_generic_mirror])


# ---------------------------------------------------------------------------
# UID resolution
# ---------------------------------------------------------------------------

def uid_from_filename(path: Path) -> str:
    """Derive a stable pseudo-uid from a backup filename, for --dir mode
    where each .backup is a different appliance and the operator can't
    realistically pass --uid per file. Prefixed so it's obvious in the CSV
    these aren't real /usr/share/untangle/conf/uid values."""
    return "file_" + hashlib.sha256(path.name.encode("utf-8")).hexdigest()[:10]


REQUIRED_TAIL_PARTS = ("usr", "share", "untangle", "settings")


def safely_remove_settings_tree(settings_root: Path) -> None:
    """Remove the parent untangle directory that fetch-settings.sh wrote to.

    Safety gates (any failure → SystemExit, nothing deleted):
      * settings_root must resolve to an existing absolute directory.
      * Its tail must be exactly .../usr/share/untangle/settings, so the
        peel-off math that derives <target> is structurally valid.
      * settings_root must contain untangle-vm/apps.js (sanity check that
        the path actually IS a settings tree, not just shaped like one).
      * The derived <target> (settings_root with the 4 trailing parts
        removed) must not be /, /usr, /tmp, /home, /var, /etc, /opt,
        or $HOME, and must have at least 3 path components — refuses
        delete-too-close-to-root mistakes.

    Logs the resolved target before deleting.
    """
    import shutil

    resolved = settings_root.resolve(strict=True)
    if not resolved.is_dir():
        raise SystemExit(f"--remove-after: {resolved} is not a directory")
    if resolved.parts[-4:] != REQUIRED_TAIL_PARTS:
        raise SystemExit(
            f"--remove-after: {resolved} does not end in "
            f"{'/'.join(REQUIRED_TAIL_PARTS)}; refusing because the "
            f"peel-off math that derives <target> assumes this layout")
    if not (resolved / "untangle-vm" / "apps.js").is_file():
        raise SystemExit(
            f"--remove-after: {resolved} does not contain untangle-vm/apps.js; "
            f"refusing to delete a path that doesn't look like a settings tree")
    target = resolved.parent.parent.parent.parent
    target_str = str(target)
    forbidden = {"/", "/usr", "/tmp", "/home", "/var", "/etc", "/opt",
                 str(Path.home())}
    if target_str in forbidden or len(target.parts) < 3:
        raise SystemExit(
            f"--remove-after: refusing to delete {target} "
            f"(too close to root or system directory)")
    logger.info("--remove-after: recursively removing %s", target)
    shutil.rmtree(target)


def uid_from_settings_dir(settings_root: Path) -> Optional[str]:
    """Read conf/uid from a fetch-settings.sh-style tree.

    Looks for <root>/conf/uid where <root> is the parent of the settings
    dir (since fetch-settings.sh extracts under <target>/usr/share/untangle/
    with both settings/ and conf/ as siblings). Returns None if not found.
    """
    # settings_root is .../usr/share/untangle/settings; conf/uid is at
    # .../usr/share/untangle/conf/uid (sibling of settings/).
    uid_file = settings_root.parent / "conf" / "uid"
    if uid_file.is_file():
        try:
            value = uid_file.read_text(encoding="utf-8").strip()
            if value:
                return value
        except (OSError, UnicodeDecodeError) as ex:
            logger.warning("could not read %s: %s", uid_file, ex)
    return None


# ---------------------------------------------------------------------------
# Per-input pipeline
# ---------------------------------------------------------------------------

def sweep_settings_root(appliance_uid: str, settings_root: Path,
                        inventory: Dict[str, List[FieldRule]],
                        validator: RpcValidator,
                        rows: Dict[Tuple[str, str, str], SweepRow]) -> None:
    files = select_settings_files(settings_root)
    walker = Walker(appliance_uid, inventory, validator, rows)
    for f in files:
        if any(f.name.endswith(ext) for ext in JS_EXTENSIONS_TO_SKIP):
            continue
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError, UnicodeDecodeError) as ex:
            logger.warning("skip %s: %s", f, ex)
            continue
        walker.walk_tree(data, f"$.{f.parent.name}/{f.name}")


def sweep_backup_file(backup_path: Path, appliance_uid: str,
                      inventory, validator, rows) -> None:
    logger.info("sweeping %s as uid=%s", backup_path, appliance_uid)
    with tempfile.TemporaryDirectory(prefix="safecheck-sweep-") as tmp:
        try:
            root = extract_backup(backup_path, Path(tmp))
        except Exception as ex:
            logger.error("extract %s failed: %s", backup_path, ex)
            return
        sweep_settings_root(appliance_uid, root, inventory, validator, rows)


def sweep_settings_dir(settings_path: Path, appliance_uid: str,
                       inventory, validator, rows) -> None:
    logger.info("sweeping %s as uid=%s", settings_path, appliance_uid)
    sweep_settings_root(appliance_uid, settings_path, inventory, validator, rows)


def sweep_live(uvm_context, appliance_uid: str, validator, inventory, rows) -> None:
    walker = Walker(appliance_uid, inventory, validator, rows)
    for label, tree in acquire_live_trees(uvm_context):
        if tree is None:
            continue
        walker.walk_tree(tree, f"$.{label}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

PERSISTENT_RESULTS_PATH = Path("/tmp/safecheck-sweep-results.csv")


def default_out(for_inventory: bool = False) -> Path:
    """Default output path. Sweep results are persistent (single fixed file
    that accumulates rejections across runs). --list-inventory uses a
    timestamped dump because each run is a one-off snapshot."""
    if for_inventory:
        stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M")
        return Path(f"/tmp/safecheck-inventory-{stamp}.csv")
    return PERSISTENT_RESULTS_PATH


def default_source_root() -> Path:
    script = Path(__file__).resolve()
    # tools/safecheck-sweep/test_safecheck_sweep.py → ngfw_src root
    return script.parent.parent.parent


def main(argv: Optional[List[str]] = None) -> int:
    description = (
        "NGFW-15768 SafeType production-data sweep.\n"
        "\n"
        "Walks settings JSON (from a .backup archive, a directory of backups,\n"
        "an already-extracted settings tree, or live RPC getSettings calls),\n"
        "identifies every @SafeCheck-annotated field via the javaClass\n"
        "discriminator, and validates each value against the production\n"
        "SafeCheckValidator via the safeCheckTool.validate(...) RPC.\n"
        "\n"
        "Output: PERSISTENT CSV at /tmp/safecheck-sweep-results.csv (a\n"
        "        single fixed file that accumulates across runs).\n"
        "        Each row = one (pojo_class, field) pair with at least one\n"
        "        rejected value ever seen. Entries are accumulate-only:\n"
        "        a clean run never erases prior evidence. Use --reset to\n"
        "        wipe the file explicitly when you want a fresh start."
    )
    epilog = (
        "Examples:\n"
        "  # Single backup archive — --uid required (backup doesn't carry it):\n"
        "  %(prog)s --file /path/to/customer.backup --uid abc-123-customer\n"
        "\n"
        "  # Batch sweep of 30-40 production backups (per-file uid from filename):\n"
        "  %(prog)s --dir /path/to/corpus --ngfw 192.168.56.5 \\\n"
        "      --username admin --password 'admin123'\n"
        "\n"
        "  # Already-extracted settings tree (uid auto-read from conf/uid):\n"
        "  %(prog)s --settings-dir /tmp/extracted/usr/share/untangle/settings \\\n"
        "      --ngfw 192.168.56.5 --username admin --password 'admin123'\n"
        "\n"
        "  # Same, plus auto-cleanup of the extracted tree after the sweep:\n"
        "  %(prog)s --settings-dir /tmp/extracted/usr/share/untangle/settings \\\n"
        "      --remove-after --ngfw 192.168.56.5 \\\n"
        "      --username admin --password 'admin123'\n"
        "\n"
        "  # Live mode — uid must be passed explicitly:\n"
        "  %(prog)s --live --uid <uid> --ngfw 192.168.56.5 \\\n"
        "      --username admin --password 'admin123'\n"
        "\n"
        "  # Dump the @SafeCheck inventory (offline, no NGFW needed):\n"
        "  %(prog)s --list-inventory\n"
        "\n"
        "Auth notes:\n"
        "  * --username / --password are mandatory for any non-localhost --ngfw.\n"
        "  * For 127.0.0.1 / localhost, credentials may be omitted (the\n"
        "    runtests-harness convention applies).\n"
        "  * Passwords appear in shell history and `ps aux` — acceptable for\n"
        "    dev appliances; rotate after use.\n"
        "\n"
        "Output columns:\n"
        "  pojo_class, field, safetypes, allow_list, rejected_value_shapes\n"
        "  Only rows with at least one rejection are emitted.\n"
        "  rejected_value_shapes is a JSON object {uid: [shape, ...]}\n"
        "  where uid identifies the contributing appliance (from its\n"
        "  /usr/share/untangle/conf/uid) and each shape is the redacted\n"
        "  form of a rejected value — alpha→'a', digit→'9', punctuation\n"
        "  preserved, secrets entirely masked.\n"
        "\n"
        "See docs/ngfw-15768-production-data-sweep-plan.md for the sweep\n"
        "methodology and triage rubric."
    )

    p = argparse.ArgumentParser(
        description=description,
        epilog=epilog,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    mode = p.add_mutually_exclusive_group(required=True)
    mode.add_argument("--file", type=Path, metavar="PATH",
                      help="single .backup archive to sweep")
    mode.add_argument("--dir", type=Path, metavar="PATH",
                      help="directory of .backup files (sweeps every *.backup found)")
    mode.add_argument("--settings-dir", type=Path, metavar="PATH",
                      help="already-extracted settings tree; path must contain "
                           "untangle-vm/apps.js and per-app subdirs")
    mode.add_argument("--live", action="store_true",
                      help="live mode: source JSON via RPC getSettings calls "
                           "instead of files (requires --ngfw + credentials)")
    mode.add_argument("--list-inventory", action="store_true",
                      help="dump parsed @SafeCheck inventory to CSV; no NGFW "
                           "needed (fully offline)")
    p.add_argument("--out", type=Path, default=None, metavar="PATH",
                   help="output CSV path (default for sweep modes: "
                        "/tmp/safecheck-sweep-results.csv [PERSISTENT — "
                        "accumulates across runs]; default for "
                        "--list-inventory: /tmp/safecheck-inventory-"
                        "<timestamp>.csv)")
    p.add_argument("--reset", action="store_true",
                   help="delete the persistent results CSV before this run "
                        "(useful after a major source-code refactor or to "
                        "clear stale state)")
    p.add_argument("--uid", default=None, metavar="UID",
                   help="appliance UID to key rejections under. Required "
                        "for --file and --live. For --settings-dir, used "
                        "only if <root>/conf/uid is missing. Ignored for "
                        "--dir (per-file uid is derived from filename).")
    p.add_argument("--remove-after", action="store_true",
                   help="ONLY valid with --settings-dir. After a successful "
                        "sweep, recursively remove the parent untangle "
                        "directory (the <target> passed to fetch-settings.sh). "
                        "Refuses if the path doesn't look like a settings "
                        "tree (must contain untangle-vm/apps.js).")
    p.add_argument("--ngfw", default="127.0.0.1", metavar="HOST",
                   help="target NGFW host for the validate RPC + live-mode "
                        "getSettings (default: 127.0.0.1)")
    p.add_argument("--username", default=None, metavar="USER",
                   help="admin username (mandatory for non-localhost --ngfw)")
    p.add_argument("--password", default=None, metavar="PASS",
                   help="admin password (mandatory for non-localhost --ngfw)")
    p.add_argument("--scheme", choices=("http", "https"), default="http",
                   help="scheme for the NGFW RPC endpoint (default: http)")
    p.add_argument("--source-root", type=Path, default=default_source_root(),
                   metavar="PATH",
                   help="Java source root for annotation scan "
                        "(default: derived from script location)")
    p.add_argument("--verbose", "-v", action="store_true",
                   help="enable DEBUG logging (orphan-skip lines, RPC details)")
    args = p.parse_args(argv)

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )

    logger.info("scanning @SafeCheck annotations under %s", args.source_root)
    inventory = scan_java_source_for_safecheck(args.source_root)
    field_count = sum(len(rs) for rs in inventory.values())
    logger.info("inventory: %d classes, %d annotated fields",
                len(inventory), field_count)

    if args.list_inventory:
        out = args.out or default_out(for_inventory=True)
        write_inventory(out, inventory)
        logger.info("wrote %s", out)
        return 0

    out = args.out or default_out()

    if args.reset and out.is_file():
        logger.info("--reset: removing existing %s", out)
        out.unlink()

    if args.remove_after and not args.settings_dir:
        raise SystemExit("--remove-after is only valid with --settings-dir")

    validator = build_rpc_validator(args.ngfw, args.username, args.password, args.scheme)
    rows: Dict[Tuple[str, str, str], SweepRow] = {}

    if args.file:
        if not args.uid:
            raise SystemExit(
                "--file requires --uid <appliance-uid> (the .backup archive "
                "does not contain /usr/share/untangle/conf/uid, so the operator "
                "must supply it explicitly)")
        sweep_backup_file(args.file, args.uid, inventory, validator, rows)
    elif args.dir:
        # Per-file uid derived from backup filename — --uid is meaningless
        # here because a batch contains multiple appliances.
        if args.uid:
            logger.warning("--uid is ignored in --dir mode "
                           "(each backup's uid is derived from its filename)")
        for bk in sorted(args.dir.glob("*.backup")):
            sweep_backup_file(bk, uid_from_filename(bk),
                              inventory, validator, rows)
    elif args.settings_dir:
        uid = uid_from_settings_dir(args.settings_dir) or args.uid
        if not uid:
            raise SystemExit(
                f"--settings-dir: no uid found at "
                f"{args.settings_dir.parent / 'conf' / 'uid'} and --uid not "
                f"provided. Either re-fetch via fetch-settings.sh (which now "
                f"pulls conf/uid) or pass --uid <value> explicitly.")
        sweep_settings_dir(args.settings_dir, uid, inventory, validator, rows)
    elif args.live:
        if not args.uid:
            raise SystemExit(
                "--live requires --uid <value> (the appliance UID from "
                "/usr/share/untangle/conf/uid on the target box).")
        from uvm import Uvm  # type: ignore
        ctx = Uvm().getUvmContext(hostname=args.ngfw, username=args.username,
                                  password=args.password, timeout=240, scheme=args.scheme)
        sweep_live(ctx, args.uid, validator, inventory, rows)

    total_after, _cleared, updated = merge_and_write(out, rows)
    total_values = sum(r.total_count for r in rows.values())
    total_rejected = sum(r.rejected_count for r in rows.values())
    logger.info("merged into %s: %d entries total (%d updated/added)",
                out, total_after, updated)
    logger.info("this run: %d rejections / %d values across %d (class, field) pairs analyzed",
                total_rejected, total_values, len(rows))

    if args.remove_after and args.settings_dir:
        if not rows:
            logger.warning(
                "--remove-after: skipping removal because the sweep processed "
                "0 (class, field) pairs (likely the JSON files failed to parse "
                "or no annotated POJOs were present); leaving %s in place for "
                "inspection", args.settings_dir)
        else:
            safely_remove_settings_tree(args.settings_dir)

    return 0


if __name__ == "__main__":
    sys.exit(main())
