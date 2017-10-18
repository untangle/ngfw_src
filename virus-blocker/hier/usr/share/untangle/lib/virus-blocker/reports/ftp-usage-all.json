{
    "uniqueId": "virus-blocker-JJ05hQYG",
    "category": "Virus Blocker",
    "description": "The amount of scanned and blocked FTP requests over time.",
    "displayOrder": 201,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "ftp_events",
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
    "title": "FTP Usage (all)",
    "type": "TIME_GRAPH"
}
