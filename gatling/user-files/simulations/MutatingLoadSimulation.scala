import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class MutatingLoadSimulation extends Simulation {
  val baseUrl = sys.env.getOrElse("TARGET_BASE", "http://httpbin:8080")

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json, text/html;q=0.9,*/*;q=0.8")
    .userAgentHeader("Gatling-Mutator")

  val queryFeeder = Iterator.continually(Map(
    "q"     -> Vector("", "page=1", "page=2&limit=50", "q=test", "q=%F0%9F%92%A9")(util.Random.nextInt(5)),
    "delay" -> util.Random.nextInt(3).toString,
    "code"  -> Vector("200","201","204","400","500")(util.Random.nextInt(5))
  ))

  val headersFeeder = Iterator.continually(Map(
    "accept" -> (if (util.Random.nextBoolean()) "application/json" else "text/html"),
    "trace"  -> ("gatling-" + util.Random.nextInt(999999).toString)
  ))

  val s = scenario("Mutating load")
    .feed(queryFeeder).feed(headersFeeder)
    .randomSwitch(
      40d -> exec(http("GET /get")
        .get(session => if (session("q").as[String].isEmpty) "/get" else s"/get?${session("q").as[String]}")
        .header("Accept", "${accept}").header("X-Trace", "${trace}")
        .check(status.in(200,304))),
      25d -> exec(http("POST /post")
        .post("/post").header("Content-Type","application/json").header("X-Trace","${trace}")
        .body(StringBody("""{ "id": ${__Random(1,10000)}, "note": "mut" }""")).asJson
        .check(status.is(200))),
      25d -> exec(http("GET /delay/{n}")
        .get(session => s"/delay/${session("delay").as[String]}")
        .header("Accept", "${accept}")
        .check(status.is(200))),
      10d -> exec(http("GET /status/{code}")
        .get(session => s"/status/${session("code").as[String]}")
        .check(status.is(session("code").as[String].toInt)))
    )

  setUp(
    s.inject(
      rampUsersPerSec(1) to 50 during (30.seconds),
      constantUsersPerSec(50) during (30.seconds),
      rampUsersPerSec(50) to 0 during (15.seconds)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.percentile4.lte(1200),
    global.failedRequests.percent.lte(1.0)
  )
}
