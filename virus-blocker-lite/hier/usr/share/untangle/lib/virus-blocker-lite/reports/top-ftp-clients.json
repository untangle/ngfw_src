{
    "uniqueId": "virus-blocker-lite-UaEbbQhM",
    "category": "Virus Blocker Lite",
    "description": "The number of clients with blocked viruses by FTP activity.",
    "displayOrder": 205,
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
    "title": "FTP Top Blocked Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
