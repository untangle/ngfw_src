{
    "uniqueId": "ssl-inspector-F10QTQJPXF",
    "category": "SSL Inspector",
    "description": "The amount of SSL sessions over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "ssl_inspector_detail",
            "operator": "is",
            "value": "not null"
        }
    ],
     "timeDataColumns": [
        "sum(CASE WHEN ssl_inspector_status='INSPECTED' THEN 1 ELSE 0 END) as inspected",
        "sum(CASE WHEN ssl_inspector_status='IGNORED' THEN 1 ELSE 0 END) as ignored"
    ],
    "colors": [
        "#396c2b",
        "#e5e500"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "Sessions Scanned",
    "type": "TIME_GRAPH"
}
