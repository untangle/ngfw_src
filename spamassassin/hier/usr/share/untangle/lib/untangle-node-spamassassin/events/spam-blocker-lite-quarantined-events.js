{
    "category": "Spam Blocker Lite",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "B"
        },
        {
            "column": "spam_blocker_lite_action",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "Q"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","spam_blocker_lite_is_spam","spam_blocker_lite_action","spam_blocker_lite_score"],
    "description": "All emails marked as Spam and quarantined.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "Quarantined Events",
    "uniqueId": "spam-blocker-lite-EXN6C6M674"
}
