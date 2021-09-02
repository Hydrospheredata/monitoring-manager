build:
	docker build -t dummy-plugin .

run:
	docker run --name dummy-plugin -d -p 8088:80 --network my-overlay dummy-plugin

remove:
	docker rm -f dummy-plugin

test:
	curl localhost:8088/plugininfo.json