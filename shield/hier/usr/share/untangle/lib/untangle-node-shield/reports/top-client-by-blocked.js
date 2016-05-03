{
    "uniqueId": "shield-pvWK1UFSwD",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by client.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(shield_blocked::int)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
