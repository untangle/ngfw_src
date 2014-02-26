/**
 * $Id$
 */
package com.untangle.uvm.util;

public class QueryUtil
{
    public static String toOrderByClause( String... sortColumns){
        return toOrderByClause(null, sortColumns);
    }

    public static String toOrderByClause(String alias, String... sortColumns)
    {
        final StringBuilder orderBy = new StringBuilder();
        if (0 < sortColumns.length) {
            orderBy.append("order by ");
            for (int i = 0; i < sortColumns.length; i++) {
                String col = sortColumns[i];
                String dir;
                if (col.startsWith("+")) {
                    dir = "ASC";
                    col = col.substring(1);
                } else if (col.startsWith("-")) {
                    dir = "DESC";
                    col = col.substring(1);
                } else {
                    dir = "ASC";
                }

                if (col.toUpperCase().startsWith("UPPER(")) {
                    rewriteUpper(orderBy, col, alias);
                } else {
                    if (alias != null) {
                        orderBy.append(alias);
                        orderBy.append(".");
                    }
                    orderBy.append(col);
                }

                orderBy.append(" ");
                orderBy.append(dir);
                if (i + 1 < sortColumns.length) {
                    orderBy.append(", ");
                }
            }
        }

        return orderBy.toString();
    }

    private static void rewriteUpper(StringBuilder sb, String col, String alias)
    {
        if (!col.endsWith(")")) {
            sb.append(col);
        } else {
            sb.append("upper(");
            if (alias != null) {
                sb.append(alias);
                sb.append(".");
            }
            sb.append(col.substring(6, col.length() - 1));
            sb.append(")");
        }
    }
}