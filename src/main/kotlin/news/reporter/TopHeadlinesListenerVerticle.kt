package news.reporter

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.kafka.client.consumer.KafkaReadStream
import org.apache.kafka.common.serialization.StringDeserializer


class TopHeadlinesListenerVerticle : AbstractVerticle() {
    override fun start(promise: Promise<Void>) {
        val consumer = KafkaReadStream.create(vertx, mapOf(
                "bootstrap.servers" to config().getJsonArray("brokers").toSet().joinToString(","),
                "group.id" to config().getString("group")),
                StringDeserializer(),
                StringDeserializer())

        consumer.handler { record -> println("${Thread.currentThread().id} - record ${record.value()}") }

        consumer.subscribe(setOf(config().getString("topic"))) {
            if (it.succeeded()) promise.complete() else promise.fail(it.cause())
        }
    }
}