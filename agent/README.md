# AI Agent 对话系统

一个基于 Spring Boot + React 的 AI 对话系统，支持多会话管理和多轮对话。

## 项目结构

```
agent/
├── agent-backend/          # Spring Boot 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agentbackend/
│   │   │   │   ├── config/         # 配置类
│   │   │   │   ├── controller/     # 控制器层
│   │   │   │   ├── dto/            # 数据传输对象
│   │   │   │   ├── entity/         # 实体类
│   │   │   │   ├── mapper/         # MyBatis Mapper
│   │   │   │   └── service/        # 服务层
│   │   │   └── resources/
│   │   │       ├── application.yml # 配置文件
│   │   │       └── schema.sql      # 数据库初始化脚本
│   │   └── test/
│   └── pom.xml
└── agent-frontend/         # React 前端
    ├── public/
    ├── src/
    │   ├── api/           # API 接口
    │   ├── components/    # 组件
    │   ├── App.js         # 主应用
    │   └── index.js       # 入口文件
    └── package.json
```

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.5
- Spring AI 1.0.0-M4
- MyBatis-Plus 3.5.5
- MySQL 8.0+

### 前端
- React 18
- Axios
- 原生 CSS

## 快速开始

### 1. 环境准备

确保已安装以下环境：
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+

### 2. 数据库配置

1. 创建数据库并执行初始化脚本：

```bash
mysql -u root -p < agent-backend/src/main/resources/schema.sql
```

或者在 MySQL 客户端中执行：

```sql
CREATE DATABASE IF NOT EXISTS agent_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行 `agent-backend/src/main/resources/schema.sql` 中的 SQL 语句。

2. 修改数据库配置：

编辑 `agent-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agent_ai?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password  # 修改为你的数据库密码
```

### 3. 配置 AI API

编辑 `agent-backend/src/main/resources/application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: your_openai_api_key  # 修改为你的 OpenAI API Key
      base-url: https://api.openai.com  # 或使用其他兼容的 API 地址
```

**注意：** 如果你使用的是其他 OpenAI 兼容的 API（如国内的中转服务），请修改 `base-url`。

### 4. 启动后端

```bash
cd agent-backend
mvn clean install
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动。

### 5. 启动前端

```bash
cd agent-frontend
npm install
npm start
```

前端将在 `http://localhost:3000` 启动。

### 6. 访问应用

打开浏览器访问：`http://localhost:3000`

## 功能特性

### ✅ 已实现功能

1. **会话管理**
   - 创建新会话
   - 查询会话列表
   - 会话自动排序（按更新时间）

2. **AI 对话**
   - 多轮对话（上下文记忆）
   - 消息持久化
   - 历史记录查询

3. **用户界面**
   - 左侧会话列表
   - 右侧聊天窗口
   - 消息滚动到底部
   - Loading 状态显示

## API 接口文档

### 1. 创建会话

```
POST /api/conversation/create
```

请求体：
```json
{
  "conversationId": "uuid-string",
  "userId": 1
}
```

### 2. 查询会话列表

```
GET /api/conversation/list?userId=1
```

### 3. 发送消息

```
POST /api/chat/send
```

请求体：
```json
{
  "conversationId": "uuid-string",
  "userId": 1,
  "message": "你好"
}
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reply": "AI 回复内容"
  }
}
```

### 4. 获取历史记录

```
GET /api/chat/history?conversationId=uuid-string
```

## 生产部署

### 前端打包

```bash
cd agent-frontend
npm run build
```

将生成的 `build` 目录内容复制到 `agent-backend/src/main/resources/static/` 目录下。

### 后端打包

```bash
cd agent-backend
mvn clean package
```

生成的 JAR 文件在 `target` 目录下。

### 运行

```bash
java -jar agent-backend-0.0.1-SNAPSHOT.jar
```

访问：`http://localhost:8080`

## 注意事项

1. **API Key 安全**：请勿将 API Key 提交到版本控制系统
2. **数据库密码**：生产环境请使用强密码
3. **CORS 配置**：生产环境请修改 `WebConfig.java` 中的 CORS 配置
4. **用户系统**：当前为简化版本，userId 写死为 1，生产环境需要实现完整的用户认证系统

## 常见问题

### 1. 数据库连接失败

检查 MySQL 是否启动，用户名密码是否正确，数据库是否已创建。

### 2. AI 调用失败

检查 API Key 是否正确，网络是否通畅，API 地址是否可访问。

### 3. 前端无法访问后端

检查后端是否启动，端口是否正确，CORS 配置是否正确。

## 开发计划

- [ ] 用户认证系统
- [ ] 会话删除功能
- [ ] 消息编辑功能
- [ ] 流式输出
- [ ] 多模型支持
- [ ] 导出对话记录

## 许可证

MIT License
