FROM findepi/graalvm:native as builder
WORKDIR /builder
ADD ./build/featherweight.jar /builder/featherweight.jar

RUN native-image \
     -H:+ReportUnsupportedElementsAtRuntime \
     -H:EnableURLProtocols=http \
     -J-Xms3G -J-Xmx4G --no-server \
     -jar featherweight.jar

FROM debian:stable-slim
EXPOSE 8080
COPY --from=builder /builder/featherweight /featherweight

CMD ["./featherweight"]