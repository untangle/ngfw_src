{
    "uniqueId": "virus-blocker-ugosjuGk",
    "category": "Virus Blocker",
    "description": "A summary of virus blocking actions for FTP activity.",
    "displayOrder": 3,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "sum(case when virus_blocker_clean is not null then 1 else 0 end) as scanned",
        "sum(case when virus_blocker_clean is false then 1 else 0 end) as blocked"
    ],
    "textString": "Virus Blocker scanned {0} FTP requests of which {1} were blocked.", 
    "readOnly": true,
    "table": "ftp_events",
    "title": "Virus Blocker FTP Summary",
    "type": "TEXT"
}
