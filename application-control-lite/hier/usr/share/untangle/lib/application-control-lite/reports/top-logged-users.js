{
    "uniqueId": "application-control-lite-5DhuKIZiYq",
    "category": "Application Control Lite",
    "description": "The top logged sessions by user.",
    "displayOrder": 700,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_lite_blocked",
            "operator": "=",
            "value": "FALSE"
        },
        {
        "javaClass": "com.untangle.app.reports.SqlCondition",
        "column": "application_control_lite_protocol",
        "operator": "is",
        "value": "not null"
        }
    ],
    "title": "Top Logged Users",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

