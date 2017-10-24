{
    "uniqueId": "spam-blocker-19q73IWt",
    "category": "Spam Blocker",
    "description": "The number of IP addresses sending spam.",
    "displayOrder": 202,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "msgs",
    "pieGroupColumn": "c_client_addr",
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
    "title": "Top Spam Sender Addresses",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
