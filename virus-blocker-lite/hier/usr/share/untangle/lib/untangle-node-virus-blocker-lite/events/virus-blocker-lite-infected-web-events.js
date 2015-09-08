{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "FALSE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "description": "Infected HTTP sessions blocked by Virus Blocker Lite.",
    "displayOrder": 11,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "Infected Web Events",
    "uniqueId": "virus-blocker-lite-053SAEB6ZP"
}
