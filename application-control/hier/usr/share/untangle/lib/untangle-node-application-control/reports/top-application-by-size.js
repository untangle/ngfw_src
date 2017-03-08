{
    "uniqueId": "application-control-lBxH9QZ8A8",
    "category": "Application Control",
    "description": "The number of bytes grouped by application.",
    "displayOrder": 201,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "application_control_application",
    "pieSumColumn": "coalesce(sum(p2c_bytes)+sum(p2s_bytes),0)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "application_control_application",
            "operator": "is",
            "value": "not null"
        }
    ],
    "title": "Top Applications (by size)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
