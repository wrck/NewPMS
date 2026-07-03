# ============================================================================
# vibe-server 后端镜像 - 多阶段构建
# 阶段1：Maven 编译打包
# 阶段2：JRE 17 运行
# ============================================================================

# ---------------------------------------------------------------------------
# 阶段1：使用 Maven 镜像编译打包
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom 文件，利用 Docker 层缓存加速依赖下载
COPY pom.xml ./
COPY module-common/pom.xml ./module-common/
COPY module-auth/pom.xml ./module-auth/
COPY module-project/pom.xml ./module-project/
COPY module-device/pom.xml ./module-device/
COPY module-resource/pom.xml ./module-resource/
COPY module-delivery/pom.xml ./module-delivery/
COPY module-agent/pom.xml ./module-agent/
COPY module-acceptance/pom.xml ./module-acceptance/
COPY module-finance/pom.xml ./module-finance/
COPY module-collaboration/pom.xml ./module-collaboration/
COPY module-integration/pom.xml ./module-integration/
COPY module-report/pom.xml ./module-report/
COPY module-system/pom.xml ./module-system/
COPY vibe-server-bootstrap/pom.xml ./vibe-server-bootstrap/

# 预下载依赖（提速后续构建）
RUN mvn -B dependency:go-offline -Dmaven.test.skip=true || true

# 复制全部源码
COPY . .

# 编译打包，跳过测试
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# 阶段2：使用 JRE 镜像运行
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

LABEL maintainer="vibe-team"
LABEL description="网络设备原厂实施项目管理系统 - 后端服务"

WORKDIR /app

# 时区设置
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制构建产物
# vibe-server-bootstrap 为启动模块，pom.xml 中 <finalName>vibe-server</finalName>
# 故 spring-boot-maven-plugin repackage 产出可执行 fat jar：vibe-server.jar
COPY --from=builder /build/vibe-server-bootstrap/target/vibe-server.jar /app/app.jar

# 暴露服务端口
EXPOSE 8080

# JVM 参数（容器内推荐使用 -XX:MaxRAMPercentage 限制堆内存）
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
