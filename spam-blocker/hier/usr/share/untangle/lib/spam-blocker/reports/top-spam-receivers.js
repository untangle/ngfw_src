{
    "uniqueId": "spam-blocker-f6JqJWHh",
    "category": "Spam Blocker",
    "description": "The number of email addresses with spam.",
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
            "column": "spam_blocker_is_spam",
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
    "title": "Top Spam Recipients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
