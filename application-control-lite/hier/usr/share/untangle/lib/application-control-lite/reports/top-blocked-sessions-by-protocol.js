{
    "uniqueId": "application-control-lite-6B8xRYMNWJ",
    "category": "Application Control Lite",
    "description": "The top blocked sessions by protocol.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "application_control_lite_protocol",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_lite_blocked",
            "operator": "=",
            "value": "TRUE"
        }
    ],
    "title": "Top Blocked Protocols",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

