# Monitoring Manager

## Plugin system

1. A Plugin must expose 8088 port
2. A Plugin must implement `/plugininfo.json` route with the following schema:
```json
{
   "iconUrl": "Icon for Plugin UI",
   "routePath": "???",
   "ngModuleName": "???",
   "remoteEntry": "???",
   "remoteName": "???",
   "exposedModule": "???"
}
```

## Integration Test environment

### Database 
```shell
docker run -d -e POSTGRES_PASSWORD=root -e POSTGRES_USER=root -e POSTGRES_DB=monitoring -p 5432:5432 postgres
```
### Minio
```shell
docker run -d -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"
```

