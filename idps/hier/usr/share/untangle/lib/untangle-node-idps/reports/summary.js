{
    "uniqueId": "intrusion-prevention-kt095LB6",
    "category": "Intrusion Prevention",
    "description": "A summary of intrusion detection and prevention actions.",
    "displayOrder": 19,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "textColumns": [
        "count(*) as detected",
        "sum(blocked::int) as blocked"
    ],
    "textString": "Intrusion Prevention detected {0} attacks of which {1} were blocked.", 
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Intrusion Prevention Summary",
    "type": "TEXT"
}
