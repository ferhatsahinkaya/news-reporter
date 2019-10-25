package news.reporter

import io.vertx.core.Vertx.vertx

class Application {
    fun run() = vertx().deployVerticle(Launcher())
}

fun main() = Application().run()
