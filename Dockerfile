# 多階段構建
FROM openjdk:21-jdk-slim AS builder

# 安裝必要的工具
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 設置工作目錄
WORKDIR /app

# 複製 Maven wrapper 和 pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# 複製源代碼
COPY src/ src/

# 構建應用程式
RUN ./mvnw clean package -DskipTests

# 運行階段
FROM openjdk:21-jdk-slim

# 安裝 Docker CLI 和必要的工具用於 Docker-in-Docker
RUN apt-get update && \
    apt-get install -y \
    curl \
    ca-certificates \
    gnupg \
    lsb-release && \
    # 安裝 Docker CLI
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    rm -rf /var/lib/apt/lists/*

# 設置工作目錄
WORKDIR /app

# 複製構建好的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8888

# 設置啟動命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"]