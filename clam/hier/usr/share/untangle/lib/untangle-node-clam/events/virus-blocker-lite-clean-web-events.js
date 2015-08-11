{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "description": "Scanned HTTP sessions marked clean.",
    "displayOrder": 12,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "http_events",
    "title": "Clean Web Events",
    "uniqueId": "virus-blocker-lite-0NT6SJ0JHB"
}
