{
    "uniqueId": "ssl-inspector-UCVS4IhqjG",
    "category": "SSL Inspector",
    "description": "The number of ignored sessions grouped by site.",
    "displayOrder": 201,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "ssl_inspector_detail",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "ssl_inspector_status",
            "operator": "=",
            "value": "IGNORED"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Ignored Sites",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
