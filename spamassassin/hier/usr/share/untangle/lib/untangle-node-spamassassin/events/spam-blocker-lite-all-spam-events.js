{
    "category": "Spam Blocker Lite",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "in",
            "value": "('T', 'C')"
        },
        {
            "column": "spam_blocker_lite_is_spam",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        },
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","spam_blocker_lite_is_spam","spam_blocker_lite_action","spam_blocker_lite_score"],
    "displayOrder": 20,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "mail_addrs",
    "title": "All Spam Events",
    "uniqueId": "spam-blocker-lite-4GKAEATNA2"
}
