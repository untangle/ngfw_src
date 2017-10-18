{
    "uniqueId": "application-control-lBxH9QZ8A8",
    "category": "Application Control",
    "description": "The number of bytes grouped by application.",
    "displayOrder": 201,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "application_control_application",
    "pieSumColumn": "coalesce(sum(s2c_bytes)+sum(c2s_bytes),0)",
    "readOnly": true,
    "table": "session_minutes",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_application",
            "operator": "is",
            "value": "not null"
        }
    ],
    "title": "Top Applications (by size)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
