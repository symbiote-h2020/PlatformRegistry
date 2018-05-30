FROM java:8

WORKDIR /

COPY ./build/libs/PlatformRegistry-1.2.0.jar platformregistry.jar
COPY ./cmdscript.sh shell.sh

RUN chmod 777 shell.sh

EXPOSE 8080

CMD ./shell.sh
