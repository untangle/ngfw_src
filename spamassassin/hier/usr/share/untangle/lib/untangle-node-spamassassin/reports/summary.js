{
    "uniqueId": "spam-blocker-lite-DniRBEni",
    "category": "Spam Blocker Lite",
    "description": "A summary of spam blocking actions for email activity.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "textColumns": [
        "sum(case when spam_blocker_lite_is_spam is not null then 1 else null end::int) as scanned",
        "sum(case when spam_blocker_lite_is_spam is true then 1 else null end::int) as blocked"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "textString": "Spam Blocker Lite scanned {0} email messages of which {1} were spam.", 
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Summary",
    "type": "TEXT"
}
