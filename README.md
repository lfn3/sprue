# sprue

Quick sketch of generating models classes as Kotlin data classes using [KotlinPoet](https://github.com/square/kotlinpoet)
and Clojure

```shell script
$ lein run
# //Test.kt
# package net.lfn3.sprue
# 
# import java.lang.String
# 
# data class Test(
#   val name: String
# )
# //TestId.kt
# package net.lfn3.sprue
# 
# import kotlin.Long
# 
# class TestId(
#   val id: Long
# ) : Id(id)
```

## License

Copyright © 2019 Liam Falconer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
