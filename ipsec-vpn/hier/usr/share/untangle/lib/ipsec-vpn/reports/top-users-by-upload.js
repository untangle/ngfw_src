{
    "uniqueId": "ipsec-KoLK9ykl0R",
    "category": "IPsec VPN",
    "description": "The top IPsec users grouped by amount of data uploaded.",
    "displayOrder": 300,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "client_username",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "client_username",
    "pieSumColumn": "sum(tx_bytes)",
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
    "title": "Top Upload Users",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

