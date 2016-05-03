{
    "uniqueId": "application-control-lite-Ls3kMNeILp",
    "category": "Application Control Lite",
    "description": "The top blocked sessions by host.",
    "displayOrder": 400,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "application_control_lite_blocked",
            "operator": "=",
            "value": "TRUE"
        }
    ],
    "title": "Top Blocked Hosts",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

