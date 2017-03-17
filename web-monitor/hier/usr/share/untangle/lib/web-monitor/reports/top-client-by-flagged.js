{
    "uniqueId": "web-monitor-iVSJMxo2XO",
    "category": "Web Monitor",
    "conditions": [
        {
            "column": "web_filter_flagged",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of flagged web request grouped by client.",
    "displayOrder": 502,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Flagged Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
