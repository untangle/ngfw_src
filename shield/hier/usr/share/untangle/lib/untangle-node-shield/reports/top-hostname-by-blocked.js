{
    "uniqueId": "shield-81IWC45RM4",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by hostname.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(shield_blocked::int)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Hostnames",
    "type": "PIE_GRAPH"
}
