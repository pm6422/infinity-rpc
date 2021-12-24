#!/bin/sh

echo "The application will start in ${SLEEP_TIME}s..." && sleep ${SLEEP_TIME}
exec mkdir -p /app/embedmongo
exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Duser.home=/app -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "${START_CLASS}" "$@"