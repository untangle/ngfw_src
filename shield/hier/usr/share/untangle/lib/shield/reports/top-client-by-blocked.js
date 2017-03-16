{
    "uniqueId": "shield-pvWK1UFSwD",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by client.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "filter_prefix",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "shield_blocked"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
