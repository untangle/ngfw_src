{
    "uniqueId": "application-control-GlzEJqEcXv",
    "category": "Application Control",
    "description": "The amount of scanned, flagged, and blocked sessions over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_application",
            "operator": "is",
            "value": "not null"
        }
    ],
     "timeDataColumns": [
        "count(*) as scanned",
        "sum(application_control_flagged::int) as flagged",
        "sum(application_control_blocked::int) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#e5e500",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Scanned Sessions (all)",
    "type": "TIME_GRAPH"
}
