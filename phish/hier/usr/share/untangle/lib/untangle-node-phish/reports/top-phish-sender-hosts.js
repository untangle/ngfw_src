{
    "uniqueId": "phish-blocker-sozctB1d",
    "category": "Phish Blocker",
    "description": "The number of IP addresses sending phishing attempts.",
    "displayOrder": 202,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "msgs",
    "pieGroupColumn": "s_client_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "phish_blocker_is_spam",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Top Phish Sender Hosts",
    "type": "PIE_GRAPH"
}
