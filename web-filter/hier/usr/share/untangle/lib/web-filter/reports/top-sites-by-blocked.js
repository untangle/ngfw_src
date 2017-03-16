{
    "uniqueId": "web-filter-MjA3N2EyOD",
    "category": "Web Filter",
    "description": "The number of blocked web requests grouped by website.",
    "displayOrder": 304,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "host",
    "pieSumColumn": "count(*)",
     "conditions": [
        {
            "column": "web_filter_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
   "readOnly": true,
   "table": "http_events",
   "title": "Top Blocked Sites",
    "pieStyle": "PIE",
   "type": "PIE_GRAPH"
}
