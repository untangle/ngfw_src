{
    "uniqueId": "firewall-4nnbmfOVa3",
    "category": "Firewall",
    "description": "The number of flagged session grouped by username.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
    "pieSumColumn": "coalesce(sum(firewall_flagged::int),0)",
    "preCompileResults": false,
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Usernames",
    "type": "PIE_GRAPH"
}
