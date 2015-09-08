{
    "uniqueId": "web-filter-lite-Yzg3NWI4YT",
    "category": "Web Filter Lite",
    "description": "The number of flagged web requests grouped by category.",
    "displayOrder": 202,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "web_filter_lite_category",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "web_filter_lite_flagged",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Top Flagged Categories",
    "type": "PIE_GRAPH"
}
