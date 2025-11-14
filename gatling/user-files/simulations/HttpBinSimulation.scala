import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class HttpBinSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://host.docker.internal:8080")
    .acceptHeader("application/json")
    .userAgentHeader("Gatling")

  val postRequest = exec(
    http("POST /post")
      .post("/post")
      .body(RawFileBody("post.json")).asJson
  )

  val scn = scenario("HttpBin scenario")
    .exec(
      http("GET /get")
        .get("/get")
    )
    .exec(
      http("GET /status/200")
        .get("/status/200")
    )
    .exec(
      http("GET /status/500")
        .get("/status/500")
    )
    .exec(
      http("GET /delay/1")
        .get("/delay/1")
    )
    .exec(postRequest)

  setUp(
    scn.inject(
      constantUsersPerSec(80).during(30.seconds)
    )
  ).protocols(httpProtocol)
}
