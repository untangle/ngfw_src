{
    "uniqueId": "firewall-gsR2R5tGWw",
    "category": "Firewall",
    "description": "The number of flagged session grouped by client.",
    "displayOrder": 501,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "coalesce(sum(firewall_flagged::int),0)",
    "preCompileResults": false,
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Clients",
    "type": "PIE_GRAPH"
}
