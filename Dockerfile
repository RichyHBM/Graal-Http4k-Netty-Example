FROM findepi/graalvm:native as builder
# build our application
WORKDIR /builder
ADD ./build/libs/server-all.jar /builder/server.jar
RUN native-image \
    --static \
    -H:IncludeResources="(.*.conf)|(static/.*)|(META-INF/mime.types)" \
    -jar server.jar
#####
# The actual image to run
#####
FROM openjdk:8-jre-alpine3.7
WORKDIR /app
EXPOSE 8080
COPY --from=builder /builder/server .
COPY --from=builder /builder/server.jar .
# Simple performance tests
RUN echo; \
echo Size comparison; \
ls -lh; \
echo; \
/usr/bin/time -f "Native maxRSS %MkB, real %e, user %U, sys %S" ./server --skip-logs; \
/usr/bin/time -f "Jar maxRSS %MkB, real %e, user %U, sys %S" java -jar server.jar --skip-logs
