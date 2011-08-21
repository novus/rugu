# rugu: a Scala DSL for SSH

A REPL session, starting with the Ssh object which handles scoped connections
and executions. More authentication methods are coming...

    import com.novus.rugu._
    
    scala> val ssh = new Ssh(user = "xx", password = "xx", host = "xx", knownHosts = Some("/known_hosts"))
    ssh: com.novus.rugu.Ssh = com.novus.rugu.Ssh@2a41da1c
    
Let's add 1 to a random number from the server.
    
    scala> ssh("echo $RANDOM" :| { (_:Int) + 1 })
    res0: Either[Throwable,Int] = Right(1639)
    
What about users in the system?

    scala> ssh("cat /etc/passwd | cut -d ':' -f 1" :| { (_:List[String]).map(identity) })
    res1: Either[Throwable,List[String]] = Right(List(root, bin, daemon, adm, lp, sync, shutdown ...
    
Output can be ignored.

    scala> ssh("touch new_file")                                                         
    res2: Either[Throwable,Unit] = Right(())
    
Or it can be redirected to a local file (currently only character streams):

    scala> ssh("hostname" :> "host.txt")
    res3: Either[Throwable,Unit] = Right(())
    
Concatenation (of character streams) to a file is supported:
    
    scala> ssh("echo \"hello `hostname`!\"" :>> "host.txt" )      
    res4: Either[Throwable,Unit] = Right(())
    
### Notes

* Pipe (`:|`) operations are type-safe; instances of the `StreamProcessor`
  type class transform the raw stream into the format requred by the function.
* A `StreamProcessor` identity instance is provided if you need to get at the
  raw stream.
