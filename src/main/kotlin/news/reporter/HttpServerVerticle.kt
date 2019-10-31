package news.reporter

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import kotlin.properties.Delegates.notNull

class HttpServerVerticle : AbstractVerticle() {
    private var httpServer: HttpServer by notNull()

    override fun start(promise: Promise<Void>) {
        val mongoClient = MongoClient.createShared(vertx, config().getJsonObject("data-store"))

        val router: Router = Router.router(vertx)
        router.get("/top-headlines")
                .handler { routingContext ->
                    mongoClient.findBatch("top-headlines", JsonObject())
                            .handler {
                                println(it.toString())
                                routingContext
                                        .response()
                                        .setChunked(true)
                                        .write(it.toString())
                            }.endHandler {
                                routingContext.response().end()
                            }
                            .exceptionHandler {
                                it.printStackTrace()
                            }
                }

        httpServer = vertx.createHttpServer()
                .requestHandler(router)
                .listen(config().getJsonObject("http-server").getInteger("http.port")) {
                    if (it.succeeded()) promise.complete() else promise.fail(it.cause())
                }
    }
}