{
    "uniqueId": "spam-blocker-QuhTJ1ude8",
    "category": "Spam Blocker",
    "description": "The ratio of spam (true) to ham (false)",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "spam_blocker_is_spam",
    "orderDesc": true,
    "units": "msg",
    "pieGroupColumn": "spam_blocker_is_spam",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "spam_blocker_is_spam",
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
    "title": "Spam Ratio",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
