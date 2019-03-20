FROM maven:3-jdk-8-alpine AS builder

COPY . .
RUN mvn clean install && cd parecodistribution && ./prepareBuild.sh

FROM java:8-jre-alpine
COPY --from=builder parecodistribution/target/pareco-distribution-bin .

ENTRYPOINT ["/bin/sh"]