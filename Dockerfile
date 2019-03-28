FROM maven:3-jdk-8-alpine AS builder

COPY . .
RUN mvn clean install && cd pareco-distribution && ./prepareBuild.sh

FROM java:8-jre-alpine
COPY --from=builder pareco-distribution/target/pareco-distribution-bin .

ENTRYPOINT ["/bin/sh"]