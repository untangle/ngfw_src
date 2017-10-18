{
    "uniqueId": "phish-blocker-9uBCmCxM",
    "category": "Phish Blocker",
    "description": "The number of IP addresses sending phish.",
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
    "title": "Top Phish Sender Addresses",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
