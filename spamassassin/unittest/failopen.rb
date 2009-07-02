Untangle::RemoteUvmContext.nodeManager.nodeInstances('untangle-node-spamassassin').each do |nid| Untangle::RemoteUvmContext.nodeManager.nodeContext(nid).node.enableSmtpFailClosed(false) end
