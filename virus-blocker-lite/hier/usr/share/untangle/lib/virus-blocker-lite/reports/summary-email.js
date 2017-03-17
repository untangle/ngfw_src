{
    "uniqueId": "virus-blocker-lite-CRxmUhVM",
    "category": "Virus Blocker Lite",
    "description": "A summary of virus blocking actions for Email activity.",
    "displayOrder": 4,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "sum(case when virus_blocker_lite_clean is not null then 1 else 0 end) as scanned",
        "sum(case when virus_blocker_lite_clean is false then 1 else 0 end) as blocked"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "textString": "Virus Blocker Lite scanned {0} email messages of which {1} were blocked.", 
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Virus Blocker Lite Email Summary",
    "type": "TEXT"
}
