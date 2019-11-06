package news.reporter

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient

class StoreTopHeadlineVerticle : AbstractVerticle() {
    override fun start() {
        val mongoClient = MongoClient.createShared(vertx, config())

        vertx.eventBus()
                .consumer<JsonObject>("top-headline") {
                    val topHeadLine = it.body()

                    println("${Thread.currentThread().id} - received record $topHeadLine")

                    val document = JsonObject()
                            .put("_id", topHeadLine.getValue("url"))

                    topHeadLine.forEach { field -> document.put(field.key, field.value) }

                    mongoClient.save("top-headlines", document) { ar ->
                        if (ar.failed()) println(ar.cause())
                    }
                }
    }
}