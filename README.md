# Pareco [![Build Status](https://travis-ci.org/mediatoolkit/pareco.svg?branch=master)](https://travis-ci.org/mediatoolkit/pareco)

Pareco is an utility for remote data synchronization (**pa**rallel **re**mote **co**py).

## Description

The main goal of Pareco is to synchronize (copy) data between 2 directories. 
It has the semantics of basic directory copy (similar to [rsync](https://en.wikipedia.org/wiki/Rsync)) where transfer of whole files
and portions of files can be skipped if data is already present on destination. 

Main benefits of Pareco are:
 - parallelism/concurrency to speed transfer up
 - skipping of unnecessary transfers
 - synchronization capabilities (deletion of deleted files)
 - informal logging

## Use cases

* simple directory copy from local to the remote directory (upload) or from remote to local directory (download)
* restoring database snapshot from backup
* migrating database from one server to another with minimized downtime even if link throughput is low 
_(see [Tutorial example](#tutorial-example) below)_

## Features

* upload from client to server
* download from server to client
* make use of multiple [parallel/concurrent](#connections-parallelism) transfer connections
* skipping transfer of already equal files
* skipping transfer of parts ([chunks](#file-chunks)) of files that are the same (torrent-like)
* [authentication](#authentication) using an access token
* tunable verbose stats and logging
* sync of file metadata (last modified time, posix file permissions)
* optional deletion of unexpected files
* glob pattern matching for [inclusion](#inclusion-exclusion-of-files-and-directories) and/or 
[exclusion](#inclusion-exclusion-of-files-and-directories) of sub-directories and files
* variety of available digest hash functions and the option to disable digest

## When (not) to use Pareco

* use it if you expect gain in speed by using multiple connections
over single connection
* use it if you expect that sync will be able to skip some portion
of the data
* don't use it if the throughput between the server or client to the 
file system is lower than the throughput between client and the server
_(i.e. running both server and the client on the same host machine and copying
the data between the local file system and remote disk which is mounted 
locally using NFS)_

## Build

Pareco uses maven, use

    mvn clean compile package

to build server and client.

After the build completes, both client and server will be packaged in

    parecodistribution/target/pareco-distribution-{version}.zip
    
and, more conveniently, in uncompressed directory

    parecodistribution/target/pareco-distribution-{version}/

## Docker

see documentation [here](README-DOCKER.md)

## Basic usage example

Both, client and server offer help option to list available options: 

    ./pareco-server.sh -h
    ./pareco-cli.sh -h

First, start the server: 

    ./pareco-server --port 8080

after server has successfully started, initiate transfer with client: 

    ./pareco-cli.sh --mode upload \
        --localDir my-local-directory \
        --remoteDir my-remote-directory \
        --server http://my.server.com:8080

or, instead of command line client, start a basic web UI client wrapper:

    ./pareco-cli-runner.sh --port 8080
    
and open `http://localhost:8080` in browser

client will execute upload of all files and directories within its 
`my-local-directory` to server into its local directory `my-remote-directory`.

## Server

Properties:
 - http REST service application
 - maintains different sessions for different transfer
 - expires sessions after expired inactivity
 - once started, a server can be used many times for different transfer sessions

## Client

Properties:
 - fully controls both upload and download transfer
 - used for single transfer session
 
## Client runner

Client runner is wrapper around command line client with simple web UI. 
It can be used instead raw client.

## Options

#### Mode

Pareco supports upload and download transfer.

#### Local and remote directory

Source and destination directories can be specified either absolute or relative.
 - If a relative path is specified then it is resolved relative to the current user 
 directory where is server or client is started
 - If an absolute path is specified then it is resolved absolute to file system
 on the machine where server or client is started
 
#### Server
 
Server option has form `http[s]://host[:port]`.
While client supports **https** and server does not yet, **https** can still be used if 
a server is placed behind a proxy, for example, **nginx** or **HAProxy**.
Port is optional, if not specified, then the default value is `80` for **http**,
`443` for **https**.
  
#### Authentication
 
Authentication is optional and disabled by default. 
It can be used by starting both client and server with manually provided 
access token using option `-a my-token` to provide server-side check 
if the client is allowed to perform a transfer.
 
The server can be started with option `-g` to automatically generate and 
print access token to be used by a client.
 
#### File chunks
 
A file is virtually split into chunks with a size which can optionally be specified using option `-c`.

The smaller chunk size is, there is a better chance that more chunks in a file will be skipped,
but there will be more overhead in chunk metadata exchange. 

In contrast, the bigger chunk size then there is less overhead due to metadata 
exchange. In that case, there is less of a chance that some file chunks will 
be skipped as even a single difference in chunk contents will likely cause hash 
digests not to match and a chunk will need to be transferred.

#### Connections-parallelism

A number of concurrent transfer connections can be set using `-n` option.

For small files, it means how many files can be processed/transferred concurrently.

For large files, it means how many chunks are transferred concurrently.

Small files are handled concurrently while large files are handled one by one.
A file is classified as small if the number of chunks is less than the number of transfer 
connections, large otherwise.

#### Deletion of unexpected files

Automatic deletion is disabled by default. It can be enabled using option `-del` or 
`--deleteUnexpected`.

**Warning**: Use it with caution, double check not to mistake and specify wrong directories.

When performing a transfer from source directory into destination directory,
file/directory is unexpected in the case when destination directory contains file/directory 
which is not present in the source directory.

#### Hashing-digest

Transfer of a file/chunk can be skipped if source's and destination's file/chunk digests 
match each other.

Hashing algorithm can be selected using option `--hash`. 
Pareco uses Guava's implementations of popular hashing algorithms.

Each hash function has different properties, the best suitable functions for Pareco's file/chunk
integrity checks is some fast non-cryptographic function such as: MURMUR, CRC, ADLER, ...

Calculation of digest can be disabled using `--skipDigest` option. Then, file integrity is 
checked only using file size and last modification time.

#### Inclusion-exclusion of files and directories

Contents of a directory can be filtered using `--include` and/or `--exclude` options.
Both, inclusion and exclusion options accept glob file path pattern. 

Pattern is applied to the relative path of each file/directory in respect to 
the source or destination directory.

Example:

Given following directory structure:

    my-dir/
        file1.txt
        A-dir/
            file2.txt
            file3.zip
        B-dir/
            file4.txt

 - `*.txt` will match only `file1.txt` 
 - `*/*.txt` will match `file2.txt` and `file4.txt`
 - `**.txt` will match `file1.txt`, `file2.txt` and `file4.txt`
 - `A*` will match dir `A-dir` and all of its contents `file2.txt` and `file3.zip`

## Tutorial example

Pareco can be used for migrating a database from one machine to another.

Normal migration without pareco would be done in the following steps:
 - stop the database
 - copy all of its data
 - start the database on the  new machine
 
Problem with this approach is that the copying of data can be long running operation and thus
database downtime is also long.

Using Pareco, migration downtime can be minimized using the following steps:
 - start pareco server on a machine where database currently runs on
 - keep the database still running even if it performs write operations
 - on target machine initiate download transfer
 - the copied database is now transferred but very likely in a dirty/corrupted state
 - stop the database
 - initiate download transfer once again, this time transfer will be much faster
 since only changes in files need to be re-transferred
 - start the database on the new machine
 
Note: you probably want to use option `--deleteUnexpected` to remove any database files 
which are deleted since first download transfer.

 
## Requirements
 
Pareco requires java 8 runtime, or Docker engine

## Licence

MIT Licence

