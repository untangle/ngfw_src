{
    "uniqueId": "virus-blocker-gNI4MdxW",
    "category": "Virus Blocker",
    "description": "The number of clients with blocked viruses by FTP activity.",
    "displayOrder": 206,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "host",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "FTP Top Blocked Sites",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
