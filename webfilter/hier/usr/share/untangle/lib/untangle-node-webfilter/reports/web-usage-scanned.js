{
    "uniqueId": "web-filter-lite-y3kSEfxoTe",
    "category": "Web Filter Lite",
    "description": "The amount of total, flagged, and blocked web requests over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
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
    "timeStyle": "BAR_3D",
    "title": "Web Usage (scanned)",
    "type": "TIME_GRAPH"
}
