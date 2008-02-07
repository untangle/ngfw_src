rpc = {}

rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
rpc.nodeManager = rpc.jsonrpc.RemoteUvmContext.nodeManager();

var a = rpc.nodeManager.nodeInstances('untangle-node-protofilter')
var nc = rpc.nodeManager.nodeContext(a.list[0])
var n = nc.node()
var s = n.getProtoFilterSettings()
var pl = s.patterns.list.length;

