# trade-imports-stub

This is a stub service to mock out responses from third party integrations for the Trade Imports project.
Locally, it is set-up to run on port 8087.

It is also available on the shared cdp environments if needed at the usual urls.


### Latency profiles

This stub stands in for two integrations that `trade-imports-reference-data` calls, and gives each its own latency profile:

| Integration | Stands in for | Paths |
|---|---|---|
| `trade-token` | The Trade Platform OAuth token endpoint, called before MDM | `POST /tenant/oauth2/v2.0/token` |
| `mdm` | MDM reference data through APIM | `GET /mdm/geo/countries`, `GET /mdm/trade/bcp/poes` |

Each profile is one of:

- `zero-delay`: adds no delay. This is the default, so local runs, the E2E suites and the pull-request smoke run stay as fast as ever.
- `sla`: delays each answer by a draw from a lognormal distribution fitted to the integration's targets. The median is matched exactly and the tail is the least-squares fit to p95 and p99 in log space. The interim targets are p50 100 ms, p95 400 ms and p99 1,000 ms, which fit to p95 470 ms and p99 892 ms. They are interim and unagreed until section 9.5 of the volumetrics page agrees them.

The profile is chosen when the container starts, so one image serves every environment. Set `STUB_LATENCY_PROFILE` for every integration, or `STUB_LATENCY_TRADE_TOKEN_PROFILE` or `STUB_LATENCY_MDM_PROFILE` for one. The targets are overridable as `STUB_LATENCY_<TRADE_TOKEN|MDM>_P50_MS`, `_P95_MS` and `_P99_MS` (defaults 100, 400 and 1000). CDP perf-test sets `STUB_LATENCY_PROFILE=sla` in cdp-app-config; that change is made by a person, not by this repo.

`GET /latency-profiles` reports, for each integration: `integration`, `interface`, `owner`, `serviceLevelSource`, `agreed`, `lastConformed`, `profile`, `slaTargets`, `fitted`, `targets` (the targets the running profile aims for, zero for `zero-delay`) and `answered` (`count`, `p50Ms`, `p95Ms`, `p99Ms`). `answered` is measured from the stub receiving a request to its response being ready. `count` is every answer since the stub started or the last clear, and the percentiles are over a random sample of up to 10,000 of them, on the instance that answers the read. `DELETE /latency-profiles/answered` forgets every integration's answers and returns 204, so a run's report covers only that run. `lastConformed` is empty until the conformance run sets it.

The metadata is set when the container starts, like the profile:

| Variable | Purpose | Default |
|---|---|---|
| `STUB_LATENCY_TRADE_TOKEN_AGREED` | Whether the token endpoint's targets are agreed | `false` |
| `STUB_LATENCY_TRADE_TOKEN_LAST_CONFORMED` | Date the token endpoint's profile was last conformed, `YYYY-MM-DD` | unset |
| `STUB_LATENCY_MDM_AGREED` | Whether MDM's targets are agreed | `false` |
| `STUB_LATENCY_MDM_LAST_CONFORMED` | Date MDM's profile was last conformed, `YYYY-MM-DD` | unset |

### About the licence

The Open Government Licence (OGL) was developed by the Controller of Her Majesty's Stationery Office (HMSO) to enable
information providers in the public sector to license the use and re-use of their information under a common open
licence.

It is designed to encourage use and re-use of information freely and flexibly, with only a few conditions.
