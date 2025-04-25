# Sử dụng hình ảnh nhỏ gọn với Gradle và JDK
FROM gradle:8.1.0-jdk17-alpine AS builder

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy toàn bộ mã nguồn vào container
COPY . .

# Build dự án bằng Gradle
RUN gradle build

# Tạo hình ảnh runtime nhỏ gọn chỉ chứa mã đã build
FROM openjdk:17-jdk-alpine

WORKDIR /app

# Copy mã đã build từ giai đoạn trước
COPY --from=builder /app/build/libs/tcp-gateway.jar .

# Thiết lập biến môi trường
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Chạy ứng dụng
CMD ["java", "-jar", "tcp-gateway.jar"]
