# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

始终用中文回答

## 项目概述
这是一个基于 Spring Boot 的小红书爬虫系统，使用 Playwright 模拟浏览器行为，实现了与 MediaCrawler Python 项目相同的功能。

## 常用命令

### 构建和运行
```bash
# 编译项目
mvn clean compile

# 运行单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=XhsSignatureHelperTest

# 启动应用（开发模式，使用H2数据库）
mvn spring-boot:run -Dspring.profiles.active=dev

# 启动应用（默认模式）
mvn spring-boot:run

# 打包
mvn clean package

# 运行打包后的jar
java -jar target/libre-spider-1.0.0.jar
```

### 开发调试
```bash
# 跳过测试打包
mvn clean package -DskipTests

# 查看依赖树
mvn dependency:tree

# 更新依赖版本
mvn versions:display-dependency-updates

# 安装 Playwright 浏览器（首次运行前必须执行）
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## 核心架构

### 服务分层架构
1. **XhsCrawlerService** - 爬虫主服务，协调各个组件
   - 在 @PostConstruct 中初始化 Playwright 和各个服务
   - 管理页面生命周期
   - 提供搜索、获取笔记详情、获取用户信息等核心功能

2. **XhsController** - REST API 控制器
   - `/api/xhs/search` - 搜索笔记
   - `/api/xhs/note/{noteId}` - 获取笔记详情
   - `/api/xhs/user/{userId}` - 获取用户信息
   - `/api/xhs/login/status` - 检查登录状态

### 签名算法实现（最关键）
- `XhsSignatureHelper` - 实现小红书的签名算法，包含 mrc、b64Encode、sign 等核心方法
- 签名生成流程：获取 a1(cookie) + b1(localStorage) + X-s/X-t(JS加密) → 生成 x-s-common
- **注意事项**：
  - mrc 数组必须有 256 个元素（标准 CRC 表长度）
  - 请求头大小写必须严格匹配：X-S, X-T, x-S-Common, X-B3-Traceid
  - 时间戳需要乘以 1000

### 浏览器自动化架构
1. `PlaywrightService` - 管理 Playwright 浏览器实例生命周期
   - 使用 `com.microsoft.playwright.options.Cookie` 和 `LoadState` 类
   - 在 @PostConstruct 中启动浏览器，@PreDestroy 中清理资源
2. `JavaScriptExecutor` - 执行 JS 代码获取加密参数（window._webmsxyw）
   - 注入 stealth.min.js 反检测脚本
   - 生成搜索ID
3. `CookieService` - 管理 Cookie 存储和更新
4. `XhsApiClient` - 发送 HTTP 请求，处理签名和请求头

### API 请求流程
1. `XhsApiClient` 构建请求 → 调用 JS 获取加密参数
2. 使用 `XhsSignatureHelper` 生成签名
3. 添加所有必需的请求头（X-S, X-T, x-S-Common, X-B3-Traceid）
4. 发送 HTTP 请求（使用 OkHttp）

### 数据获取策略
- 笔记详情：优先 HTML 解析（信息更全） → 降级到 API 调用
- 用户信息：解析 HTML 中的 window.__INITIAL_STATE__
- 搜索功能：调用 API 并使用 searchId 参数

## 配置说明

### 应用配置（application.yml）
实际使用的配置：
- `xhs.cookies` - Cookie 字符串（必须包含 web_session）
- `xhs.apiDomain` - API 请求域名
- `xhs.webDomain` - 网站域名
- `xhs.enableCdpMode` - 是否启用 Chrome DevTools Protocol

### Cookie 配置步骤
1. 打开浏览器访问 https://www.xiaohongshu.com
2. 登录账号
3. F12 打开开发者工具，在 Network 标签页中找到请求
4. 复制请求头中的 Cookie 值
5. 粘贴到 application.yml 的 `xhs.cookies` 字段

### 环境配置
- 开发环境：使用 H2 内存数据库
- 生产环境：PostgreSQL 数据库
- 端口：14315

## 重要实现细节

### Cookie 登录
只需要设置 web_session cookie 即可实现登录状态，其他 cookie 用于签名生成。

### 反爬虫对策
1. 使用真实浏览器环境（Playwright）
2. 注入反检测脚本（stealth.min.js）
3. 动态生成签名参数
4. 控制请求频率

### 已知问题和限制
1. 获取笔记详情时需要 xsecSource 和 xsecToken 参数（目前使用空字符串）
2. 需要定期更新 Cookie
3. 依赖 Playwright 1.40.0 版本的 API

## 与 Python MediaCrawler 项目的兼容性

为确保与 Python 版本功能一致，需要特别注意：
1. **签名算法必须与 Python 版本完全一致**，特别是 mrc 数组和编码规则
2. **请求头大小写格式必须严格匹配 Python 版本**
3. **Cookie 处理策略保持一致**
4. 参考路径：/Users/libre/code/python/MediaCrawler

## 测试说明

测试使用 H2 内存数据库和模拟数据，不会真实调用小红书 API。运行测试前确保 Playwright 已安装。

## 使用示例

启动服务后，可以通过以下方式调用：

```bash
# 搜索笔记
curl "http://localhost:14315/api/xhs/search?keyword=美食&page=1&sortType=GENERAL"

# 获取笔记详情
curl "http://localhost:14315/api/xhs/note/65f7f3b8000000001203a4b5"

# 获取用户信息
curl "http://localhost:14315/api/xhs/user/5f8d9a5d000000000101c8b9"

# 检查登录状态
curl "http://localhost:14315/api/xhs/login/status"
```

## 注意事项

1. **编译错误修复**：
   - 如遇到 "integer number too large" 错误，需要给大数字添加 L 后缀
   - 如遇到类找不到错误，检查 Playwright API 的正确导入路径
2. **Playwright API 版本**：本项目使用 1.40.0 版本，注意 API 兼容性
3. **请求头顺序可能影响请求成功率**
4. **Cookie 有效期有限**，需要定期更新
5. **遵守 robots.txt 和使用条款**，合理控制爬取频率

## 数据库错误

### 数据库初始化问题
- SQL 语句执行失败时的常见原因：
  - 数据库表未正确创建
  - 数据库连接配置错误
  - 缺少必要的建表 SQL 脚本
- **具体错误示例**：
  - `org.postgresql.util.PSQLException: ERROR: relation "crawler_task_status" does not exist`
  - 这表明 `crawler_task_status` 表不存在，需要检查数据库初始化脚本
  - 解决方法：
    1. 检查 `application.yml` 中的数据库配置
    2. 确认已执行正确的建表 SQL
    3. 检查数据库迁移脚本（如 Flyway）是否正确配置
    4. 手动创建缺失的数据库表