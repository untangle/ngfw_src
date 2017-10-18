{
    "uniqueId": "virus-blocker-bCgxepqj",
    "category": "Virus Blocker",
    "description": "A summary of virus blocking actions for web activity.",
    "displayOrder": 3,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "sum(case when virus_blocker_clean is not null then 1 else 0 end) as scanned",
        "sum(case when virus_blocker_clean is false then 1 else 0 end) as blocked"
    ],
    "textString": "Virus Blocker scanned {0} web requests of which {1} were blocked.", 
    "readOnly": true,
    "table": "http_events",
    "title": "Virus Blocker Web Summary",
    "type": "TEXT"
}
