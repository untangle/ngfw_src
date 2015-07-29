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
            "operator": "=",
            "value": "'Q'"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","phish_blocker_is_spam","phish_blocker_action","phish_blocker_score"],
    "description": "All email sessions detected as phishing attempts and quarantined.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "mail_addrs",
    "title": "Quarantined Events",
    "uniqueId": "phish-blocker-E0WZTCERV7"
}
