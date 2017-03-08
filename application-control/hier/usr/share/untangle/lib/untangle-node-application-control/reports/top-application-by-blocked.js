{
    "uniqueId": "application-control-5fRe2GcMgK",
    "category": "Application Control",
    "description": "The number of blocked sessions grouped by application.",
    "displayOrder": 203,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "application_control_application",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "application_control_blocked",
            "operator": "=",
            "value": "true"
        }
    ],
    "title": "Top Blocked Applications",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
