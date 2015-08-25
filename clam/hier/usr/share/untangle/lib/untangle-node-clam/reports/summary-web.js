{
    "uniqueId": "virus-blocker-lite-omMJnSjI",
    "category": "Virus Blocker Lite",
    "description": "A summary of virus blocking actions for web activity.",
    "displayOrder": 4,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "textColumns": [
        "sum(case when virus_blocker_lite_clean is not null then 1 else null end::int) as scanned",
        "sum(case when virus_blocker_lite_clean is false then 1 else null end::int) as blocked"
    ],
    "textString": "Virus Blocker Lite scanned {0} web requests of which {1} were blocked.", 
    "readOnly": true,
    "table": "http_events",
    "title": "Virus Blocker Lite Web Summary",
    "type": "TEXT"
}
