{
    "uniqueId": "phish-blocker-QuhTJ1ude8",
    "category": "Phish Blocker",
    "description": "The ratio of phish (true) to ham (false)",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "phish_blocker_is_spam",
    "orderDesc": true,
    "units": "msg",
    "pieGroupColumn": "phish_blocker_is_spam",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "phish_blocker_is_spam",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "not null"
        },
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "colors": [
        "#8c0000",
        "#396c2b"
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Phish Ratio",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
