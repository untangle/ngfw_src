{
    "uniqueId": "web-filter-lite-NDYyZjFhZm",
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "web_filter_lite_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of blocked web request grouped by hostname.",
    "displayOrder": 403,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Blocked Hostnames",
    "type": "PIE_GRAPH"
}
