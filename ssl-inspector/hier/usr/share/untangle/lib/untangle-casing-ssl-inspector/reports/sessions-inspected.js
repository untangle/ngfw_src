{
    "uniqueId": "ssl-inspector-NnhAltR0Xd",
    "category": "SSL Inspector",
    "description": "The amount of inspected SSL sessions over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "ssl_inspector_status",
            "operator": "=",
            "value": "INSPECTED"
        }
    ],
     "timeDataColumns": [
        "count(*) as inspected"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "Inspected Sessions",
    "type": "TIME_GRAPH"
}
