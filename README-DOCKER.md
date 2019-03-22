# Docker support

## Build

Simple docker image build without requiring for java on host machine.

    docker build -t pareco .
    
If this build is too slow for you (due to downloading dependencies), 
it is possible to do building on host machine and then package built artifacts into docker image.

    mvn clean install
    cd parecodistribution && ./prepareBuild.sh && cd -
    docker build -f Dockerfile.dev -t pareco .

## Usage

Usage is basically the same as [without](README.md#basic-usage-example) docker, 
additional concerns are port mappings and volume mounts.

## Starting server

    docker run \
        -v /my/server/host/path:/mount/server \
        -p 12345:8080 \
        pareco:latest \
        /pareco-server.sh -p 8080

## Starting client

    docker run \
        -v /my/cli/host/path:/mount/client \
        pareco:latest \
        /pareco-cli.sh -m upload \
        -s http://my.server.example:12345 \
        -l /mount/client -r /mount/server \
        --forceColors



