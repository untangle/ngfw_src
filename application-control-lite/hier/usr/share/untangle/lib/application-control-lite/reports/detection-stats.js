{
    "uniqueId": "application-control-lite-9Yyq8iXZJ5",
    "category": "Application Control Lite",
    "description": "The number of logged and blocked sessions over time.",
    "displayOrder": 99,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "sessions",
    "readOnly": true,
    "table": "sessions",
    "timeDataColumns": [
        "COUNT(*) as logged",
        "SUM(application_control_lite_blocked::int) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#8c0000"
    ],

    "conditions": [
        {
        "javaClass": "com.untangle.app.reports.SqlCondition",
        "column": "application_control_lite_protocol",
        "operator": "is",
        "value": "not null"
       }
    ],

    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Detection Statistics",
    "type": "TIME_GRAPH"
}

