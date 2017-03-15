{
    "uniqueId": "shield-2ObNkapIEq",
    "category": "Shield",
    "description": "The amount of scanned and blocked sessions over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "bypassed",
            "operator": "=",
            "value": "false"
        }
    ],
    "timeDataColumns": [
        "count(*) as scanned"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Scanned Sessions",
    "type": "TIME_GRAPH"
}
