{
    "uniqueId": "firewall-x4d7u259zk",
    "category": "Firewall",
    "description": "The number of flagged session grouped by hostname.",
    "displayOrder": 402,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "coalesce(sum(firewall_blocked::int),0)",
    "preCompileResults": false,
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Hostnames",
    "type": "PIE_GRAPH"
}
