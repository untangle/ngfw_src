{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "in",
            "value": "('T', 'C')"
        },
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "FALSE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","addr","sender","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "displayOrder": 21,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "Infected Email Events",
    "uniqueId": "virus-blocker-lite-44TQDYOWVL"
}
