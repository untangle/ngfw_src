{
    "uniqueId": "phish-blocker-BsWNaIy8",
    "category": "Phish Blocker",
    "description": "The amount of scanned email over time.",
    "displayOrder": 102,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when phish_blocker_is_spam is not null then 1 else null end::int) as scanned"
    ],
    "colors": [
        "#e5e500"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Email Usage (scanned)",
    "type": "TIME_GRAPH"
}
