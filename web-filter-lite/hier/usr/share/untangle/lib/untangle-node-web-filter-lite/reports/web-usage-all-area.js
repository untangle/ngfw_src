{
    "uniqueId": "web-filter-lite-9dIdqhMNva",
    "category": "Web Filter Lite",
    "description": "The amount of total, flagged, and blocked web requests over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "count(*) as scanned",
        "sum(web_filter_lite_flagged::int) as flagged",
        "sum(web_filter_lite_blocked::int) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#e5e500",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "AREA",
    "title": "Web Usage [area]",
    "type": "TIME_GRAPH"
}
