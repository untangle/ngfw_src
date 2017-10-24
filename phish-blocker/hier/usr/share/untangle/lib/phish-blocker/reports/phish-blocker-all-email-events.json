{
    "category": "Phish Blocker",
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
            "column": "phish_blocker_action",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","c_client_addr","s_server_addr","addr","sender","subject","phish_blocker_is_spam","phish_blocker_action","phish_blocker_score"],
    "description": "All email sessions scanned by Phish Blocker.",
    "displayOrder": 1010,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "All Email Events",
    "uniqueId": "phish-blocker-H2833I40O7"
}
