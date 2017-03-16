{
    "uniqueId": "ssl-inspector-pqIdaeonC2",
    "category": "SSL Inspector",
    "description": "The number of inspected sessions grouped by site.",
    "displayOrder": 200,
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
            "value": "INSPECTED"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Inspected Sites",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
