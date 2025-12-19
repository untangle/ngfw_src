/**
 * $Id$
 */
package com.untangle.app.reports;

import com.untangle.uvm.util.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Scans SQL scripts for dangerous statements.
 */
public class SqlScanner {

    // Dangerous patterns to block
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            // Server-side OS command execution
            Pattern.compile(
                    "\\bcopy\\b[\\s\\S]*?\\b(to|from)\\b[\\s\\S]*?\\bprogram\\b",
                    Pattern.CASE_INSENSITIVE
            ),

            // psql client-side shell escape
            Pattern.compile("\\\\!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\\\copy\\b", Pattern.CASE_INSENSITIVE),

            // Native / untrusted procedural languages
            Pattern.compile("\\blanguage\\s+c\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bplpythonu\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bplperlu\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpltclu\\b", Pattern.CASE_INSENSITIVE),

            // Anonymous code blocks
            Pattern.compile("\\bdo\\s*\\$\\$", Pattern.CASE_INSENSITIVE),

            // Dynamic SQL
            Pattern.compile("\\bexecute\\b", Pattern.CASE_INSENSITIVE),

            // Filesystem access
            Pattern.compile("\\blo_import\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blo_export\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfile_fdw\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\badminpack\\b", Pattern.CASE_INSENSITIVE),

            // Remote SQL / data exfiltration
            Pattern.compile("\\bdblink\\b", Pattern.CASE_INSENSITIVE),

            // Persistent or privilege escalation
            Pattern.compile("\\balter\\s+system\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsuperuser\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcreaterole\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcreatedb\\b", Pattern.CASE_INSENSITIVE),

            // Functions / Procedures
            Pattern.compile("^\\s*create\\s+function\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*alter\\s+function\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*create\\s+procedure\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*alter\\s+procedure\\b", Pattern.CASE_INSENSITIVE),

            // SECURITY DEFINER only exists inside function/procedure
            Pattern.compile("\\bsecurity\\s+definer\\b", Pattern.CASE_INSENSITIVE),

            // Privileges / Roles
            Pattern.compile("^\\s*alter\\s+role\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*create\\s+role\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*drop\\s+role\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*set\\s+role\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*alter\\s+default\\s+privileges\\b", Pattern.CASE_INSENSITIVE),

            // GRANT / REVOKE on non-table objects (functions, schemas, roles)
            Pattern.compile("^\\s*grant\\s+.*\\s+on\\s+(function|schema|database|role)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*revoke\\s+.*\\s+on\\s+(function|schema|database|role)\\b",
                    Pattern.CASE_INSENSITIVE),

            // Extensions / external access
            Pattern.compile("^\\s*create\\s+extension\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*alter\\s+extension\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*drop\\s+extension\\b", Pattern.CASE_INSENSITIVE),

            // Session / replication
            Pattern.compile("^\\s*set\\s+session_replication_role\\b", Pattern.CASE_INSENSITIVE),

            // Data-changing DML (never emitted by COPY-based dump)
            Pattern.compile("^\\s*delete\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*truncate\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*update\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*merge\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*insert\\b", Pattern.CASE_INSENSITIVE)
    );

    // COPY FROM STDIN state transitions
    private static final Pattern COPY_FROM_STDIN =
            Pattern.compile("^\\s*copy\\s+.+\\s+from\\s+stdin\\s*;\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern COPY_DATA_END =
            Pattern.compile("^\\s*\\\\.\\s*$");

    /**
     * Decompresses and inspects a GZIP-compressed SQL file.
     * @param gzFile The GZIP-compressed SQL file.
     * @throws IOException If an I/O error occurs.
     */
    public static void inspectSqlGz(File gzFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(new FileInputStream(gzFile)), StandardCharsets.UTF_8))) {

            StringBuilder sqlBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlBuffer.append(line).append('\n');
            }

            inspectSql(sqlBuffer.toString());
        }
    }

    /**
     * Inspects a raw SQL string for dangerous statements after stripping comments.
     * @param rawSql The raw SQL script.
     */
    private static void inspectSql(String rawSql) {
        String noComments = stripSqlComments(rawSql);

        // Split into statements safely
        List<String> statements = splitStatements(noComments);

        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) continue;

            String normalized = normalize(trimmed);

            // Pattern scan
            for (Pattern p : DANGEROUS_PATTERNS) {
                if (p.matcher(normalized).find()) {
                    throw new SecurityException(String.format("Blocked dangerous SQL statement: %s, blocking pattern: %s", trimmed, p.pattern()));
                }
            }
        }
    }

    /**
     * Strips all block and line comments from an SQL string.
     * @param sql The SQL string.
     * @return The SQL string without comments.
     */
    private static String stripSqlComments(String sql) {
        sql = sql.replaceAll("(?s)/\\*.*?\\*/", Constants.EMPTY_STRING);
        sql = sql.replaceAll("(?m)--.*?$", Constants.EMPTY_STRING);
        return sql;
    }

    /**
     * Normalizes an SQL statement to lowercase and collapses whitespace.
     * @param sql The SQL statement.
     * @return The normalized SQL string.
     */
    private static String normalize(String sql) {
        return sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /**
     * Splits a multi-statement SQL string into a list of individual statements.
     * Handles quotes and COPY FROM STDIN blocks.
     * @param sql The SQL string to split.
     * @return A list of SQL statements.
     */
    private static List<String> splitStatements(String sql) {
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inDollarQuote = false;
        String dollarQuoteTag = null;
        boolean inCopyData = false;

        List<String> statements = new ArrayList<>();

        String[] lines = sql.split("\n", -1);
        for (String line : lines) {
            if (inCopyData) {
                if (COPY_DATA_END.matcher(line).matches()) {
                    inCopyData = false;
                }
                continue;
            }

            if (COPY_FROM_STDIN.matcher(line).matches()) {
                statements.add(line); // COPY statement itself
                inCopyData = true;
                current.setLength(0);
                continue;
            }

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                if (!inSingleQuote && !inDoubleQuote) {
                    if (!inDollarQuote && c == '$') {
                        Matcher m = Pattern.compile("\\$[\\w]*\\$")
                                .matcher(line.substring(i));
                        if (m.find() && m.start() == 0) {
                            inDollarQuote = true;
                            dollarQuoteTag = m.group();
                            current.append(dollarQuoteTag);
                            i += dollarQuoteTag.length() - 1;
                            continue;
                        }
                    } else if (inDollarQuote &&
                            line.startsWith(dollarQuoteTag, i)) {
                        current.append(dollarQuoteTag);
                        i += dollarQuoteTag.length() - 1;
                        inDollarQuote = false;
                        dollarQuoteTag = null;
                        continue;
                    }
                }

                if (!inDollarQuote) {
                    if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote;
                    if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote;
                }

                if (c == ';' && !inSingleQuote && !inDoubleQuote && !inDollarQuote) {
                    current.append(c);
                    statements.add(current.toString());
                    current.setLength(0);
                    continue;
                }

                current.append(c);
            }

            current.append('\n');
        }

        if (!current.isEmpty()) {
            statements.add(current.toString());
        }

        return statements;
    }
}
