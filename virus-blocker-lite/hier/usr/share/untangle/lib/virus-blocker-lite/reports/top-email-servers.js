{
    "uniqueId": "virus-blocker-lite-yAxwtn6D",
    "category": "Virus Blocker Lite",
    "description": "The number of clients with blocked viruses by Email activity.",
    "displayOrder": 306,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        },
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Email Top Blocked Sites",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
