FROM openjdk:21-jdk-slim

# 把本地 jar 包复制到镜像里
COPY target/movie-0.0.1-SNAPSHOT.jar /app/myapp.jar

# 进入容器后默认工作目录
WORKDIR /app

# 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "myapp.jar"]

# 应用监听的端口（比如 Spring Boot 默认8080）
EXPOSE 8080