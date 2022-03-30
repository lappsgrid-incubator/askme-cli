NAME=web
WEBHOOK=http://129.114.17.83:9000/api/webhooks/58674964-50b3-4629-b452-417c08ccf874

include ../master.mk

start:
	docker run -d -p 8080:8080 -v /private/etc/lapps:/etc/lapps --name $(NAME) $(IMAGE)
	docker logs -f $(NAME)
	docker rm -f $(NAME)

less:
	lessc src/main/resources/static/css/main.less src/main/resources/static/css/main.css	

debug:
	docker run -it -p 8080:8080 -v /private/etc/lapps/askme-prod.ini:/etc/lapps/askme.ini --name $(NAME) $(IMAGE) 

update:
	echo "Not implemented."
	#curl -X POST http://129.114.17.83:9000/api/webhooks/96a05d8c-978b-40d9-9c6c-bc9856318c35

