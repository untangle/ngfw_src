{
    "category": "Spam Blocker Lite",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        },
        {
            "column": "spam_blocker_lite_action",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "Q"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","c_client_addr","s_server_addr","addr","sender","subject","spam_blocker_lite_is_spam","spam_blocker_lite_action","spam_blocker_lite_score","spam_blocker_lite_tests_string"],
    "description": "All emails marked as Spam and quarantined.",
    "displayOrder": 1030,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "Quarantined Events",
    "uniqueId": "spam-blocker-lite-EXN6C6M674"
}
