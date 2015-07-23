{
    "uniqueId": "spam-blocker-lite-QuhTJ1ude8",
    "category": "Spam Blocker Lite",
    "description": "The ratio of spam (true) to ham (false)",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "spam_blocker_lite_is_spam",
    "orderDesc": true,
    "units": "msg",
    "pieGroupColumn": "spam_blocker_lite_is_spam",
    "pieSumColumn": "count(*)",
    "conditions": [],
    "colors": [
        "#8c0000",
        "#396c2b"
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Spam Ratio",
    "type": "PIE_GRAPH"
}
