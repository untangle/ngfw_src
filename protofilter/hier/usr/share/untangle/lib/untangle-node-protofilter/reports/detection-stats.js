{
    "uniqueId": "application-control-lite-9Yyq8iXZJ5",
    "category": "Application Control Lite",
    "description": "The number of logged and blocked sessions over time.",
    "displayOrder": 99,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "sessions",
    "readOnly": true,
    "table": "sessions",
    "timeDataColumns": [
        "COUNT(*) as logged",
        "COUNT(NULLIF(application_control_lite_blocked = true, false)) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#8c0000"
    ],

    "conditions": [
        {
        "javaClass": "com.untangle.node.reporting.SqlCondition",
        "column": "application_control_lite_protocol",
        "operator": "is",
        "value": "not null"
       }
    ],

    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Detection Statistics",
    "type": "TIME_GRAPH"
}

