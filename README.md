## Ziverge Tech Boxer

### How to run it

#### Run with SBT
For some reason piping to stdIn doesn't work when run from sbt. 
So name the executable file with `-i` and the app will spin it up and consume it's json.  
```
sbt "run -i <path-to-file-to-exec-for-generating-input> [--port <port-for-http-endpoint]>
eg:
sbt "run -i /Users/andersbohn/Downloads/blackbox.macosx --port 8080"
```
#### Run with jvm directly
Get the classpath from sbt, eg:
```
export ZTB_CP=`sbt "export runtime:fullClasspath" | tail -n 1`
```
Now pipe the executable into Main, eg:
```
~/Downloads/blackbox.macosx | java -cp $ZTB_CP Main  
```

### Usage notes

#### Ask the endpoint while the app is running
On `http"//localhost:<port>/stats`

Eg:
```
> curl localhost:8080/stats
{ "eventCount":7,
  "errorCount":1,
  "wordCount": {"baz":2,"bar":3,"foo":2} }
```
Which shows stats per window, thus resets every N events/M seconds as per windowing argument (below)

#### Windowing arguments 
Defaults config for windowing the stream are in [Config#Default](src/main/scala/domain/Config). 

They can be overridden with args, eg:  
```
~/Downloads/blackbox.macosx | java -cp $ZTB_CP Main --windowSeconds 10 --windowCount 100 
```


### TODO Notes
 - ✅ zio json for en event and stats
 - ✅ zio http ready to go
 - ✅ zio stream from file and from a spun up process too
 - ⚠️ get a Managed Layer or just a bracket for the ZStream
 - ✅ groupby/window/watermark for the zstream 
 - ✅ get the stream to store the stats in a TRef read from the endpoint
 - ✅ make it consume from standard in    
 - 🙈 fix the testclock adjust so count goes back to the total 14   
 - ✅ clean up a bit and add some docs 
 - ❔ maybe test running longer or add a super fast stress test or something?