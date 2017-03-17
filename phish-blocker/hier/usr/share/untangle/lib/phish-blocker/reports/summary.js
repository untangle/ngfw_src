{
    "uniqueId": "phish-blocker-DniRBEni",
    "category": "Phish Blocker",
    "description": "A summary of phish blocking actions for email activity.",
    "displayOrder": 7,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "sum(case when phish_blocker_is_spam is not null then 1 else 0 end) as scanned",
        "sum(case when phish_blocker_is_spam is true then 1 else 0 end) as blocked"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "textString": "Phish Blocker scanned {0} email messages of which {1} were phish.", 
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Phish Blocker Summary",
    "type": "TEXT"
}
