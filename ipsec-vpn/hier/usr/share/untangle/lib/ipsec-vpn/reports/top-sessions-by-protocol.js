{
    "uniqueId": "ipsec-5Rw4NWt5dd",
    "category": "IPsec VPN",
    "description": "The top IPsec VPN connections by protocol.",
    "displayOrder": 400,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "client_protocol",
    "pieSumColumn": "count(*)",
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
    "title": "Top Protocols",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

