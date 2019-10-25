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
                    DeploymentOptions().setConfig(it.result().getJsonObject("http-server")),
                    THROW_EXCEPTION_IF_DEPLOYMENT_FAILS("HttpServer"))

            vertx.deployVerticle(
                    TopHeadlinesListenerVerticle::class.java,
                    DeploymentOptions().setConfig(it.result().getJsonObject("top-headlines")),
                    THROW_EXCEPTION_IF_DEPLOYMENT_FAILS("TopHeadlinesListener"))
        }
    }
}