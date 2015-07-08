{
    "uniqueId": "web-filter-lite-iVSJMxo2XO",
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "web_filter_lite_flagged",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of flagged web request grouped by client.",
    "displayOrder": 502,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Flagged Clients",
    "type": "PIE_GRAPH"
}
