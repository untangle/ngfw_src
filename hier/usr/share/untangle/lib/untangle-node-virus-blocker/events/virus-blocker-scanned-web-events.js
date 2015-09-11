{
    "category": "Virus Blocker",
    "conditions": [
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","virus_blocker_clean","virus_blocker_name","s_server_addr","s_server_port"],
    "description": "All HTTP sessions scanned by Virus Blocker.",
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "Scanned Web Events",
    "uniqueId": "virus-blocker-CLE9XCOTQ3"
}
