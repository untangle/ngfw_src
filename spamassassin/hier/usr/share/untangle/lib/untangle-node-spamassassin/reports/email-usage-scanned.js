{
    "uniqueId": "spam-blocker-lite-LZ3oI0bA",
    "category": "Spam Blocker Lite",
    "description": "The amount of scanned email over time.",
    "displayOrder": 102,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when spam_blocker_lite_is_spam is not null then 1 else null end::int) as scanned"
    ],
    "colors": [
        "#e5e500"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Email Usage (scanned)",
    "type": "TIME_GRAPH"
}
