# angus-mail-demo

Sample Helidon SE project that includes simple [MailService](src/main/java/io/helidon/samples/mail/MailService.java)
utilizing [Angus Mail](https://eclipse-ee4j.github.io/angus-mail/).

## Build and run

Edit `mail.username` and `mail.password` properties in
[application.yaml](src/main/resources/application.yaml)
configuration file to match your SMTP server setup. This sample
uses GMail, if you want to use different provider, don't forget to update
SMTP server settings appropriately.

With JDK17+
```bash
mvn package
java -jar target/angus-mail-demo.jar
```

## Exercise the application
```
curl -X GET http://localhost:8080/send
{"message":"email has been sent!"}
```

```
curl -X GET http://localhost:8080/search?term=helidon
{"message":"OK","count":1,"result":[{"from":"...","subject":"Greetings from Helidon!"}]}
```

## Building a Native Image

Make sure you have GraalVM locally installed:

```
$GRAALVM_HOME/bin/native-image --version
```

Build the native image using the native image profile:

```
mvn package -Pnative-image
```

This uses the org.graalvm.buildtools:native-maven-plugin to perform the native compilation using your installed copy of GraalVM. It might take a while to complete.
Once it completes start the application using the native executable (no JVM!):

```
./target/angus-mail-demo
```

Yep, it starts fast. You can exercise the application’s endpoints as before.


## Building the Docker Image
```
docker build -t angus-mail-demo .
```

## Running the Docker Image

```
docker run --rm -p 8080:8080 angus-mail-demo:latest
```

Exercise the application as described above.
                                

## Building a Custom Runtime Image

Build the custom runtime image using the jlink image profile:

```
mvn package -Pjlink-image
```

This uses the org.graalvm.buildtools:native-maven-plugin to perform the custom image generation.
After the build completes it will report some statistics about the build including the reduction in image size.

The target/angus-mail-demo-jri directory is a self contained custom image of your application. It contains your application,
its runtime dependencies and the JDK modules it depends on. You can start your application using the provide start script:

```
./target/angus-mail-demo-jri/bin/start
```

Class Data Sharing (CDS) Archive
Also included in the custom image is a Class Data Sharing (CDS) archive that improves your application’s startup
performance and in-memory footprint. You can learn more about Class Data Sharing in the JDK documentation.

The CDS archive increases your image size to get these performance optimizations. It can be of significant size (tens of MB).
The size of the CDS archive is reported at the end of the build output.

If you’d rather have a smaller image size (with a slightly increased startup time) you can skip the creation of the CDS
archive by executing your build like this:

```
mvn package -Pjlink-image -Djlink.image.addClassDataSharingArchive=false
```

For more information on available configuration options see the org.graalvm.buildtools:native-maven-plugin documentation.
                                
