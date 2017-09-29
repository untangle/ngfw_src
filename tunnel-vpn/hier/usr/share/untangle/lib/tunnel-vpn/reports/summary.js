{
    "uniqueId": "tunnel-vpn-dR1irhUiFZ",
    "category": "Tunnel VPN",
    "description": "A summary of Tunnel VPN traffic.",
    "displayOrder": 17,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "round(coalesce(sum(in_bytes),0)/1024/1024) as mbrecv",
        "round(coalesce(sum(out_bytes),0)/1024/1024) as mbxmit"
    ],
    "textString": "Tunnel VPN received {0} megabytes and sent {1} megabytes over VPN tunnels.",
    "readOnly": true,
    "table": "tunnel_vpn_stats",
    "title": "Tunnel VPN Summary",
    "type": "TEXT"
}
