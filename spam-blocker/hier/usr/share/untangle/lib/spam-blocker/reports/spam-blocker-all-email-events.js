{
    "category": "Spam Blocker",
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
            "column": "spam_blocker_action",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","c_client_addr","s_server_addr","addr","sender","subject","spam_blocker_is_spam","spam_blocker_action","spam_blocker_score","spam_blocker_tests_string"],
    "description": "All emails scanned by Spam Blocker.",
    "displayOrder": 1010,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "All Email Events",
    "uniqueId": "spam-blocker-30UKJZEGWZ"
}
