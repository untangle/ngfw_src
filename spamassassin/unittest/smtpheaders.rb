Untangle::RemoteUvmContext.nodeManager.nodeInstances('untangle-node-spamassassin').each do |nid| Untangle::RemoteUvmContext.nodeManager.nodeContext(nid).node.enableSmtpSpamHeaders(true) end
