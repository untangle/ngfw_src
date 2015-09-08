{
    "uniqueId": "virus-blocker-lite-JqAuZFGR",
    "category": "Virus Blocker Lite",
    "description": "The number of clients with blocked viruses by web activity.",
    "displayOrder": 105,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Web Top Blocked Clients",
    "type": "PIE_GRAPH"
}
