{
    "uniqueId": "application-control-lite-pcRLxvPFeR",
    "category": "Application Control Lite",
    "description": "The top detected protocols.",
    "displayOrder": 300,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "application_control_lite_protocol",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
        "javaClass": "com.untangle.node.reporting.SqlCondition",
        "column": "application_control_lite_protocol",
        "operator": "is",
        "value": "not null"
        }
    ],
    "title": "Top Detected Protocols",
    "type": "PIE_GRAPH"
}

