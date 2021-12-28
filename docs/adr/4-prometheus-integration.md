# Integration of Monitoring Manager with Prometheus

* Status: accepted, superseded by [ADR-5](/docs/adr/5-otlp-integration.md) 
* Deciders: Konstantin Makarychev, Bulat Lutfullin 
* Date: 2021-12-24

Technical Story: description

## Context and Problem Statement

For monitoring purposes we need to implement an integration with popular monitoring tool.
Essentially, we want to expose high-level metrics, so users can use them in their monitoring solution.

## Decision Drivers <!-- optional -->

* We need to integrate with popular monitoring solutions. 
* The nature of our metrics require some kind of Push api, rather that Pull. 

## Considered Options

* Prometheus Exporter (Pull based API)
* Prometheus PushGateway (Push based API)
* Prometheus AlertManager (as implemented in sonar project) 

## Decision Outcome

Chosen option: "PushGateway", because Push api suits us, and we don't restrict users with alerts configuration. 

## Links <!-- optional -->

* Implemented for [ADR-3](/docs/adr/3-plugin-reports.md)
* AlertManager implementation in [Sonar](https://github.com/Hydrospheredata/sonar/blob/020435f679e0b432c3858a5e13a1a2a7aae4a742/src/main/scala/io/hydrosphere/sonar/services/AlertManagerService.scala)

<!-- markdownlint-disable-file MD013 -->
