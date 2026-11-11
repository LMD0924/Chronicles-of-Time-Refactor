# 拾光记 · Chronicles of Time

> **一条时间线，串起记录、学习、规划、社区与档案**
>
> 不再是「高中 / 大学 / 职场」三个互不相干的模块。所有功能都属于同一个用户，
> 都挂在同一条时间轴上，彼此关联、彼此印证。

本项目是「拾光记」的重构版：功能点保持不变，按**混合 DDD**重新组织后端，
用**拾光时间线**重新组织前端信息架构，视觉语言沿用 `AdmitAtlas`（PathForge 纸面风格），
唯一不同的是**路由切换过渡改为「时间轴卷轴」**。

---

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3.5.11 · Spring Cloud 2025.0.1（依赖保留、默认关闭）· Maven 多模块 · Java 17 |
| 运行形态 | **混合 DDD 模块化单体**：平铺限界上下文 + 唯一 `launcher` 启动类、唯一 8080 端口、唯一可执行 jar |
| 持久层 | MyBatis-Plus 3.5.9 + MySQL 8 单库 + Flyway 自动迁移；`dev` profile 可切 H2 文件库 |
| 认证 | PBKDF2WithHmacSHA256 密码哈希 + 纯 JDK HS256 JWT + 三级路径拦截器 + `token_version` 主动失效 |
| 前端 | Vue 3 · Vite 5 · **JavaScript** · TailwindCSS 3 · Pinia · Vue Router · Axios · ECharts 5（按需引入）· markdown-it |
| 基础设施 | Redis（保留，懒连接）；P2 起可演进 Nacos / Gateway / Elasticsearch / MinIO |

用户与管理员**共用同一个前端**：管理员登录后在同一套导航里多出「管理后台」入口，不另建前端工程。

## 目录结构

```
Chronicles-of-Time/
├── back-end-spring-cloud/           # 后端聚合工程（父 POM 在此）
│   ├── common-core/                 # 统一响应、异常、Repository 契约、JWT、鉴权、模块探针
│   ├── api/                         # 跨上下文共享 DTO 契约
│   ├── gateway/                     # 统一 Web 入口配置（CORS）
│   ├── identity/                    # 身份与访问：认证、用户、角色、个人中心、管理后台、审计
│   ├── timeline/                    # 拾光时间线：记录、里程碑、贯通关联、工作与进阶
│   ├── learning/                    # 学习：题库、练习、答题、错题、成绩、图谱
│   ├── planning/                    # 规划：选科、志愿、等级、专业要求、课程学分、复习与目标
│   ├── archive/                     # 档案：简历、证书、课程树、论文、看板、选课参考
│   ├── community/                   # 社区：内容、评论、点赞、收藏、文章图谱
│   ├── file/                        # 图片 / 文件 / 批量上传
│   ├── launcher/                    # ★ 唯一启动类、唯一配置、Flyway V1/V2
│   │   └── src/main/resources/db/migration/
│   ├── sql/                         # 生产索引参考脚本（不参与启动）
│   ├── deploy/                      # docker-compose（MySQL / Redis）
│   └── docs/后端模块总览.md
├── front-end/                       # Vue 3 SPA（Vite 5）
│   └── src/
│       ├── config/navigation.js     # 顶部导航 + 悬停 Mega Menu 配置
│       ├── components/ResourcePage.vue  # 通用 CRUD 页面（列表/搜索/新增/编辑/删除/批量删除）
│       ├── components/AppHeader.vue     # 顶部胶囊导航 + 悬停大面板
│       ├── layouts/DefaultLayout.vue    # 时间轴卷轴过渡
│       ├── views/                   # 55 个页面（含 views/admin/ 7 页）
│       └── style.css                # 与 AdmitAtlas 一致的视觉 token + 卷轴过渡
├── docs/                            # 全部设计文档（中文）
└── README.md / LICENSE / .gitignore
```

每个业务模块内部为严格四层：`interfaces / application / domain(model+repository) / infrastructure(persistence)`。
依赖方向 `launcher → 各业务模块 → api → common-core`，业务模块之间原则上不直接互相依赖，通过 `AuthContext` 取当前用户。
唯一例外：`archive → identity` 的单向依赖，用于「完整简历」接口读取 identity 上下文的 `resume-sections`。

## 快速开始

```bash
# 后端：编译 12 个 Reactor 模块并运行单元测试（JDK 17+、Maven 3.8+）
cd back-end-spring-cloud
mvn -B clean package

# 方式一：MySQL 8（默认；URL 已带 createDatabaseIfNotExist=true，首次启动自动建库）
# root 密码必须走环境变量，默认空密码会报 Access denied；不要把密码写进配置文件提交到仓库
export COT_DB_USER=root COT_DB_PASSWORD=<你的密码>    # Windows: setx COT_DB_PASSWORD "你的密码"
# 需要改连接串时再覆盖 COT_DB_URL
java -jar launcher/target/cot-launcher-0.1.0-SNAPSHOT.jar

# 方式二：零外部依赖演示（H2 文件库 + 同一套 Flyway 迁移）
java -jar launcher/target/cot-launcher-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev

# 验证
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/system/modules
curl http://localhost:8080/api/v1/dashboard

# 前端（Node 20+）
cd ../front-end
npm install
npm run dev          # http://localhost:5173
npm run build        # 产物 dist/
```

Flyway 在启动时自动建表并写入演示数据，无需手工执行 SQL；`chronicles_of_time` 库由
`createDatabaseIfNotExist=true` 自动创建，也可预先手动建。
JWT 密钥走 `COT_AUTH_SECRET`（**生产必须覆盖开发默认值**），上传目录走 `COT_UPLOAD_DIR`。

### 端口说明（重要）

默认端口是 `8080`，但有两处常见冲突，启动前请留意：

1. **参考项目占用**：本机 `AdmitAtlas` 若正在 IDEA 中运行，会占用 `8080`。此时新项目连的是老服务，
   表现是接口全部返回 404。请换端口启动，并同步修改 `front-end/vite.config.js` 的 proxy 目标：
   ```bash
   java -jar launcher/target/cot-launcher-0.1.0-SNAPSHOT.jar \
     --spring.profiles.active=dev --server.port=18080
   ```
2. **环境变量松散绑定**：部分 IDE / 代理会注入 `SERVER__PORT`（例如 `58693`），
   Spring Boot 会把它宽松绑定成 `server.port` 并覆盖配置文件。命令行 `--server.port=8080`
   优先级最高，可强制锁定端口；也可在启动前 `unset SERVER__PORT`（Windows：`set SERVER__PORT=`）。

若改用非 8080 端口，前端 `vite.config.js` 里 `/api` 与 `/files` 两处 `target` 必须一起改。

## 两种身份与权限

| 身份 | 可访问 |
|---|---|
| **游客** | 首页 `/`、内容广场 `/community`、文章详情、文章图谱、标签洞察、公开仪表盘 |
| **用户 USER** | 游客能力 + 拾光时间线、记录、学习、规划、档案、社区写操作、个人中心、设置 |
| **管理员 ADMIN** | 用户能力 + `/admin` 管理后台（概览 / 用户与角色 / 内容治理 / 数据字典 / 操作审计 / 系统信息） |

内置演示账号（**仅本地演示，生产必须删除**）：

| 账号 | 密码 | 角色 |
|---|---|---|
| `user` | `Cot@2026user` | USER |
| `admin` | `Cot@2026admin` | ADMIN |

安全口径：自助注册强制 USER；登录失败按账号计数，第 5 次冻结 30 分钟；
退出登录、停用账号、重置密码、改密码都会让已签发令牌立即失效；
所有管理端写操作写入 `audit_log`；业务数据默认按 `user_id` 隔离，管理员可显式 `all=true` 跨账号核查。
跨域聚合的 `/api/v1/timeline/feed` 同样只返回当前账号的私有节点（社区已发布文章为公开内容，
所有登录用户可见），管理员可用 `all=true` 跨账号核查；该接口需登录，首页仅在已登录时请求。

## 前端视觉

沿用 `AdmitAtlas` 的**轻盈纸面风格**：极浅冷白渐变底、半透明磨砂轻卡片、蓝色链接文字、
彩色描边按钮（边框 / 文字 / 图标同色）、彩色小胶囊、6px 细进度条、手写体中文大标题、
双主题（默认浅色，顶栏可切暗色并记忆）、滚动收缩胶囊导航、滚动入场 / 数字滚动 / 墨迹揭示 / 光斑漂移。

**布局差异**：顶部胶囊导航（样式完全一致）+ 菜单过多时**鼠标悬停总菜单弹出 Mega Menu 大面板**，
首页改为「手写体 Hero + 时间线预览 + 4 个彩色圆形指标 + 功能宫格」。

**唯一不同的视觉项 —— 路由过渡**：参考项目是「磨砂 HUD 遮罩 + 扫描光带」，本项目换成
**时间轴卷轴**：旧页面像纸页向左卷起退场、新页面从右侧沿时间轴展开，切换瞬间沿内容区左侧
掠过一条时间轴脊线；全部只动画 `transform / opacity / filter`，`prefers-reduced-motion` 下自动降级。

手写体字体 `public/fonts/MaShanZheng.woff2` 为按需子集（26 KB），新增手写文案需重新生成子集。

## 功能全景

功能点全部保留，只改命名与组织方式（完整对照见 `docs/功能清单.md` 与 `docs/迁移对照.md`）：

| 分组 | 主要内容 |
|---|---|
| 时间线 | 成长记录、里程碑、贯通关联、工作记录、进阶记录、跨域聚合 feed、趋势与统计 |
| 学习 | 学习总览、题库、练习中心（判分）、答题记录、错题本、成绩分析、知识图谱、学习路径、练习场次 |
| 规划 | 学科字典与组合、学科雷达、选科中心（9 个面板）、志愿填报（检索/推荐/模拟/方案/明细）、复习计划、目标路径、综评、强基清单、只读视图、GPA 模拟、毕业缺口、学期课表、方案对比、等级量表、专业要求 |
| 档案 | 个人中心、档案概览、简历分组编辑、证书、课程树、论文与建议、论文看板、选课参考、设置 |
| 社区 | 内容广场、Markdown 发布与预览、详情与评论、点赞收藏、文章图谱、标签洞察 |
| 管理 | 概览、用户与角色、内容治理、数据字典、操作审计、系统信息 |
| 基础设施 | 图片 / 单文件 / 批量上传、双主题、路由守卫、全局错误与 401 会话清理 |

## 文档索引

| 文档 | 内容 |
|---|---|
| [架构设计](docs/架构设计.md) | 限界上下文、四层结构、依赖方向、时间线聚合、运行形态 |
| [功能清单](docs/功能清单.md) | 全部功能点与交付对照 |
| [迁移对照](docs/迁移对照.md) | 老项目 → 新项目的命名、路由、模块映射 |
| [前端视觉规范](docs/前端视觉规范.md) | 设计 token、动效、导航与卷轴过渡 |
| [数据模型](docs/数据模型.md) | 43 张表与字段字典 |
| [接口说明](docs/接口说明.md) | REST 约定、权限矩阵、端点清单 |
| [开发指南](docs/开发指南.md) | 环境、构建、验证、演示账号 |
| [路线图](docs/路线图.md) | P0 - P3 演进 |
| [后端模块总览](back-end-spring-cloud/docs/后端模块总览.md) | 11 个模块职责 |

## License

[MIT](LICENSE) © 2026 LMD0924
