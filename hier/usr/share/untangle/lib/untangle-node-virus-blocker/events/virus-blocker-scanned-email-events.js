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
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","addr","sender","virus_blocker_clean","virus_blocker_name","s_server_addr","s_server_port"],
    "description": "All email sessions scanned by Virus Blocker.",
    "displayOrder": 20,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "mail_addrs",
    "title": "Scanned Email Events",
    "uniqueId": "virus-blocker-50Q7EJGWE4"
}
