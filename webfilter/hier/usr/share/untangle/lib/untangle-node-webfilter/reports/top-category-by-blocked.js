{
    "uniqueId": "web-filter-lite-ZjVhMzI1NWx",
    "category": "Web Filter Lite",
    "description": "The number of blocked web requests grouped by category.",
    "displayOrder": 203,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "web_filter_lite_category",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "web_filter_lite_blocked",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Top Blocked Categories",
    "type": "PIE_GRAPH"
}
