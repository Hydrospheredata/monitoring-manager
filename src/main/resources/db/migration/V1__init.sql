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
    depconfigname TEXT NOT NULL,
    image TEXT NOT NULL,
    status TEXT NOT NULL,
--  PluginInfo
    iconurl TEXT,
    routePath TEXT,
    ngModuleName TEXT,
    remoteEntry TEXT,
    remoteName TEXT,
    exposedModule TEXT
);