{
    "uniqueId": "application-control-lite-mA20Nz5XUg",
    "category": "Application Control Lite",
    "description": "The top blocked sessions by user.",
    "displayOrder": 600,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "column": "application_control_lite_blocked",
            "operator": "=",
            "value": "TRUE"
        }
    ],
    "title": "Top Blocked Users",
    "type": "PIE_GRAPH"
}

