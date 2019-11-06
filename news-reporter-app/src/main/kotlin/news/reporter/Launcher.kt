package news.reporter

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject

private val THROW_EXCEPTION_IF_DEPLOYMENT_FAILS = { name: String ->
    { ar: AsyncResult<String> -> check(ar.succeeded()) { "Cannot deploy verticle $name due to ${ar.cause()}" } }
}

class Launcher : AbstractVerticle() {

    override fun start() {
        val configRetriever = ConfigRetriever.create(
                vertx,
                ConfigRetrieverOptions()
                        .addStore(ConfigStoreOptions().setType("file").setConfig(JsonObject().put("path", "news/reporter/verticle-config.json"))))

        configRetriever.getConfig {
            vertx.deployVerticle(
                    HttpServerVerticle::class.java,
                    DeploymentOptions().setConfig(JsonObject()
                            .put("http-server", it.result().getJsonObject("http-server"))
                            .put("data-store", it.result().getJsonObject("data-store"))),
                    THROW_EXCEPTION_IF_DEPLOYMENT_FAILS("HttpServerVerticle"))

            vertx.deployVerticle(
                    ReceiveTopHeadlineVerticle::class.java,
                    DeploymentOptions()
                            .setWorker(true)
                            .setConfig(it.result().getJsonObject("top-headlines")),
                    THROW_EXCEPTION_IF_DEPLOYMENT_FAILS("ReceiveTopHeadlineVerticle"))

            vertx.deployVerticle(
                    StoreTopHeadlineVerticle::class.java,
                    DeploymentOptions()
                            .setWorker(true)
                            .setConfig(it.result().getJsonObject("data-store")),
                    THROW_EXCEPTION_IF_DEPLOYMENT_FAILS("StoreTopHeadLineVerticle"))
        }
    }
}