
.______       _______  _______   _______  __       __    __   _______ 
|   _  \     |   ____||       \ /  _____||  |     |  |  |  | |   ____|
|  |_)  |    |  |__   |  .--.  |  |  __  |  |     |  |  |  | |  |__   
|      /     |   __|  |  |  |  |  | |_ | |  |     |  |  |  | |   __|  
|  |\  \----.|  |____ |  '--'  |  |__| | |  `----.|  `--'  | |  |____ 
| _| `._____||_______||_______/ \______| |_______| \______/  |_______|
                                                                      

v.2.0

Changelog:
- Support for multiple OpenNLP dictionaries
- Support for multiple OpenNLP regex
- Support for automatic data categorization based on dictionary name and regex name
- Bug Fixes
- Should be even faster now ;-)

Author: Redglue
Status: Stable

- This build includes Oracle and SQLServer support
- OpenNLP model included for Portuguese language (it is not that good..)
- Dictionaries are not included (xml files)
- Basic commands are:

1) java -jar RedataSense.jar --help
2) java -jar RedataSense.jar database-discovery -c 
3) java -jar RedataSense.jar database-discovery -d
4) java -jar RedataSense.jar file-discovery

Refer https://github.com/redglue/redsense for more details
