{
    "uniqueId": "ipsec-ZmtCC9rci4",
    "category": "IPsec VPN",
    "description": "The top IPsec users grouped by amount of data downloaded.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "client_username",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "client_username",
    "pieSumColumn": "sum(rx_bytes)",
    "readOnly": true,
    "table": "ipsec_user_events",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "elapsed_time",
            "operator": "is",
            "value": "not null"
        }
    ],
    "title": "Top Download Users",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

