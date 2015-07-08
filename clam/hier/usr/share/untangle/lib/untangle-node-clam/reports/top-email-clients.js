{
    "uniqueId": "virus-blocker-lite-hdWymiDA",
    "category": "Virus Blocker Lite",
    "description": "The number of clients with blocked viruses by Email activity.",
    "displayOrder": 305,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
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
    "title": "Email Top Blocked Clients",
    "type": "PIE_GRAPH"
}
