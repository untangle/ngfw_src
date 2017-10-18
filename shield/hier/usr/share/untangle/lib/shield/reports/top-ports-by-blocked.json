{
    "uniqueId": "shield-KOQ0fVCEx0",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by server port.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_port",
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
    "title": "Top Blocked Ports",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
