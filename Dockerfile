FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/media-gateway-*.jar media-gateway.jar

EXPOSE 10040

# JVM 参数最佳实践（与 magpie 保持一致）：
# -Xms / -Xmx        堆初始/最大内存（建议同值避免动态扩缩）
# -XX:+UseG1GC       使用 G1 垃圾回收器（JDK 17 默认，适合低延迟服务端）
# -XX:MaxGCPauseMillis=200  GC 暂停目标 200ms
# -XX:+UseContainerSupport 感知容器 cgroup 内存限制
# -XX:+ExitOnOutOfMemoryError OOM 直接退出，由编排系统重启
# -Dfile.encoding=UTF-8 显式指定文件编码
ENV JAVA_OPTS="-server \
  -Xms1024m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseContainerSupport \
  -XX:+ExitOnOutOfMemoryError \
  -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar media-gateway.jar"]
