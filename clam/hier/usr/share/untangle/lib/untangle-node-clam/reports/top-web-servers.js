{
    "uniqueId": "virus-blocker-lite-DMTDC6W0",
    "category": "Virus Blocker Lite",
    "description": "The number of clients with blocked viruses by web activity.",
    "displayOrder": 106,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "host",
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
    "table": "http_events",
    "title": "Web Top Blocked Sites",
    "type": "PIE_GRAPH"
}
