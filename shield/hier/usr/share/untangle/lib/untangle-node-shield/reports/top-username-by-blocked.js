{
    "uniqueId": "shield-k2W4GfY07m",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by username.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(shield_blocked::int)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Usernames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
