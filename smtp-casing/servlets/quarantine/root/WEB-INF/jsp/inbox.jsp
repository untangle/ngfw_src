<!DOCTYPE html>

<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>Quarantine Digest</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css";
    </style>
    <script type="text/javascript" src="/ext5/ext-all-debug.js"></script>
    <script type="text/javascript" src="/ext5/packages/ext-theme-gray/build/ext-theme-gray.js"></script>

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
