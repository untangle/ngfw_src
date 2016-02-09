{
    "uniqueId": "web-filter-lite-Z1MJRq6VfX",
    "category": "Web Filter Lite",
    "description": "The amount of flagged, and blocked web requests over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "sum(web_filter_lite_flagged::int) as flagged",
        "sum(web_filter_lite_blocked::int) as blocked"
    ],
    "colors": [
        "#e5e500",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Web Usage (flagged)",
    "type": "TIME_GRAPH"
}
