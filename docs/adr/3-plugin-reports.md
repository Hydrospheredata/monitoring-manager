# Implementation of Reports API

* Status: accepted 
* Deciders: hydroteam 
* Date: 16 Nov 2021 

Technical Story: description

## Context and Problem Statement

Need to implement a single point of access for users to view all information that plugins calculated.

## Decision Drivers

* No strict data schema for plugin results. Data drift and profiler calculate completely different data without strict schema.
* There is no single persistence layer for plugins. Plugins can utilize whatever they want to save data.
* Plugins implement their own API and UI. Manager service doesn't know about all this.

## Considered Options

* Implement ACK with reports.
* Implement a plugin that can aggregate info from other plugins.
* Enforce single persistence layer.
* Don't implement it at all

## Decision Outcome

Chosen option: "Implement ACK with reports", because it comes out best in terms of effort and abstractions. 

### Positive Consequences

* GRPC API is generic to some extent.
* ACK + useful info about file
* Centralized storage and handling of reports.

### Negative Consequences

* Might be too rigid - API is derived only for 2 specific plugins in mind.
* Low tech expertize with bidirectional zio-grpc

## Links <!-- optional -->

* GRPC API for reports [here](/src/main/protobuf/monitoring_manager.proto#L128-L157)
* The solution could be represented by this [Architecture diagram](/docs/img/arch.png) 

<!-- markdownlint-disable-file MD013 -->
