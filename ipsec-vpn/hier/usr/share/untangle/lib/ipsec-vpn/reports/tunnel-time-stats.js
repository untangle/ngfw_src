{
    "uniqueId": "ipsec-7y1o6zC1Ez",
    "category": "IPsec VPN",
    "description": "The amount of IPsec tunnel traffic over time.",
    "displayOrder": 80,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "bytes",
    "readOnly": true,
    "table": "ipsec_tunnel_stats",
"timeDataColumns": [
        "sum(in_bytes::int) as recv",
        "sum(out_bytes::int) as xmit"
    ],
    "colors": [
        "#396c2b",
        "#e5e500"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Hourly Tunnel Traffic",
    "type": "TIME_GRAPH"
}
