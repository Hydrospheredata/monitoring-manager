# Monitoring Manager

The manager service for monitoring solution.
Responsible for:
1. Model registration
2. Model data discovery (training and inference)
3. Plugin registration
4. Plugin reports aggregation
5. Proxying HTTP requests to plugins

## Plugin system

A plugin is a web service that MUST use [GRPC API for plugin management](/src/main/protobuf/monitoring_manager.proto)

Plugins can expose their own web UI as a module that will be attached to our UI.
The HTTP requests sent from the web UI are to be proxied by manager.

Lifecycle of a plugin:
1. (Optional) Internal initialization of a plugin.
2. (If plugin provides web UI) Start HTTP server.
3. Call `PluginManagementService.RegisterPlugin` GRPC method with plugin info.
4. Call `ModelCatalogService.GetModelUpdates` to get endless stream of models to work with.
5. (Optional) Process training data of models.
6. Call `DataStorageService.GetInferenceDataUpdates` to get endless stream of inference files of a model.
7. (Optional) Process inference data of models.

## Documentation
The documentation is in progress, and you can find everything related to this service in [docs](/docs) folder.  
This project also keeps track of major decisions using ADRs. You can find them in [docs/adr](/docs/adr) folder.

## Build and Test
`sbt build` - builds docs, jars, and a docker image with this service.  
`sbt testAll` - runs unit and integration tests. Integration tests require docker.

