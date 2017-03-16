{
    "uniqueId": "spam-blocker-6q1mBo4f",
    "category": "Spam Blocker",
    "description": "The amount of spam email over time.",
    "displayOrder": 104,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when spam_blocker_is_spam is true then 1 else 0 end) as spam"
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
    "title": "Email Usage (spam)",
    "type": "TIME_GRAPH"
}
