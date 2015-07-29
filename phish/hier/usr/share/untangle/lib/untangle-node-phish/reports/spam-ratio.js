{
    "uniqueId": "phish-blocker-QuhTJ1ude8",
    "category": "Phish Blocker",
    "description": "The ratio of phish (true) to ham (false)",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "phish_blocker_is_spam",
    "orderDesc": true,
    "units": "msg",
    "pieGroupColumn": "phish_blocker_is_spam",
    "pieSumColumn": "count(*)",
    "conditions": [],
    "colors": [
        "#8c0000",
        "#396c2b"
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Phish Ratio",
    "type": "PIE_GRAPH"
}
