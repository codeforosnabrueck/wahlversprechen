FROM ubuntu:18.04
RUN  apt-get update \
  && apt-get install -y wget \
  && apt-get install -y unzip \
  && apt-get install -y openjdk-8-jdk \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /src
RUN wget https://downloads.typesafe.com/play/2.2.6/play-2.2.6.zip
RUN unzip play-2.2.6.zip
ADD . /src
CMD  play-2.2.6/play
