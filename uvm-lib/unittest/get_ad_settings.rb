address_book = Untangle::RemoteUvmContext.appAddressBook()

settings = address_book.getAddressBookSettings()

puts settings.inspect
