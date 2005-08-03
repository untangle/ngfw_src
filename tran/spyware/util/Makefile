all: clean build
	cp ./cookie.txt ../resources/spyware/com/metavize/tran/spyware/ 
	cp ./blocklist.reg ../resources/spyware/com/metavize/tran/spyware/ 
	cp ./urlblacklist.txt ../resources/spyware/com/metavize/tran/spyware/ 
	cp ./subnet.txt ../resources/spyware/com/metavize/tran/spyware/ 

build: url activex cookie subnet

url:
	@echo -e "\n\n\nCreating URL List... "
	@( cd ./files ; wget http://www.geocities.com/spywarekilla/hosts.zip && unzip hosts.zip)
	@( cd ./files ; wget http://www.mvps.org/winhelp2002/hosts.txt )
	@cat ./files/hosts > urlblacklist
	@cat ./files/hosts.txt >> urlblacklist
	@cat urlblacklist | grep -v "^#.*" | grep "^127.0.0.1" | awk '{print $$2}' | sed 's/\#.*//' | sort > urlblacklist.txt
	@rm -f urlblacklist
	@echo -e "\n\ndone (urlblacklist.txt) \n\n"

activex:
	@echo -e "\n\n\nCreating ActiveX List... "
	@( cd ./files ; wget http://www.spywareguide.com/blocklist.reg )
	@cp  ./files/blocklist.reg .
	@echo -e "\n\ndone (blocklist.reg) \n\n"

cookie:
	@echo -e "\n\n\nCreating Cookie List... "
	@( cd ./files ; wget http://www.geocities.com/spywarekilla/autoinstaller.zip  && unzip autoinstaller.zip)
	@cat ./files/Spyware\ blacklist\ auto-installer.reg | tr -d '\377' | tr -d '\376' | tr -d '\000' >| ./files/cookies
	@cat ./files/cookies | grep HKEY | sed 's/^.*History\\\(.*\)\]/\1/' | grep -v HKEY > ./files/cookie.txt.tmp
	@cat ./files/cookie.txt.tmp | \
	egrep -v "(google.com|yahoo.com|cnet.com|paypal.com|msn.com|microsoft.com|hotmail.com|macromedia.com|lycos.com|bestbuy.com|aol.com|amazon.com)" > cookie.txt
	@echo -e "\n\ndone (cookie.txt) \n\n"

subnet:
	@echo -e "\n\n\nCreating subnet List..."
	@( cd ./files ; wget http://www.geocities.com/yosponge/blockips.txt)
	@cat ./files/blockips.txt | tr -d '\r' | egrep '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $$1 " " $$2 " " $$3}' > ./files/blockips2.txt
	@cat ./files/blockips2.txt | egrep -v "single IP" | egrep -v "Use a" | sed "s/^ //" > ./files/blockips3.txt
	@cat ./files/blockips3.txt | egrep '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}.*[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > ./files/blockips-mask.txt
	@cat ./files/blockips3.txt | egrep -v '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}.*[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > ./files/blockips-single.txt
	@rm -f ./files/subnet.txt
	@cat ./files/blockips-single.txt | awk '{print $$1 " mask 255.255.255.255,Malware," $$2 }' >> ./files/subnet.txt
	@cat ./files/blockips-mask.txt | awk '{print $$1 " mask " $$2 ",Malware," $$3 }' >> ./files/subnet.txt
	@cat ./files/subnet.txt | egrep '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3} mask [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > ./files/subnet.txt.tmp
	@cat ./files/subnet.txt.tmp | egrep -v -i "(Microsoft|MSN)" > ./subnet.txt
	@echo -e "\n\ndone (none) \n\n"

clean:
	rm -f ./cookie.txt 
	rm -f ./blocklist.reg 
	rm -f ./urlblacklist.txt 
	rm -f ./subnet.txt 
	rm -f ./files/*
