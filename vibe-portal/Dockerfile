# ============================================================================
# vibe-portal 前端镜像 - 多阶段构建
# 阶段1：Node 20 编译打包
# 阶段2：Nginx 1.25 提供静态资源服务
# ============================================================================

# ---------------------------------------------------------------------------
# 阶段1：使用 Node 镜像编译前端
# ---------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /build

# 先复制 package.json，利用 Docker 层缓存加速依赖安装
COPY package.json package-lock.json* ./

# 安装依赖
RUN npm install --no-audit --no-fund

# 复制全部源码
COPY . .

# 构建生产环境产物
RUN npm run build

# ---------------------------------------------------------------------------
# 阶段2：使用 Nginx 镜像提供静态资源
# ---------------------------------------------------------------------------
FROM nginx:1.25-alpine

LABEL maintainer="vibe-team"
LABEL description="网络设备原厂实施项目管理系统 - 外部门户 H5（客户/代理商）"

# 时区设置
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制构建产物到 Nginx 静态资源目录
COPY --from=builder /build/dist /usr/share/nginx/html

# 自定义 Nginx 配置由 docker-compose 挂载（/etc/nginx/conf.d/default.conf）
RUN rm -f /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 使用 Nginx 默认启动命令
CMD ["nginx", "-g", "daemon off;"]
