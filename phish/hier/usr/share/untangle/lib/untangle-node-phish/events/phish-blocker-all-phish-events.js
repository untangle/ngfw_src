{
    "category": "Phish Blocker",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "in",
            "value": "('T', 'C')"
        },
        {
            "column": "phish_blocker_is_spam",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","phish_blocker_is_spam","phish_blocker_action","phish_blocker_score"],
    "displayOrder": 20,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "All Phish Events",
    "uniqueId": "phish-blocker-M5PBL5CN1B"
}
