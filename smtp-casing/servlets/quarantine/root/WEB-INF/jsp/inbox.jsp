<!DOCTYPE html>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>
<html xmlns:uvm="http://java.untangle.com/jsp/uvm">
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>Quarantine Digest</title>
    <style type="text/css">
        @import "/ext6/classic/theme-gray/resources/theme-gray-all.css";
    </style>
    <uvm:skin src="quarantine.css" name="${skinName}"/>
    <script type="text/javascript" src="/ext6/ext-all.js"></script>
    <script type="text/javascript" src="/ext6/classic/theme-gray/theme-gray.js"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/inbox.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Inbox.init({
                token : '${currentAuthToken}',
                address : '${currentAddress}',
                forwardAddress : '${forwardAddress}',
                companyName: '${companyName}',
                currentAddress: '${currentAddress}',
                quarantineDays : '${quarantineDays}',
                safelistData : ${safelistData},
                remapsData : ${remapsData}
            })
        });
    </script>
 </head>
<body>
</body>
</html>
