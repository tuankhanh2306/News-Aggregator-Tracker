# ==========================================
# Stage 1: Build stage (Biên dịch mã nguồn)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 1. Copy file cấu hình maven trước để tận dụng Docker Cache layer
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# Tải trước dependencies (nếu pom.xml không đổi thì bước này được cache)
RUN ./mvnw dependency:go-offline -B

# 2. Copy source code và đóng gói file JAR (bỏ qua test vì CI đã test rồi)
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Runtime stage (Chạy ứng dụng siêu nhẹ)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user không có quyền root để tăng tính bảo mật (Best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy chỉ duy nhất file JAR từ Stage 1 sang Stage 2
COPY --from=builder /build/target/*.jar app.jar

# Mở port 8080
EXPOSE 8080

# Cấu hình tối ưu bộ nhớ cho container Java 21
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]