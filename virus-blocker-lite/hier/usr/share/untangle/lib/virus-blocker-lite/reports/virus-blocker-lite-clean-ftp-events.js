{
    "category": "Virus Blocker Lite",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","uri","virus_blocker_lite_clean","virus_blocker_lite_name","c_client_addr","s_server_addr"],
    "description": "Scanned FTP sessions marked clean.",
    "displayOrder": 1032,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "ftp_events",
    "title": "Clean Ftp Events",
    "uniqueId": "virus-blocker-lite-T2Q826GF27"
}
