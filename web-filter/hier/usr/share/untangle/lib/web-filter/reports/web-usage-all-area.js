{
    "uniqueId": "web-filter-h0jelsttGp",
    "category": "Web Filter",
    "description": "The amount of total, flagged, and blocked web requests over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "count(*) as scanned",
        "sum(web_filter_flagged::int) as flagged",
        "sum(web_filter_blocked::int) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#e5e500",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "AREA",
    "title": "Web Usage",
    "type": "TIME_GRAPH"
}
