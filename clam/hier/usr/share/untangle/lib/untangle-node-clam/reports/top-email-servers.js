{
    "uniqueId": "virus-blocker-lite-yAxwtn6D",
    "category": "Virus Blocker Lite",
    "description": "The number of clients with blocked viruses by Email activity.",
    "displayOrder": 306,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Email Top Blocked Sites",
    "type": "PIE_GRAPH"
}
