# rugu: a Scala DSL for SSH

A REPL session, starting with the Ssh object which handles scoped connections
and executions.

    import com.novus.rugu._
    
    scala> val ssh = Ssh(Host("xx"), UsernameAndPassword("xx", "xx"), Some("/home/chrs/.ssh/known_hosts"))
    ssh: com.novus.rugu.SshSession = com.novus.rugu.SshSession@c7d9406
    
Let's start by printing the date on the server:
    
    ssh("date" :| println)
    Sun Aug 21 15:27:58 EDT 2011
    res2: Either[Throwable,Unit] = Right(())

Again, but this time we'll return the value as a string:
    
    scala> ssh("date" :| identity)
    res9: Either[Throwable,String] = Right(Sun Aug 21 01:13:34 EDT 2011)
    
Let's add 1 to a random number from the server.
    
    scala> ssh("echo $RANDOM" :#| { (_:Int) + 1 })
    res0: Either[Throwable,Int] = Right(1639)
    
What about users in the system?

    scala> ssh("cat /etc/passwd | cut -d ':' -f 1" ::| { _.map(identity) })
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
    
Input can be provided to the remote command:

    scala> ssh("hello\nhi\nsalut" |: "grep hi" :| identity)
    res2: Product with Either[Throwable,String] = Right(hi)
    
Execution will fail as a Left value if the command returns a non-zero exit status:

    scala> ssh("hello world" |: "grep hi" :| identity)                               
    res4: Product with Either[Throwable,String] = Left(java.lang.RuntimeException: Non-zero exit status! 1)
    
You can short circuit the failure on non-zero by handling the result explicitly:
    
    scala> ssh.exec("fhh\nhi" |: "grep hi && dwd" :| identity) { case (s, _, e) => s -> e.toString }                      
    res1: Product with Either[Throwable,(Int, java.lang.String)] = 
    Right((127,bash: dwd: command not found
    ))
    
The explicit handler receives a Tuple3 of the exit status, transformed result,
and the error stream as a ByteArrayOutputStream.
    
### Notes

* With the exception of `|:`, operators are all prefixed with `:` for consistency and equal precedence. 
* General pipe (`:#|`) operations are type-safe; instances of the `StreamProcessor`
  type class transform the raw stream into the format requred by the function.
* A `StreamProcessor` identity instance is provided if you need to get at the
  raw stream.
* The string pipe `:|` and string list pipe `::|` operators are specializations of the general pipe
  to `String` and `List[String]`, respectively. These are mere conveniences for common formats that
  remove inference ambiguities.
  
