import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class BasicSmokeSimulation extends Simulation {

  val baseUrl = sys.env.getOrElse("TARGET_BASE", "http://httpbin:8080")

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .userAgentHeader("Gatling-Demo")

  val rndFeeder = Iterator.continually(Map(
    "rnd" -> util.Random.nextInt(100000),
    "ts"  -> System.currentTimeMillis()
  ))

  val smokeScenario = scenario("Smoke: httpbin basics")
    .feed(rndFeeder)
    .exec(http("GET /get").get("/get?rnd=${rnd}").check(status.in(200,304)))
    .pause(200.milliseconds, 800.milliseconds)
    .exec(http("POST /post")
      .post("/post").body(StringBody("""{ "t": ${ts}, "note": "smoke" }""")).asJson
      .check(status.is(200)))
    .pause(200.milliseconds, 800.milliseconds)
    .exec(http("GET /status/200").get("/status/200").check(status.is(200)))

  setUp(
    smokeScenario.inject(atOnceUsers(1), rampUsers(10).during(10.seconds))
      .protocols(httpProtocol)
  ).assertions(
    global.responseTime.percentile3.lte(800),
    global.successfulRequests.percent.gte(99.0)
  ).maxDuration(30.seconds)
}
