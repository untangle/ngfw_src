#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import gzip
import io
import logging
import re
import sys
from typing import Optional

# ------------------------------------------------------------------
# Logging setup
# - Single stream handler
# - Timestamp with milliseconds
# - No propagation to root logger
# ------------------------------------------------------------------
logger = logging.getLogger("SqlScanner")
logger.setLevel(logging.INFO)

handler = logging.StreamHandler()

formatter = logging.Formatter(
    fmt="%(asctime)s.%(msecs)03d %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)

handler.setFormatter(formatter)
logger.addHandler(handler)
logger.propagate = False


EMPTY_STRING = ""

# ------------------------------------------------------------------
# SQL patterns that must never appear in restore files
# These cover OS execution, superuser actions, role manipulation,
# data modification, unsafe extensions, and procedural languages.
# ------------------------------------------------------------------
DANGEROUS_PATTERNS = [
    re.compile(r"\bcopy\b[\s\S]*?\b(to|from)\b[\s\S]*?\bprogram\b", re.IGNORECASE),
    re.compile(r"\\!", re.IGNORECASE),
    re.compile(r"\\copy\b", re.IGNORECASE),
    re.compile(r"\blanguage\s+c\b", re.IGNORECASE),
    re.compile(r"\bplpythonu\b", re.IGNORECASE),
    re.compile(r"\bplperlu\b", re.IGNORECASE),
    re.compile(r"\bpltclu\b", re.IGNORECASE),
    re.compile(r"\bdo\s*\$\$", re.IGNORECASE),
    re.compile(r"\bexecute\b", re.IGNORECASE),
    re.compile(r"\blo_import\b", re.IGNORECASE),
    re.compile(r"\blo_export\b", re.IGNORECASE),
    re.compile(r"\bfile_fdw\b", re.IGNORECASE),
    re.compile(r"\badminpack\b", re.IGNORECASE),
    re.compile(r"\bdblink\b", re.IGNORECASE),
    re.compile(r"\balter\s+system\b", re.IGNORECASE),
    re.compile(r"\bsuperuser\b", re.IGNORECASE),
    re.compile(r"\bcreaterole\b", re.IGNORECASE),
    re.compile(r"\bcreatedb\b", re.IGNORECASE),
    re.compile(r"^\s*create\s+function\b", re.IGNORECASE),
    re.compile(r"^\s*alter\s+function\b", re.IGNORECASE),
    re.compile(r"^\s*create\s+procedure\b", re.IGNORECASE),
    re.compile(r"^\s*alter\s+procedure\b", re.IGNORECASE),
    re.compile(r"\bsecurity\s+definer\b", re.IGNORECASE),
    re.compile(r"^\s*alter\s+role\b", re.IGNORECASE),
    re.compile(r"^\s*create\s+role\b", re.IGNORECASE),
    re.compile(r"^\s*drop\s+role\b", re.IGNORECASE),
    re.compile(r"^\s*set\s+role\b", re.IGNORECASE),
    re.compile(r"^\s*alter\s+default\s+privileges\b", re.IGNORECASE),
    re.compile(r"^\s*grant\s+.*\s+on\s+(function|schema|database|role)\b", re.IGNORECASE),
    re.compile(r"^\s*revoke\s+.*\s+on\s+(function|schema|database|role)\b", re.IGNORECASE),
    re.compile(r"^\s*create\s+extension\b", re.IGNORECASE),
    re.compile(r"^\s*alter\s+extension\b", re.IGNORECASE),
    re.compile(r"^\s*drop\s+extension\b", re.IGNORECASE),
    re.compile(r"^\s*set\s+session_replication_role\b", re.IGNORECASE),
    re.compile(r"^\s*delete\b", re.IGNORECASE),
    re.compile(r"^\s*truncate\b", re.IGNORECASE),
    re.compile(r"^\s*update\b", re.IGNORECASE),
    re.compile(r"^\s*merge\b", re.IGNORECASE),
    re.compile(r"^\s*insert\b", re.IGNORECASE),
]

# COPY ... FROM STDIN; statement header
COPY_FROM_STDIN = re.compile(
    r"^\s*copy\s+.+\s+from\s+stdin\s*;\s*$",
    re.IGNORECASE
)
# End marker for COPY STDIN data block
COPY_DATA_END = re.compile(r"^\s*\\\.\s*$")


# ------------------------------------------------------------------
# Utility functions
# ------------------------------------------------------------------
def strip_sql_comments_line(line: str) -> str:
    """Remove block and line comments without touching SQL structure."""
    line = re.sub(r"/\*.*?\*/", EMPTY_STRING, line)
    line = re.sub(r"--.*$", EMPTY_STRING, line)
    return line


def normalize(sql: str) -> str:
    """Normalize SQL for pattern matching."""
    return re.sub(r"\s+", " ", sql.lower()).strip()


def inspect_statement(stmt: str) -> None:
    """Validate a complete SQL statement against forbidden patterns."""
    normalized = normalize(stmt)
    for p in DANGEROUS_PATTERNS:
        if p.search(normalized):
            raise RuntimeError(
                f"Blocked dangerous SQL statement: {stmt[:200]}..., "
                f"blocking pattern: {p.pattern}"
            )


# ------------------------------------------------------------------
# Streaming SQL scanner for .sql.gz dumps
# - Reads incrementally
# - Correctly tracks quotes and dollar blocks
# - Skips COPY STDIN data safely
# ------------------------------------------------------------------
def inspect_sql_gz(gz_file_path: str) -> None:
    logger.info("--------------- Starting SQL scan ---------------")

    # Quote / block state tracking
    in_single = False
    in_double = False
    in_dollar = False
    dollar_tag: Optional[str] = None
    in_copy_data = False

    # Buffer for building a full SQL statement
    current = []

    with io.TextIOWrapper(gzip.open(gz_file_path, "rb"), encoding="utf-8") as reader:
        for line_no, raw_line in enumerate(reader, 1):

            # Skip COPY STDIN data payload completely
            if in_copy_data:
                if COPY_DATA_END.match(raw_line):
                    in_copy_data = False
                continue

            # Detect COPY FROM STDIN header
            if COPY_FROM_STDIN.match(raw_line):
                logger.info(re.sub(r'\n{2,}', '\n', raw_line).strip())
                inspect_statement(raw_line)
                in_copy_data = True
                continue

            # Strip comments before parsing characters
            line = strip_sql_comments_line(raw_line)

            i = 0
            while i < len(line):
                c = line[i]

                # Handle dollar-quoted blocks ($tag$ ... $tag$)
                if not in_single and not in_double:
                    if not in_dollar and c == '$':
                        m = re.match(r"\$[\w]*\$", line[i:])
                        if m:
                            in_dollar = True
                            dollar_tag = m.group(0)
                            current.append(dollar_tag)
                            i += len(dollar_tag)
                            continue
                    elif in_dollar and dollar_tag and line.startswith(dollar_tag, i):
                        current.append(dollar_tag)
                        i += len(dollar_tag)
                        in_dollar = False
                        dollar_tag = None
                        continue

                # Track single and double quoted strings
                if not in_dollar:
                    if c == "'" and not in_double:
                        in_single = not in_single
                    elif c == '"' and not in_single:
                        in_double = not in_double

                # Statement boundary: semicolon outside any quote/block
                if c == ';' and not in_single and not in_double and not in_dollar:
                    current.append(c)
                    stmt = "".join(current)
                    logger.info(re.sub(r'\n{2,}', '\n', stmt).strip())
                    inspect_statement(stmt)
                    current.clear()
                    i += 1
                    continue

                current.append(c)
                i += 1

            # Preserve line separation for multi-line statements
            current.append("\n")
            # Periodic progress logging for large dumps
            if line_no % 10000 == 0:
                logger.info("Scanned %d lines...", line_no)
    # Validate trailing statement without semicolon
    if current:
        inspect_statement("".join(current))

    logger.info("--------------- SQL scan completed successfully --------------- ")

# Entry point
if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(2)

    inspect_sql_gz(sys.argv[1])
