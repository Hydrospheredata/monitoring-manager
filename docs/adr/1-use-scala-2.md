# Use Scala 2.13 as programming language for this project

* Status: accepted
* Deciders: Konstantin Makarychev, Bulat Lutfullin
* Date: 2021-08-28

Technical Story: description

## Context and Problem Statement
We need to choose the programming language for the project. We use Scala 2.13 and Python 3 in previous projects.
But Scala 3 emerged from unstable branches and is a hot topic in dev discussions.

## Decision Drivers <!-- optional -->

* Scala 3 doesn't have mature tools and ecosystem
* Scala 3 has new syntax
* Scala 3 implemented new concepts that could make development easier (enums, given, etc...)
* Scala 2.13 has stable ecosystem
* Team knows Scala 2.13 syntax and semantics

## Considered Options

* use Scala 2.13
* use Scala 3

## Decision Outcome

Chosen option: Scala 2.13. We tried Scala 3 but IDEs are unstable and not all libraries support it.
Need to wait some time before the ecosystem matures.

### Positive Consequences <!-- optional -->

* Team already has experience working with 2.13
* IDEs are stable
* All required libraries are already supported

### Negative Consequences <!-- optional -->

* Delayed migraion to Scala 3
* Scala 3 introduced new syntax that could make development easier.