# Marathon Dockerfile
FROM ubuntu:14.04
MAINTAINER Mesosphere <support@mesosphere.io>

## PACKAGES ##
RUN echo "deb http://repos.mesosphere.io/ubuntu/ trusty main" > /etc/apt/sources.list.d/mesosphere.list
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
RUN apt-get update
RUN apt-get install --assume-yes mesos marathon python-software-properties curl default-jdk

EXPOSE 8080
WORKDIR /usr/local/bin/
CMD ["--help"]
ENTRYPOINT ["java -cp /usr/local/bin/marathon mesosphere.Marathon.Main"]