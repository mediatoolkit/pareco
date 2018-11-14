# Pareco [![Build Status](https://travis-ci.org/mediatoolkit/pareco.svg?branch=master)](https://travis-ci.org/mediatoolkit/pareco)

Pareco is utility for remote data synchronization (**pa**rallel **re**mote **co**py).

## Description

Main goal of Pareco is to synchronize (copy) data between 2 directories. 
It has semantics of basic directory copy (similar to [rsync](https://en.wikipedia.org/wiki/Rsync)) where transfer of whole files
and portions of files can be skipped if data is already present on destination. 

Main benefits of Pareco are:
 - parallelism/concurrency to speed transfer up
 - skipping of unnecessary transfers
 - synchronization capabilities (deletion of deleted files)
 - informal logging

## Use cases

* simple directory copy from local to remote directory (upload) or from remote to local directory (download)
* restoring database snapshot from backup
* migrating database from one server to another with minimized downtime even if link throughput is low 
_(see [Tutorial example](#tutorial-example) below)_

## Features

* upload from client to server
* download from server to client
* make use of multiple [parallel/concurrent](#connections-parallelism) transfer connections
* skipping transfer of already equal files
* skipping transfer of parts ([chunks](#file-chunks)) of files that are same (torrent-like)
* [authentication](#authentication) using access token
* tune-able verbose stats and logging
* sync of file metadata (last modified time, posix file permissions)
* optional deletion of unexpected files
* glob pattern matching for [inclusion](#inclusion-exclusion-of-files-and-directories) and/or 
[exclusion](#inclusion-exclusion-of-files-and-directories) of sub-directories and files
* variety of available digest hash functions and option to disable digest

## Build

Pareco uses maven, use

    mvn clean compile package

to build server and client.

## Basic usage example

Both client and server offer help option to list available options: 

    ./pareco-server.sh -h
    ./pareco-cli.sh -h

First, start the server: 

    ./pareco-server --port 8080

after server has successfully started, initiate transfer with client: 

    ./pareco-cli.sh --mode upload \
        --localDir my-local-directory \
        --remoteDir my-remote-directory \
        --server http://my.server.com:8080
  
client will execute upload of all files and directories within its 
`my-local-directory` to server into its local directory `my-remote-directory`.

## Server

Properties:
 - http REST service application
 - maintains different sessions for different transfer
 - expires sessions after expired inactivity
 - once started, server can be used many times for different transfer sessions

## Client

Properties:
 - fully controls both upload and download transfer
 - used for single transfer session

## Options

#### Mode

Pareco supports upload and download transfer.

#### Local and remote directory

Source and destination directories can be specified either absolute or relative.
 - If relative path is specified then it is resolved relative to current user 
 directory where is server or client is started
 - If absolute path is specified then it is resolved absolute to file system
 on machine where server or client is started
 
#### Server
 
Server option has form `http[s]://host[:port]`.
While client supports **https** and server does not yet, **https** can still be used if
server is placed behind a proxy, for example **nginx** or **HAProxy**.
Port is optional, if not specified then default value is `80` for **http**,
`443` for **https**.
  
#### Authentication
 
Authentication is optional and disabled by default. 
It can be used by starting both client and server with manually provided 
access token using option `-a my-token` to provide server side check 
if client is allowed to perform transfer.
 
Server can be started with option `-g` to automatically generate and 
print access token to be used by client.
 
#### File chunks
 
File is virtually split into chunks with size which can optionally be specified using option `-c`.

The smaller chunk size is then there is better chance that more chunks in file will be skipped,
but there will be more overhead in chunk metadata exchange. 

On contrast, the bigger chunk size then there is less overhead due to metadata exchange, 
but then there is less chance that some file chunk can be skipped due to the fact even 
single difference in chunk contents will likely cause that hash digests won't match and chunk
will need to be transferred.

#### Connections-parallelism

Number of concurrent transfer connections can be set using `-n` option.

For small files it means how many files can be processed/transferred concurrently.

For large files it means how many chunks are transferred concurrently.

Small files are handled concurrently while large files are handled one by one.
File is classified as small if number of chunks is less than number of transfer 
connections, large otherwise.

#### Deletion of unexpected files

Automatic deletion is disabled by default. It can be enabled using option `-del`.

When performing transfer from source directory into destination directory,
file/directory is unexpected in case when destination directory contains file/directory 
which is not present in source directory.

#### Hashing-digest

Transfer of file/chunk can be skipped if source's and destination's file/chunk digests 
match each other.

Hashing algorithm can be selected using option `--hash`. 
Pareco uses Guava's implementations of popular hashing algorithms.

Each hash function has different properties, best suitable functions for Pareco's file/chunk
integrity checks is some fast non-cryptographic function such as: MURMUR, CRC, ADLER, ...

Calculation of digest can be disabled using `--skipDigest` option. Then, file integrity is 
checked only using file size and last modification time.

#### Inclusion-exclusion of files and directories

Contents of directory can be filtered using `--include` and/or `--exclude` options.
Both inclusion and exclusion options accept glob file path pattern. 

Pattern is applied to relative path of each file/directory in respect to 
source or destination directory.

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

Normal migration without pareco would be done in following steps:
 - stop the database
 - copy all of its data
 - start the database on new machine
 
Problem with this approach is that coping of data can be long running operation and thus
database downtime is also long duration.

Using Pareco, migration downtime can be minimized using following steps:
 - start pareco server on machine where database currently runs on
 - keep database still running even if it performs write operations
 - on target machine initiate download transfer
 - copied database is now transferred but very likely in dirty/corrupted state
 - stop the database
 - initiate download transfer once again, this time transfer will be much faster
 since only changes in files need to be re-transferred
 - start the database on new machine
 
Note: you probably want to use option `--deleteUnexpected` to remove any database files 
which are deleted since first download transfer.

 
## Requirements
 
Pareco requires java 8 runtime.

## Licence

MIT License

Copyright (c) 2018 Pareco, Mediatoolkit

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
