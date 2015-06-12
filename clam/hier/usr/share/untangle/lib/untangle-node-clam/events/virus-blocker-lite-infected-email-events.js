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
            "value": "FALSE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","addr","sender","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "displayOrder": 21,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "mail_addrs",
    "title": "Infected Email Events",
    "uniqueId": "virus-blocker-lite-44TQDYOWVL"
}
