{
    "uniqueId": "spam-blocker-lite-PkWTck2f",
    "category": "Spam Blocker Lite",
    "description": "The number of email addresses with spam.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "msg",
    "pieGroupColumn": "addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "spam_blocker_lite_is_spam",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Top Spam Recipients",
    "type": "PIE_GRAPH"
}
