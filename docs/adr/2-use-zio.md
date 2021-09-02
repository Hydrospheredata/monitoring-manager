# Usage of ZIO as main effect and auxillary ecosystem

* Status: accepted
* Deciders: Bulat Lutfullin
* Date: 2021-08-28

Technical Story: description 

## Context and Problem Statement

To implement Functional Programming in Scala we need to choose a library that implements Effect types.


## Decision Drivers

* We already used cats-effect in previous projects
* There are cats-compatible libraries
* Final-tagless approach, that we adopted with cats-effect in previous projects leads to a lot of manual management.
* ZIO on the other hand, has single effect type - `ZIO[R,E,T]` and leads to non TF approach that seems simpler.
* ZIO provides functional abstraction to manage the creation of services and dependencies
* ZIO has more homogenous ecosystem: a lot of applied libraries from ZIO devs and community (e.g. zio-aws, zio-k8s, etc.) that cover huge missing parts in our application.

## Considered Options

* use cats effect
* use ZIO

## Decision Outcome

Chosen option: ZIO, because it's ecosystem allows us to focus on domain, rather that wrapping third-party libraries.
Also, want to see, if ZIO's reader monad is better than TF.
