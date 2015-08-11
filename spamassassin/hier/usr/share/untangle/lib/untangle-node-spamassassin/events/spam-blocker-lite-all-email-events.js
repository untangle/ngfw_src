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
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","spam_blocker_lite_is_spam","spam_blocker_lite_action","spam_blocker_lite_score","spam_blocker_lite_tests_string"],
    "description": "All emails scanned by Spam Blocker.",
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "All Email Events",
    "uniqueId": "spam-blocker-lite-HEU9QMHY5S"
}
