package news.reporter

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.consumer.KafkaReadStream
import org.apache.kafka.common.serialization.StringDeserializer

class ReceiveTopHeadlineVerticle : AbstractVerticle() {
    override fun start(promise: Promise<Void>) {
        val consumer = KafkaReadStream.create(vertx, mapOf(
                "bootstrap.servers" to config().getJsonArray("brokers").toSet().joinToString(","),
                "group.id" to config().getString("group")),
                StringDeserializer(),
                StringDeserializer())

        consumer.handler { record ->
            vertx.eventBus().publish("top-headline", JsonObject(record.value()))

            println("${Thread.currentThread().id} - published record ${record.value()}")
        }

        consumer.subscribe(setOf(config().getString("topic"))) {
            if (it.succeeded()) promise.complete() else promise.fail(it.cause())
        }
    }
}