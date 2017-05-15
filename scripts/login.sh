import urllib2
import urllib

posturl="http://10.3.8.211/"
header={"User-Agent":"Mozilla/5.0 (compile;MSIE 10.0; Windows NT 6.1; Trident/6.0)",
        "Referer":"http://10.3.8.211/"
        }
postdata={
        "DDDDD":"****",
        "upass":"***",
        "savePWD":"0",
        "0MKKey":""
}
postData=urllib.urlencode(postdata)
request=urllib2.Request(posturl,postData,header)
response=urllib2.urlopen(request)
