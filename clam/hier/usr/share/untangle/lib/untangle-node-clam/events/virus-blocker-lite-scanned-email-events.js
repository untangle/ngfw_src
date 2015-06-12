{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "in",
            "value": "('T', 'C')"
        },
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","addr","sender","s_server_addr","s_server_port"],
    "displayOrder": 20,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "mail_addrs",
    "title": "Scanned Email Events",
    "uniqueId": "virus-blocker-lite-9WV05ZZB03"
}
