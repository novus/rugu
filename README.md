# rugu: a Scala DSL for SSH

    scala> ssh("echo $RANDOM" :| { (_:Int) + 1 })
    res0: Either[Throwable,Int] = Right(1639)

    scala> ssh("cat /etc/passwd | cut -d ':' -f 1" :| { (_:List[String]).map(identity) })
    res1: Either[Throwable,List[String]] = Right(List(root, bin, daemon, adm, lp, sync, shutdown ...
