{
    "uniqueId": "firewall-4UsnhNrCgI",
    "category": "Firewall",
    "description": "The number of flagged session grouped by server (destination) port.",
    "displayOrder": 701,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_port",
    "pieSumColumn": "coalesce(sum(firewall_flagged::int),0)",
    "preCompileResults": false,
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Server Ports",
    "type": "PIE_GRAPH"
}
