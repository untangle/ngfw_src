{
    "uniqueId": "phish-blocker-ubxvrHLv",
    "category": "Phish Blocker",
    "description": "The amount of clean email over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when phish_blocker_is_spam is false then 1 else null end::int) as clean"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Email Usage (clean)",
    "type": "TIME_GRAPH"
}
