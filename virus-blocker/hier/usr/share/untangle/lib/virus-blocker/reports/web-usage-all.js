{
    "uniqueId": "virus-blocker-9gTFTMGF",
    "category": "Virus Blocker",
    "description": "The amount of scanned and blocked web requests over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "sum(case when virus_blocker_clean is not null then 1 else 0 end) as scanned",
        "sum(case when virus_blocker_clean is false then 1 else 0 end) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Web Usage (all)",
    "type": "TIME_GRAPH"
}
