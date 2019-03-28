#fast image creation which avoids downloading all maven dependencies each time
#steps which are neded to execute before building an image:
#    mvn clean install
#    cd pareco-distribution
#    ./prepareBuild.sh
#    cd -
FROM java:8-jre-alpine
COPY pareco-distribution/target/pareco-distribution-bin .
ENTRYPOINT ["/bin/sh"]