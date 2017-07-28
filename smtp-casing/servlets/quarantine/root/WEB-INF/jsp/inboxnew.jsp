<!DOCTYPE html>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html xmlns:uvm="http://java.untangle.com/jsp/uvm">
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>${companyName}</title>

    <!-- FontAwesome -->
    <link href="/ext6.2/fonts/font-awesome/css/font-awesome.min.css" rel="stylesheet" />

    <!-- JsonRPC -->
    <script src="/jsonrpc/jsonrpc.js"></script>

    <!-- ExtJS lib & theme -->
    <c:set var="debug" value="${param['debug']}"/>
    <c:choose>
        <c:when test="${debug == '1'}">
    <script src="/ext6.2/ext-all-debug.js"></script>
        </c:when>
        <c:otherwise>
    <script src="/ext6.2/ext-all.js"></script>
        </c:otherwise>
    </c:choose>
    <script src="/ext6.2/classic/theme-${extjsTheme}/theme-${extjsTheme}.js"></script>
    <link href="/ext6.2/classic/theme-${extjsTheme}/resources/theme-${extjsTheme}-all.css" rel="stylesheet" />

    <link href="css/quarantine.css" rel="stylesheet" />

    <script src="/script/common/bootstrap.js"></script>
    <script>
        var rpc;
        Ext.onReady(function () {
            // load translations first because it's a separate call
            rpc = new JSONRpcClient('/quarantine/JSON-RPC').Quarantine;

            Ext.Ajax.request({
                url : 'i18n',
                method : 'GET',
                params: { module: 'untangle' },
                success: function(response, options) {
                    rpc.translations = Ext.decode(response.responseText);
                    initApp();
                },
                failure : function() {
                    Ext.MessageBox.alert('Error', 'Unable to load the language pack.');
                    initApp();
                },
            });

            function initApp(response, options) {
                String.prototype.t = function() {
                    return rpc.translations[this.valueOf()] || this.valueOf();
                };

                Bootstrap.load([
                    '/script/common/util-all.js',
                    'script/inboxnew.js'
                ], 'QUARANTINE', function (ex) {
                    // if everything is initialized just launch the application
                    Ext.application({
                        extend: 'Ung.Inbox',
                        namespace: 'Ung',

                        conf: {
                            token: "${currentAuthToken}",
                            address: "${currentAddress}",
                            forwardAddress: "${forwardAddress}",
                            companyName: "${fn:replace(companyName,'"','')}",
                            currentAddress: "${fn:replace(currentAddress,'"','')}",
                            quarantineDays: ${quarantineDays},
                            safelistData: ${safelistData},
                            remapsData: ${remapsData}
                        }
                    });
                });
            }




            // setups all initializations and load required scrips

        });
    </script>



    <!-- <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/request.js"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Request.init({
                companyName: "${fn:replace(companyName,'"','')}",
            })
        });
    </script> -->
 </head>
<body>
</body>
</html>
