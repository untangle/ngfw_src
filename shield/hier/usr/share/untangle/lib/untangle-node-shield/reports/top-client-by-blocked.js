{
    "uniqueId": "shield-pvWK1UFSwD",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by client.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(shield_blocked::int)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Clients",
    "type": "PIE_GRAPH"
}
