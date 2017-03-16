{
    "uniqueId": "virus-blocker-lite-JqAuZFGR",
    "category": "Virus Blocker Lite",
    "description": "The top web clients by blocked virus count.",
    "displayOrder": 105,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Web Top Blocked Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
