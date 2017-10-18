{
    "uniqueId": "phish-blocker-PkWTck2f",
    "category": "Phish Blocker",
    "description": "The number of email addresses with phish.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "msg",
    "pieGroupColumn": "addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "phish_blocker_is_spam",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        },
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Top Phish Recipients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
