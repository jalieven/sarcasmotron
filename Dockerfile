FROM ubuntu:14.04
MAINTAINER Jan Lievens <jan@milieuinfo.be>
RUN apt-get update
RUN apt-get install redis-server
RUN apt-get install vim
RUN apt-get install wget