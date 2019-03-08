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
            "operator": "=",
            "value": "Q"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","c_client_addr","s_server_addr","addr","sender","subject","phish_blocker_is_spam","phish_blocker_action","phish_blocker_score"],
    "description": "All email sessions detected as phishing attempts and quarantined.",
    "displayOrder": 1030,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "Quarantined Events",
    "uniqueId": "phish-blocker-E0WZTCERV7"
}
