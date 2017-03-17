{
    "uniqueId": "application-control-TcoSd29O1v",
    "category": "Application Control",
    "description": "The amount of flagged, and blocked sessions over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "sessions",
    "timeDataColumns": [
        "sum(application_control_blocked::int) as blocked"
    ],
    "colors": [
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "Scanned Sessions (blocked)",
    "type": "TIME_GRAPH"
}
