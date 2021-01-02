FROM oracle/graalvm-ce:20.3.0-java11 as graalvm
RUN gu install native-image

COPY . /home/app/featherweight
WORKDIR /home/app/featherweight

RUN native-image --no-fallback --enable-url-protocols=https --no-server -cp build/libs/featherweight.jar -jar build/libs/featherweight.jar

FROM frolvlad/alpine-glibc
RUN apk update && apk add libstdc++
EXPOSE 8080
COPY --from=graalvm /home/app/featherweight/featherweight /app/featherweight
ENTRYPOINT ["/app/featherweight"]