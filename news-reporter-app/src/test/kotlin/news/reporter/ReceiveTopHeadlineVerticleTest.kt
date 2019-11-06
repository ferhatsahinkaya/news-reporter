package news.reporter

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import kafka.server.KafkaConfig
import kafka.server.KafkaServerStartable
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@Disabled
@ExtendWith(VertxExtension::class)
class ReceiveTopHeadlineVerticleTest {
//    private val zookeeperServer = TestingServer()

    private val topic = "the-topic-92"
    private val zookeeperUrl = "localhost:2181"
    private val properties = mapOf(
            "zookeeper.connect" to zookeeperUrl,
            "host.name" to "localhost",
            "port" to "9999")

    private val producer = KafkaProducer(mapOf(
            "bootstrap.servers" to "${properties["host.name"]}:${properties["port"]}",
            "client.id" to "test-client-id",
            "auto.create.topics.enable" to "true"),
            StringSerializer(),
            StringSerializer())

    private val kafka = KafkaServerStartable(KafkaConfig(properties))

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
//        zookeeperServer.start()
//        Thread.sleep(4000)

        kafka.startup()

        vertx.deployVerticle(ReceiveTopHeadlineVerticle(),
                DeploymentOptions()
                        .setWorker(true)
                        .setConfig(JsonObject()
                                .put("brokers", JsonArray(listOf("${properties["host.name"]}:${properties["port"]}")))
                                .put("group", "consumer-group")
                                .put("topic", topic)),
                testContext.completing())
    }

    @AfterEach
    fun tearDown(vertx: Vertx) {
        producer.close()
        vertx.close()
        kafka.shutdown()
//        zookeeperServer.stop()
    }

    @Test
    fun shouldForwardTopHeadlinesReceivedFromKafkaTopicToEventBus(vertx: Vertx, testContext: VertxTestContext) {
        val message = "message-${Instant.now()}"
        vertx.eventBus()
                .consumer<String>("top-headline") {
                    println("Event bus message ${it.body()} is received")
                    assertThat(it.body(), equalTo(message))
                    testContext.completeNow()
                }

        Thread.sleep(4000) // TODO Remove sleep

        producer.send(ProducerRecord(topic, message))
    }
}
