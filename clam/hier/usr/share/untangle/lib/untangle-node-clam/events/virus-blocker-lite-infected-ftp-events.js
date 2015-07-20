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
    "defaultColumns": ["time_stamp","hostname","username","uri","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "description": "Infected FTP sessions blocked by Virus Blocker Lite.",
    "displayOrder": 31,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "ftp_events",
    "title": "Infected Ftp Events",
    "uniqueId": "virus-blocker-lite-170242HU6X"
}
