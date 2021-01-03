FROM oracle/graalvm-ce:20.3.0-java11 as graalvm
RUN gu install native-image

COPY . /home/app/hitchhikersguide
WORKDIR /home/app/hitchhikersguide

RUN native-image --no-fallback --enable-url-protocols=https --no-server -cp build/libs/hitchhikersguide.jar -jar build/libs/hitchhikersguide.jar

FROM frolvlad/alpine-glibc
RUN apk update && apk add libstdc++
EXPOSE 8080
COPY --from=graalvm /home/app/hitchhikersguide/hitchhikersguide /app/hitchhikersguide
ENTRYPOINT ["/app/hitchhikersguide"]