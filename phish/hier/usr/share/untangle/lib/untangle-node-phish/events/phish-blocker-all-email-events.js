{
    "category": "Phish Blocker",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "in",
            "value": "('T', 'C')"
        },
        {
            "column": "phish_blocker_action",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        },
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","phish_blocker_is_spam","phish_blocker_action","phish_blocker_score"],
    "displayOrder": 10,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "mail_addrs",
    "title": "All Email Events",
    "uniqueId": "phish-blocker-H2833I40O7"
}
