CREATE TABLE hydrosphere.index (
  objectKey TEXT NOT NULL,
  lastObjectCreated TIMESTAMP WITH TIME ZONE NOT NULL
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