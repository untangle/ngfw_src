{
    "uniqueId": "web-filter-lite-R0Bc7vikgu",
    "category": "Web Filter Lite",
    "description": "The amount of flagged, and blocked web requests over time.",
    "displayOrder": 104,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "sum(web_filter_lite_blocked::int) as blocked"
    ],
    "colors": [
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D",
    "title": "Web Usage (blocked)",
    "type": "TIME_GRAPH"
}
