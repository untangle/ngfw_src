{
    "uniqueId": "phish-blocker-5iEy71XA",
    "category": "Phish Blocker",
    "description": "The amount of phish email over time.",
    "displayOrder": 104,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when phish_blocker_is_spam is true then 1 else 0 end) as phish"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "colors": [
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Email Usage (phish)",
    "type": "TIME_GRAPH"
}
