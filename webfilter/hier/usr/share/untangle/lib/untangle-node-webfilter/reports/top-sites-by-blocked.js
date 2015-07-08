{
    "uniqueId": "web-filter-lite-MjA3N2EyOD",
    "category": "Web Filter Lite",
    "description": "The number of blocked web requests grouped by website.",
    "displayOrder": 304,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "host",
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
   "title": "Top Blocked Sites",
   "type": "PIE_GRAPH"
}
