from locust import HttpUser, task, between
import random
import urllib.parse
import json

BASE_PATHS = [
    "/", "/api/items", "/api/items/1", "/search"
]

QUERIES = ["", "page=1", "page=2&limit=50", "q=test", "q=%F0%9F%92%A9"]
HEADERS = [
    {"Accept": "application/json"},
    {"Accept": "text/html"},
    {"Accept": "application/json", "X-Trace": "locust"}
]
BODIES = [
    None,
    {"foo": "bar"},
    {"id": 123, "flags": ["a", "b"]}
]

def build_mutant(path):
    q = random.choice(QUERIES)
    h = random.choice(HEADERS)
    method = random.choice(["GET", "GET", "POST"])
    body = random.choice(BODIES) if method == "POST" else None

    url = path
    if q:
        url = f"{path}?{q}" if "?" not in path else f"{path}&{q}"

    return url, method, h, body

class WebsiteUser(HttpUser):
    wait_time = between(0.5, 1.5)

    @task(5)
    def browse(self):
        path = random.choice(BASE_PATHS)
        url, method, headers, body = build_mutant(path)
        if method == "POST":
            self.client.post(url, data=json.dumps(body), headers=headers, name=f"POST {path}")
        else:
            self.client.get(url, headers=headers, name=f"GET {path}")

    @task(1)
    def search_edge_cases(self):
        q = urllib.parse.quote("слон 🐘 ? & = %")
        self.client.get(f"/search?q={q}", headers={"Accept":"text/html"}, name="GET /search edge")
