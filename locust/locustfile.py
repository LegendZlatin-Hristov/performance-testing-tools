from locust import HttpUser, task, between
import json


class HttpBinUser(HttpUser):
    host = "http://host.docker.internal:8080"

    @task(1)
    def get_root(self):
        self.client.get("/get", name="/get")

    @task(2)
    def get_status_200(self):
        self.client.get("/status/200", name="/status/200")

    @task(3)
    def get_status_500(self):
        self.client.get("/status/500", name="/status/500")

    @task(4)
    def get_delay(self):
        self.client.get("/delay/1", name="/delay/1")

    @task(5)
    def post_endpoint(self):
        self.client.post(
            "/post",
            json={"username":"foo", "password":"bar"},
            name="/post"
        )
