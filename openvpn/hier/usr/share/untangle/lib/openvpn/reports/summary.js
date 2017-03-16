{
    "uniqueId": "openvpn-upl31dqKb1",
    "category": "OpenVPN",
    "description": "A summary of OpenVPN actions.",
    "displayOrder": 18,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "round(coalesce(sum(rx_bytes + tx_bytes), 0) / (1024*1024),1) as total"
    ],
    "textString": "OpenVPN processed {0} megabytes over OpenVPN tunnels.", 
    "readOnly": true,
    "table": "openvpn_stats",
    "title": "OpenVPN Summary",
    "type": "TEXT"
}
