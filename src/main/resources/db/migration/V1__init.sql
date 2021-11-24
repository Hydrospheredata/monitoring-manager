CREATE TABLE hydrosphere.model(
    name VARCHAR(256) NOT NULL,
    version NUMERIC NOT NULL,
    PRIMARY KEY (name, version),
    signature TEXT NOT NULL,
    metadata TEXT NOT NULL,
    trainingDataPrefix TEXT,
    inferenceDataPrefix TEXT
);

CREATE TABLE hydrosphere.plugin(
    name VARCHAR(256) NOT NULL PRIMARY KEY,
    description TEXT NOT NULL,
--  PluginInfo
    iconurl TEXT,
    routePath TEXT,
    ngModuleName TEXT,
    remoteEntry TEXT,
    remoteName TEXT,
    exposedModule TEXT,
    addr TEXT NOT NULL
);

CREATE TABLE hydrosphere.report(
    pluginId VARCHAR(256) NOT NULL,
    modelName VARCHAR(256) NOT NULL,
    modelVersion NUMERIC NOT NULL,
    file TEXT NOT NULL,
    fileModifiedAt TIMESTAMP NOT NULL,
    batchStats TEXT NOT NULL,
    featureReports TEXT NOT NULL
);