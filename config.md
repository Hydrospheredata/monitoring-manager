
## Configuration Details


|FieldName |Format       |Description|Sources|
|---       |---          |---        |---    |
|[aws](aws)|[all-of](aws)|           |       |

### aws

|FieldName                 |Format                   |Description                                                                                                                                      |Sources|
|---                       |---                      |---                                                                                                                                              |---    |
|region                    |primitive                |value of type string, optional value, AWS region to connect to                                                                                   |       |
|[credentials](credentials)|[any-one-of](credentials)|default value: DefaultCredentialsProvider(providerChain=LazyAwsCredentialsProvider(delegate=Lazy(value=Uninitialized))), AWS credentials provider|       |
|endpointOverride          |primitive                |value of type uri, optional value, Overrides the AWS service endpoint                                                                            |       |
|[client](client)          |[all-of](client)         |optional value, Common settings for AWS service clients                                                                                          |       |

### credentials

|FieldName|Format                     |Description         |Sources|
|---      |---                        |---                 |---    |
|         |primitive                  |value of type string|       |
|         |primitive                  |value of type string|       |
|         |[all-of](fielddescriptions)|                    |       |

### Field Descriptions

|FieldName      |Format   |Description                                |Sources|
|---            |---      |---                                        |---    |
|accessKeyId    |primitive|value of type string, AWS access key ID    |       |
|secretAccessKey|primitive|value of type string, AWS secret access key|       |

### client

|FieldName                   |Format              |Description                                                                                                        |Sources|
|---                         |---                 |---                                                                                                                |---    |
|[extraHeaders](extraheaders)|[list](extraheaders)|Extra headers to be sent with each request                                                                         |       |
|apiCallTimeout              |primitive           |value of type duration, optional value, Amount of time to allow the client to complete the execution of an API call|       |
|apiCallAttemptTimeout       |primitive           |value of type duration, optional value, Amount of time to wait for the HTTP request to complete before giving up   |       |
|defaultProfileName          |primitive           |value of type string, optional value, Default profile name                                                         |       |

### extraHeaders

|FieldName|Format                         |Description                      |Sources|
|---      |---                            |---                              |---    |
|name     |primitive                      |value of type string, Header name|       |
|         |[any-one-of](fielddescriptions)|                                 |       |

### Field Descriptions

|FieldName|Format   |Description                       |Sources|
|---      |---      |---                               |---    |
|value    |list     |value of type string, Header value|       |
|value    |primitive|value of type string, Header value|       |


## Configuration Details


|FieldName           |Format            |Description|Sources|
|---                 |---               |---        |---    |
|[endpoint](endpoint)|[all-of](endpoint)|           |       |

### endpoint

|FieldName         |Format   |Description                           |Sources|
|---               |---      |---                                   |---    |
|httpHost          |primitive|value of type uri                     |       |
|grpcPort          |primitive|value of type int                     |       |
|httpMaxRequestSize|primitive|value of type int, default value: 8192|       |
|httpPort          |primitive|value of type int                     |       |


## Configuration Details


|FieldName         |Format           |Description                                                               |Sources|
|---               |---              |---                                                                       |---    |
|[metrics](metrics)|[all-of](metrics)|optional value, Configures Prometheus Pushgateway access. Optional config.|       |

### metrics

|FieldName     |Format         |Description      |Sources|
|---           |---            |---              |---    |
|url           |primitive      |value of type URL|       |
|[creds](creds)|[all-of](creds)|optional value   |       |

### creds

|FieldName|Format   |Description         |Sources|
|---      |---      |---                 |---    |
|username |primitive|value of type string|       |
|password |primitive|value of type string|       |

