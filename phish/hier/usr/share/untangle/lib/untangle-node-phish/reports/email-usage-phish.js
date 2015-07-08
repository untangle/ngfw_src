{
    "uniqueId": "phish-blocker-Vdaj7OPI",
    "category": "Phish Blocker",
    "description": "The amount of phish email over time.",
    "displayOrder": 104,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when phish_blocker_is_spam is true then 1 else null end::int) as phish"
    ],
    "colors": [
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Email Usage (phish)",
    "type": "TIME_GRAPH"
}
