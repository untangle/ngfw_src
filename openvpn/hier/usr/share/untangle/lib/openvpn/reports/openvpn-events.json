{
    "uniqueId": "openvpn-Bp3UkhVS1x",
    "category": "OpenVPN",
    "description": "The amount of login and logout events over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "events",
    "readOnly": true,
    "table": "openvpn_events",
    "timeDataColumns": [
        "sum(case when type='CONNECT' then 1 else 0 end) as logins",
        "sum(case when type='DISCONNECT' then 1 else 0 end) as logouts"
    ],
    "colors": [
        "#396c2b",
        "#0099ff"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "OpenVPN Events",
    "type": "TIME_GRAPH"
}
