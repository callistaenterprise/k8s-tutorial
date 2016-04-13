FROM ofayau/ejre:8-jre
VOLUME /tmp
ADD @project.build.finalName@.jar app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]