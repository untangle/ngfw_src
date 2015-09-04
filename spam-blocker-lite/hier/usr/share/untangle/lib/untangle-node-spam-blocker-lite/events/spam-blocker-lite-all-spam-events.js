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
            "column": "spam_blocker_lite_is_spam",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","spam_blocker_lite_is_spam","spam_blocker_lite_action","spam_blocker_lite_score","spam_blocker_lite_tests_string"],
    "description": "All emails marked as Spam.",
    "displayOrder": 20,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "All Spam Events",
    "uniqueId": "spam-blocker-lite-4GKAEATNA2"
}
