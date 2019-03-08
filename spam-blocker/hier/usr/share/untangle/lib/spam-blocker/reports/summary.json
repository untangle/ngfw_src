{
    "uniqueId": "spam-blocker-gnmDTFRS",
    "category": "Spam Blocker",
    "description": "A summary of spam blocking actions for email activity.",
    "displayOrder": 6,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "sum(case when spam_blocker_is_spam is not null then 1 else 0 end) as scanned",
        "sum(case when spam_blocker_is_spam is true then 1 else 0 end) as blocked"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "textString": "Spam Blocker scanned {0} email messages of which {1} were spam.", 
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Spam Blocker Summary",
    "type": "TEXT"
}
