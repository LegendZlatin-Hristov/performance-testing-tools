import http from 'k6/http';
import {sleep} from 'k6';
import {textSummary} from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import {htmlReport} from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export const options = {
    vus: 80,
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1200'],
    },
    stages: [
        { duration: '10s', target: 5 },   // smoke
        { duration: '30s', target: 50 },  // load
        { duration: '30s', target: 100 }, // stress
        { duration: '20s', target: 0 },
    ],
};

export default function () {
    const baseUrl = 'http://host.docker.internal:8080';
    const body = JSON.stringify({"foo": "bar"});
    const params = {headers: {'Content-Type': 'application/json'}};

    http.get(`${baseUrl}/get`);
    http.get(`${baseUrl}/status/200`);
    http.get(`${baseUrl}/status/500`);
    http.get(`${baseUrl}/delay/1`);
    http.post(`${baseUrl}/post`, body, params);

    sleep(1);
}

export function handleSummary(data) {
    return {
        stdout: textSummary(data, {indent: ' ', enableColors: true}),
        '/k6/report.html': htmlReport(data),
    };
}