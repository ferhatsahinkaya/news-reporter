package news.reporter

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS

@Testcontainers
@ExtendWith(VertxExtension::class)
class ReceiveTopHeadlineVerticleIntegrationTest {

    @Container
    private val kafka = KafkaContainer()
    private val topic = "the-topic-61"

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        kafka.start()

        vertx.deployVerticle(ReceiveTopHeadlineVerticle(),
                DeploymentOptions()
                        .setConfig(JsonObject()
                                .put("brokers", JsonArray(listOf(kafka.bootstrapServers)))
                                .put("group", "consumer-group")
                                .put("topic", topic)),
                testContext.completing())
    }

    @AfterEach
    fun tearDown(vertx: Vertx) {
        vertx.close()
        kafka.stop()
    }

    @Test
    fun shouldForwardTopHeadlinesReceivedFromKafkaTopicToEventBus(vertx: Vertx, testContext: VertxTestContext) {
        val message = Instant.now().toString()
        val producer = KafkaProducer(mapOf(
                "bootstrap.servers" to kafka.bootstrapServers,
                "client.id" to "test-client-id",
                "auto.create.topics.enable" to "true"),
                StringSerializer(),
                StringSerializer())

        vertx.eventBus()
                .consumer<String>("top-headline") {
                    println("Event bus message ${it.body()} is received")
                    assertThat(it.body(), equalTo(message))
                    testContext.completeNow()
                }

        println("Before send")
        producer.send(ProducerRecord(topic, message))
                .get(3, SECONDS)
        println("After send")
        Thread.sleep(10000)
    }
}
