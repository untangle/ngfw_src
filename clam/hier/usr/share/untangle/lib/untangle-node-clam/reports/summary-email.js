{
    "uniqueId": "virus-blocker-lite-CRxmUhVM",
    "category": "Virus Blocker Lite",
    "description": "A summary of virus blocking actions for Email activity.",
    "displayOrder": 300,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "textColumns": [
        "sum(case when virus_blocker_lite_clean is not null then 1 else null end::int) as scanned",
        "sum(case when virus_blocker_lite_clean is false then 1 else null end::int) as blocked"
    ],
    "textString": "Virus Blocker Lite scanned {0} email messages of which {1} were blocked.", 
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Email Summary",
    "type": "TEXT"
}
