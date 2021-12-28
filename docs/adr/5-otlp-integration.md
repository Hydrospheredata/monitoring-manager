# OpenTelemetry collector integration

* Status: accepted
* Deciders: Bulat Lutfullin
* Date: 2021-12-24

Technical Story: description

## Context and Problem Statement

Prometheus PushGateway integration messes up metric timestamps, making them depend on poll configuration of prometheus.
In addition to that Prometheus doesn't support insert-into-past, thus making old metrics unimportable.

## Decision Drivers

* Prometheus PushGateway doesn't respect metric timestamps
* Prometheus RemoteWrite API is not properly documented and unstable
* OpenTelemetry has good GRPC API
* OpenTelemetry collector has a variety of different exporters

## Considered Options

* Implement OTLP exporter
* Implement RemoteWrite exporter

## Decision Outcome

Chosen option: "Implement OTLP exporter", because it's stable, has GRPC, supports historical metrics, and has many integrations.

## Links

* Replaces [ADR-4](/docs/adr/4-prometheus-integration.md)
* Uses [OTLP GRPC API](https://github.com/open-telemetry/opentelemetry-proto/blob/c3e79b1bfb9b94d9b1d473e89f2fb4fd18738f7f/opentelemetry/proto/collector/metrics/v1/metrics_service.proto)
* OTLP can export to Prometheus using [RemoteWrite](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter/prometheusremotewriteexporter)

<!-- markdownlint-disable-file MD013 -->
