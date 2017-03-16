{
    "uniqueId": "openvpn-uQkEs0sva7",
    "category": "OpenVPN",
    "description": "The amount of openvpn sessions over time.",
    "displayOrder": 102,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "sessions",
    "readOnly": true,
    "table": "sessions",
    "timeDataColumns": [
        "sum(case when client_intf=250 then 1 else 0 end) as from_clients",
        "sum(case when server_intf=250 then 1 else 0 end) as to_clients"
    ],
    "colors": [
        "#396c2b",
        "#0099ff"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "OpenVPN Sessions",
    "type": "TIME_GRAPH"
}
