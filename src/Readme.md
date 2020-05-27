### build

```shell script
mvn clean package
```

### running

```shell script
java -jar target/eureka-0.0.1-SNAPSHOT.jar \
    --spring.config.location=file:src/main/resources/ \
    --spring.profiles.active=dev
```