import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js'

const ttfb = new Trend('http_ttfb');
const errors = new Rate('errors');

export const options = {
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1200'],
        errors: ['rate<0.01'],
    },
    stages: [
        { duration: '10s', target: 5 },   // smoke
        { duration: '30s', target: 50 },  // load
        { duration: '30s', target: 100 }, // stress
        { duration: '20s', target: 0 },
    ],
};

const BASE_URLS = [
    'http://host.docker.internal:8080/get',
    'http://host.docker.internal:8080/post',
    'http://host.docker.internal:8080/delay/1',
];

function mutantsFor(url) {
    const qs = [
        '', 'page=1', 'page=2&limit=50', 'q=test', 'q=%F0%9F%92%A9', // unicode/edge
    ];
    const headersList = [
        { 'Accept': 'application/json' },
        { 'Accept': 'text/html' },
        { 'Accept': 'application/json', 'X-Trace': 'k6' },
    ];
    const methods = ['GET', 'GET', 'POST']; // bias към GET
    const bodies = [
        null,
        JSON.stringify({ foo: 'bar' }),
        JSON.stringify({ id: 123, flags: ['a','b'] }),
    ];

    const combos = [];
    for (const q of qs) {
        for (const h of headersList) {
            for (const m of methods) {
                for (const b of bodies) {
                    if (m !== 'POST' && b !== null) continue;
                    combos.push({
                        url: q ? (url.includes('?') ? `${url}&${q}` : `${url}?${q}`) : url,
                        params: { headers: h, tags: { mutant_q: q || 'none', method: m } },
                        method: m,
                        body: b,
                    });
                }
            }
        }
    }
    return combos;
}

const ALL_MUTANTS = BASE_URLS.flatMap(mutantsFor);

export function handleSummary(data) {
    return {
        '/tests/out/summary.json': JSON.stringify(data, null, 2),
        '/tests/out/summary.html': htmlReport(data),
    };
}

export default function () {
    const i = Math.floor(Math.random() * ALL_MUTANTS.length);
    const m = ALL_MUTANTS[i];

    let res;
    if (m.method === 'POST') {
        res = http.post(m.url, m.body, m.params);
    } else {
        res = http.get(m.url, m.params);
    }

    ttfb.add(res.timings.waiting);
    const ok = check(res, {
        'status 2xx/3xx': (r) => r.status >= 200 && r.status < 400,
    });
    errors.add(!ok);

    sleep(1);
}
