{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "FALSE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","virus_blocked_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "displayOrder": 11,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "http_events",
    "title": "Infected Web Events",
    "uniqueId": "virus-blocker-lite-053SAEB6ZP"
}
