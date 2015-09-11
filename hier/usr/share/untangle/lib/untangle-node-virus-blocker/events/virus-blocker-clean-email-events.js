{
    "category": "Virus Blocker",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        },
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
        "defaultColumns": ["time_stamp","hostname","username","addr","sender","virus_blocker_clean","virus_blocker_name","s_server_addr","s_server_port"],
    "description": "Scanned email sessions marked clean.",
    "displayOrder": 22,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "mail_addrs",
    "title": "Clean Email Events",
    "uniqueId": "virus-blocker-M3HILGQBQJ"
}
