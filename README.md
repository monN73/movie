# 电影信息平台

一个基于Spring Boot的电影信息管理与分享平台，提供电影排行榜、详情查询、评论系统和用户影单收藏等功能。

## 技术栈

- **后端框架**: Spring Boot 3.4.4
- **数据库**: MySQL
- **缓存**: Redis
- **搜索引擎**: Elasticsearch 8.x
- **消息队列**: RabbitMQ
- **认证方式**: JWT (JSON Web Tokens)
- **容器化**: Docker

## 核心功能

- 电影排行榜 (每日/每周热门)
- 电影详情信息查询
- 电影搜索功能
- 用户管理 (注册、登录)
- 用户个性化设置 (头像、背景图片上传)
- 电影收藏列表
- 电影评论系统

## 数据流

1. 从TMDB API获取电影排行榜数据
2. 使用Redis缓存热门电影和排行榜
3. 电影评论通过RabbitMQ处理并持久化到MySQL
4. 搜索功能通过Elasticsearch实现

## 环境要求

- JDK 21
- Maven 3.x
- MySQL 8.x
- Redis 6.x
- Elasticsearch 8.x
- RabbitMQ 3.x

## 本地运行

1. 克隆仓库
   ```bash
   git clone https://github.com/monN73/movie.git
   cd movie-java
   ```

2. 配置环境变量或application.properties
   ```
   spring.datasource.url=jdbc:mysql://localhost:3306/movie_db
   spring.datasource.username=root
   spring.datasource.password=yourpassword
   
   spring.redis.host=localhost
   spring.redis.port=6379
   
   elasticsearch.uris=http://localhost:9200
   elasticsearch.username=elastic
   elasticsearch.password=yourpassword
   
   spring.rabbitmq.host=localhost
   spring.rabbitmq.port=5672
   spring.rabbitmq.username=guest
   spring.rabbitmq.password=guest
   
   tmdb.api.token=your_tmdb_api_token
   omdb.apikey=your_omdb_api_key
   ```

3. 使用Maven打包
   ```bash
   ./mvnw clean package
   ```

4. 运行应用
   ```bash
   java -jar target/movie-0.0.1-SNAPSHOT.jar
   ```

## Docker部署

1. 构建Docker镜像
   ```bash
   docker build -t movie-app .
   ```

2. 运行容器
   ```bash
   docker run -d -p 8080:8080 --name movie-app movie-app
   ```

## API说明

### 认证相关
- `POST /auth/login` - 用户登录

### 用户相关
- `POST /users/register` - 用户注册
- `GET /users/get` - 获取用户信息
- `POST /users/upload/avatar` - 上传用户头像

### 电影相关
- `GET /movies/trending/{when}/{page}` - 获取热门电影(day/week)
- `POST /movies/more/{uid}` - 获取电影详细信息
- `POST /movies/fetch-all` - 拉取所有热门电影(管理员)

### 搜索相关
- `GET /search/keyword` - 关键字搜索电影

### 收藏相关
- `POST /movieList/add` - 添加电影到收藏
- `DELETE /movieList/delete` - 从收藏中删除电影
- `GET /movieList/getList?{userId}` - 获取用户收藏列表

### 评论相关
- `POST /comments/add` - 添加电影评论
- `DELETE /comments/delete` - 删除评论
- `GET /comments/movie/{movieId}` - 获取电影评论
- `GET /comments/user/{userId}` - 获取用户评论

