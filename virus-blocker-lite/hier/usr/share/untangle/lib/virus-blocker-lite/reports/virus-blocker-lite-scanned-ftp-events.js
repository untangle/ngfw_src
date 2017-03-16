{
    "category": "Virus Blocker Lite",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","uri","virus_blocker_lite_clean","virus_blocker_lite_name","c_client_addr","s_server_addr"],
    "description": "All FTP sessions scanned by Virus Blocker Lite.",
    "displayOrder": 1030,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "ftp_events",
    "title": "Scanned Ftp Events",
    "uniqueId": "virus-blocker-lite-KX1K3Q0IOC"
}
