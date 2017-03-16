{
    "uniqueId": "web-monitor-y3kSEfxoTe",
    "category": "Web Monitor",
    "description": "The amount of total web requests over time.",
    "displayOrder": 102,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "count(*) as scanned"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "Web Usage (scanned)",
    "type": "TIME_GRAPH"
}
