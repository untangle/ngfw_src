{
    "uniqueId": "spam-blocker-oRbevSO5",
    "category": "Spam Blocker",
    "description": "The amount of clean email over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when spam_blocker_is_spam is false then 1 else 0 end) as clean"
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
        "#396c2b"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Email Usage (clean)",
    "type": "TIME_GRAPH"
}
