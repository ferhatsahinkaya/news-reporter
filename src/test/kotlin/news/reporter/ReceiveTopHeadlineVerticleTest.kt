package news.reporter

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kafka.client.producer.KafkaWriteStream
import kafka.server.KafkaConfig
import kafka.server.KafkaServerStartable
import org.apache.curator.test.TestingServer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(VertxExtension::class)
class ReceiveTopHeadlineVerticleTest {
    private val zookeeperServer = TestingServer(2181)

    private val properties = mapOf(
            "zookeeper.connect" to "localhost:2181",
            "host.name" to "localhost",
            "port" to "9999",
            "auto.create.topics.enable" to "true")

    private val kafka = KafkaServerStartable(KafkaConfig(properties))

    private val producerFor = { vertx: Vertx ->
        KafkaWriteStream.create(vertx, mapOf(
                "bootstrap.servers" to "${properties["host.name"]}:${properties["port"]}",
                "group.id" to "consumer-group"),
                StringSerializer(),
                StringSerializer())
    }

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        zookeeperServer.start()
        kafka.startup()

        vertx.deployVerticle(ReceiveTopHeadlineVerticle(),
                DeploymentOptions()
                        .setWorker(true)
                        .setConfig(JsonObject()
                                .put("brokers", JsonArray(listOf("${properties["host.name"]}:${properties["port"]}")))
                                .put("group", "consumer-group")
                                .put("topic", "the-topic")),
                testContext.completing())
    }

    @AfterEach
    fun tearDown() {
        kafka.shutdown()
        zookeeperServer.stop()
    }

    @Test
    fun shouldForwardTopHeadlinesReceivedFromKafkaTopicToEventBus(vertx: Vertx, testContext: VertxTestContext) {
        println("${Thread.currentThread().id} - shouldForwardTopHeadlinesReceivedFromKafkaTopicToEventBus")
        println("logdir : ${kafka.staticServerConfig()["log.dir"]}")

        val producer = producerFor(vertx)
        producer.send(ProducerRecord("the-topic", "value"))

        vertx.eventBus()
                .consumer<String>("top-headline") {
                    assertThat(it.body(), equalTo("value"))
                    testContext.completeNow()
                }
                .exceptionHandler {
                    testContext.failNow(testContext.causeOfFailure())
                }

        producer.close()
    }
}
