# Libre Spider - 小红书采集工具

本是一个基于 Java Spring Boot 和 Playwright 实现的小红书（XHS）数据采集工具。它通过模拟浏览器行为和处理 API 签名，旨在提供一个稳定、高效的小红书内容获取解决方案。

## ✨ 主要功能

- **关键词搜索**：按关键词搜索相关笔记。
- **笔记详情获取**：根据笔记 ID 获取完整的笔记内容，包括图片、视频和评论等。
- **用户信息查询**：获取指定用户的公开信息和笔记列表。
- **API 签名处理**：内置了小红书 API 所需的 `x-s` 和 `x-t` 等签名参数的生成逻辑。
- **登录状态检查**：可以验证配置的 Cookie 是否有效。
- **RESTful API**：提供了一套简洁的 API 接口，方便与其他系统集成。
- **API 文档**：通过 SpringDoc 自动生成交互式 API 文档（Swagger UI）。

## 🛠️ 技术栈

- **核心框架**: Spring Boot 3
- **编程语言**: Java 17
- **浏览器自动化**: Microsoft Playwright
- **HTTP 客户端**: OkHttp
- **数据库**: MyBatis-Plus, H2 (默认), PostgreSQL (支持)
- **API 文档**: SpringDoc (Swagger UI)
- **构建工具**: Apache Maven

## 🚀 快速开始

### 1. 环境准备

- **Java Development Kit (JDK)**: 版本 17 或更高。
- **Maven**: 版本 3.6 或更高。
- **Node.js**: Playwright 需要 Node.js 环境来安装浏览器驱动。

### 2. 克隆项目

```bash
git clone https://github.com/your-username/libre-spider.git
cd libre-spider
```

### 3. 配置

主要的配置文件位于 `src/main/resources/application.yml`。核心配置是小红书的 Cookie。

1.  **获取 Cookie**:
    - 在电脑浏览器中登录小红书（[www.xiaohongshu.com](https://www.xiaohongshu.com)）。
    - 打开开发者工具（按 F12），切换到“网络”(Network) 标签页。
    - 刷新页面，找到任意一个对 `edith.xiaohongshu.com` 的请求。
    - 在请求头 (Request Headers) 中找到 `cookie` 字段，并复制其完整的字符串值。

2.  **更新配置**:
    打开 `application.yml` 文件，找到 `xhs.cookie` 配置项，并粘贴你复制的 Cookie。

    ```yaml
    xhs:
      cookie: "你的Cookie粘贴在这里"
      # 其他配置...
    ```

### 4. 安装 Playwright 浏览器

Playwright 需要下载相应的浏览器文件。在项目根目录下运行以下命令：

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

���者，如果你的环境允许，也可以使用 `npx`:

```bash
npx playwright install
```

### 5. 运行项目

使用 Maven 启动 Spring Boot 应用：

```bash
mvn spring-boot:run
```

当看到类似 `Started LibreSpiderApplication in X.XXX seconds` 的日志时，表示应用已成功启动。

## 📖 API 使用说明

应用启动后，可以通过 API 来访问数据。

### 访问 API 文档

为了方便调试和查看所有可用的 API，本项目集成了 Swagger UI。
请访问：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### API 端点示例

#### 1. 搜索笔记

- **URL**: `GET /api/xhs/search`
- **参数**:
  - `keyword` (必需): 搜索的关键词。
  - `page` (可选, 默认 1): 页码。
  - `sortType` (可选, 默认 `GENERAL`): 排序方式。可选值: `GENERAL` (综合), `POPULARITY_DESCENDING` (热门), `CREATE_TIME_DESCENDING` (最新)。
- **示例**:
  ```bash
  curl -X GET "http://localhost:8080/api/xhs/search?keyword=Java"
  ```

#### 2. 获取笔记详情

- **URL**: `GET /api/xhs/note/{noteId}`
- **路径参数**:
  - `noteId` (必需): 笔记的 ID。
- **示例**:
  ```bash
  curl -X GET "http://localhost:8080/api/xhs/note/64c3e3a3000000002702b6d7"
  ```

## 📄 免责声明

- 本项目仅用于学习和技术��究，严禁用于任何商业或非法用途。
- 使用本项目产生的一切后果由使用者自行承担。
- 请尊重小红书的版权和用户隐私，合理使用数据。
- 如果项目侵犯了您的权益，请联系作者删除。
