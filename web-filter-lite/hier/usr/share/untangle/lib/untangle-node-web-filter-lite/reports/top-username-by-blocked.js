{
    "uniqueId": "web-filter-lite-FL1y3iUjE2",
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "web_filter_lite_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of blocked web request grouped by username.",
    "displayOrder": 603,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Blocked Usernames",
    "type": "PIE_GRAPH"
}
