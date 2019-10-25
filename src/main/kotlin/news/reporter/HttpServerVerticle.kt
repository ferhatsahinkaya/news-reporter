package news.reporter

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import kotlin.properties.Delegates.notNull

class HttpServerVerticle : AbstractVerticle() {
    private var httpServer: HttpServer by notNull()

    override fun start(promise: Promise<Void>) {
        val router: Router = Router.router(vertx)
        router.get("/top-headlines")
                .handler { routingContext ->
                    vertx.eventBus()
                            .request("top-headlines", "get headlines") { ar: AsyncResult<Message<String>> ->
                                if (ar.succeeded()) {
                                    routingContext.response()
                                            .write(ar.result().body())
                                            .end()
                                } else {
                                    routingContext.fail(500)
                                }
                            }
                }

        httpServer = vertx.createHttpServer()
                .requestHandler(router)
                .listen(config().getInteger("http.port")) {
                    if (it.succeeded()) promise.complete() else promise.fail(it.cause())
                }
    }
}