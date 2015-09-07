{
    "category": "Phish Blocker",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "B"
        },
        {
            "column": "phish_blocker_action",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","phish_blocker_is_spam","phish_blocker_action","phish_blocker_score"],
    "description": "All email sessions scanned by Phish Blocker.",
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "All Email Events",
    "uniqueId": "phish-blocker-H2833I40O7"
}
