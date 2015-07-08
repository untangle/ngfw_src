{
    "uniqueId": "phish-blocker-aYBcy2pO",
    "category": "Phish Blocker",
    "description": "The amount of scanned, clean, and phish email over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when phish_blocker_is_spam is not null then 1 else null end::int) as scanned",
        "sum(case when phish_blocker_is_spam is false then 1 else null end::int) as clean",
        "sum(case when phish_blocker_is_spam is true then 1 else null end::int) as phish"
    ],
    "colors": [
        "#e5e500",
        "#396c2b",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Email Usage (all)",
    "type": "TIME_GRAPH"
}
