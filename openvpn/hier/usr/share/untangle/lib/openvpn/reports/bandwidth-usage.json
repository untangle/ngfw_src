{
    "uniqueId": "openvpn-StzlzfZAp8",
    "category": "OpenVPN",
    "description": "The approximate amount of data transfered over openvpn connections.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "KB/s",
    "readOnly": true,
    "table": "openvpn_stats",
    "timeDataColumns": [
        "round(coalesce(sum(rx_bytes + tx_bytes), 0) / (1024*60),1) as total",
        "round(coalesce(sum(tx_bytes), 0) / (1024*60),1) as sent",
        "round(coalesce(sum(rx_bytes), 0) / (1024*60),1) as received"
    ],
    "colors": [
        "#396c2b",
        "#0099ff",
        "#6600ff"
    ],
    "timeDataInterval": "MINUTE",
    "timeStyle": "LINE",
    "title": "OpenVPN Bandwidth Usage",
    "type": "TIME_GRAPH"
}
