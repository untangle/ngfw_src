{
    "uniqueId": "spam-blocker-lite-LZ3oI0bA",
    "category": "Spam Blocker Lite",
    "description": "The amount of scanned email over time.",
    "displayOrder": 102,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when spam_blocker_lite_is_spam is not null then 1 else 0 end) as scanned"
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
        "#b2b2b2"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Email Usage (scanned)",
    "type": "TIME_GRAPH"
}
