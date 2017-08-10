{
    "uniqueId": "captive-portal-IPiZ5SS7",
    "category": "Captive Portal",
    "description": "The top clients that were blocked by Captive Portal because they were not logged in.",
    "displayOrder": 300,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "column": "captive_portal_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "title": "Top Blocked Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

