{
    "uniqueId": "ssl-inspector-ggDy9pSApA",
    "category": "SSL Inspector",
    "description": "A summary of SSL Inspector actions.",
    "displayOrder": 10,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "count(*) as scanned",
        "sum(CASE WHEN ssl_inspector_status='INSPECTED' THEN 1 ELSE 0 END) as inspected"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "ssl_inspector_detail",
            "operator": "is",
            "value": "not null"
        }
    ],
    "textString": "SSL Inspector scanned {0} sessions and inspected {1} sessions.", 
    "readOnly": true,
    "table": "sessions",
    "title": "SSL Inspector Summary",
    "type": "TEXT"
}
