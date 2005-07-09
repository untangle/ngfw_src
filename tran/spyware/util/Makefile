clean:
	rm -f ./cookie.txt 
	rm -f ./blocklist.reg 
	rm -f ./urlblacklist.txt 
	rm -f ./files/*

all: get
	cp ./cookie.txt ../resources/spyware/com/metavize/tran/spyware/ 
	cp ./blocklist.reg ../resources/spyware/com/metavize/tran/spyware/ 
	cp ./urlblacklist.txt ../resources/spyware/com/metavize/tran/spyware/ 

get:
	@rm -rf ./files/*

#
#	Registry URL blacklist
#
	@echo -e "\n\n\nCreating URL List... "
	@( cd ./files ; wget http://www.geocities.com/spywarekilla/hosts.zip && unzip hosts.zip)
	@( cd ./files ; wget http://www.mvps.org/winhelp2002/hosts.txt )
	@cat ./files/hosts > urlblacklist
	@cat ./files/hosts.txt >> urlblacklist
	@cat urlblacklist | grep -v "^#.*" | grep "^127.0.0.1" | awk '{print $$2}' | sed 's/\#.*//' | sort > urlblacklist.txt
	@rm -f urlblacklist
	@echo -e "\n\ndone (urlblacklist.txt) \n\n"

#
#	Registry ActiveX blocklist 
#
	@echo -e "\n\n\nCreating ActiveX List... "
	@( cd ./files ; wget http://www.spywareguide.com/blocklist.reg )
	@cp  ./files/blocklist.reg .
	@echo -e "\n\ndone (blocklist.reg) \n\n"


#
#	Cookie blacklist
#
	@echo -e "\n\n\nCreating Cookie List... "
	@( cd ./files ; wget http://www.geocities.com/spywarekilla/autoinstaller.zip  && unzip autoinstaller.zip)
	cat ./files/Spyware\ blacklist\ auto-installer.reg | tr -d '\377' | tr -d '\376' | tr -d '\000' >| ./files/cookies
	cat ./files/cookies | grep HKEY | sed 's/^.*History\\\(.*\)\]/\1/' > cookie.txt
	@echo -e "\n\ndone (cookie.txt) \n\n"


#
#	Subnet blacklist
#	
	@echo -e "\n\n\nCreating subnet List..."
	@echo -e "\n\ndone (none) \n\n"
