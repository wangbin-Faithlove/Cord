import requests
import json

SLICES_URL = "http://192.168.121.86:8888/api/core/slices/"
PORTS_URL = "http://192.168.121.86:8888/api/core/ports/"
NETWORKS_URL = "http://192.168.121.86:8888/api/core/controllernetworks/"


user = "padmin@vicci.org"
psw  = "letmein"
auth = (user, psw)


def getJsonData(url):
	r = requests.get(url, auth=auth)
	return r.json()

def getSlicesName(url):
	slices = getJsonData(url)
	MANAGEMENT = "management"
	PUBLIC = "public"
	slices_name = []
	slices_name = [i["name"] for i in slices if not (i["name"].endswith(MANAGEMENT) or i["name"].endswith(PUBLIC))]
	return slices_name

def getPortNumbers(url, slicesName):
	ports = getJsonData(url)
	portNumbers = [[p["id"],p["network"][-2]] for s in slicesName for p in ports if p["humanReadableName"].startswith(s)]
	return portNumbers

def getNetworkId(url, ports):
	networks = getJsonData(NETWORKS_URL)
	netIds = [network["net_id"] for p in ports for network in networks if str(network["id"]) == str(p[1])]
	return netIds

def slicesInfo():
	slicesName = getSlicesName(SLICES_URL)
	ports = getPortNumbers(PORTS_URL, slicesName)
	networksId = getNetworkId(NETWORKS_URL, ports)
	portNumbers = [p[0] for p in ports]

	slices = {}
	for i in range(len(slicesName)):
		slices[slicesName[i]] = {"portNumber":portNumbers[i], "networkId": networksId[i]}

	return slices

if __name__ == "__main__":
	slices = slicesInfo()
	print json.dumps(slices)
