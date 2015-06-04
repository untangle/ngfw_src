{
    "uniqueId": "firewall-2joT1JbMKZw",
    "category": "Firewall",
    "description": "The number of flagged session grouped by hostname.",
    "displayOrder": 401,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "coalesce(sum(firewall_flagged::int),0)",
    "preCompileResults": false,
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Hostnames",
    "type": "PIE_GRAPH"
}
