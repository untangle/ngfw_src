<!DOCTYPE html>

<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>${companyName} | Request Quarantine Digest</title>
    <style type="text/css">
        @import "/ext6/classic/theme-gray/resources/theme-gray-all.css";
    </style>
    <script type="text/javascript" src="/ext6/ext-all.js"></script>
    <script type="text/javascript" src="/ext6/classic/theme-gray/theme-gray.js"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/request.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Request.init({
                companyName: '${companyName}',
            })
        });
    </script>
 </head>
<body>
</body>
</html>
