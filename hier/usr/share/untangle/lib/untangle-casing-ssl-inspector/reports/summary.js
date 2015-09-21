{
    "uniqueId": "ssl-inspector-ggDy9pSApA",
    "category": "HTTPS Inspector",
    "description": "A summary of HTTPS Inspector actions.",
    "displayOrder": 10,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "textColumns": [
        "count(*) as scanned",
        "sum(CASE WHEN ssl_inspector_status='INSPECTED' THEN 1 ELSE 0 END) as inspected"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "ssl_inspector_detail",
            "operator": "is",
            "value": "not null"
        }
    ],
    "textString": "HTTPS Inspector scanned {0} sessions and inspected {1} sessions.", 
    "readOnly": true,
    "table": "sessions",
    "title": "HTTPS Inspector Summary",
    "type": "TEXT"
}
