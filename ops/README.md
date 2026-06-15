# BlogHub 运维脚本

一键管理 BlogHub 全栈服务的启停和状态查看。

## 脚本列表

| 脚本 | 说明 |
|------|------|
| `start-all.ps1` / `.bat` | 一键启动全部服务 |
| `stop-all.ps1` / `.bat` | 一键停止服务 |
| `status.ps1` / `.bat` | 查看各服务运行状态 |
| `start-xxl-job.ps1` | 单独启动 XXL-Job Admin |
| `start-backend.ps1` | 单独启动 Backend |
| `start-frontend.ps1` | 单独启动 Frontend |

## 服务清单

| 服务 | 端口 | 访问地址 |
|------|------|----------|
| MySQL | 3306 | — |
| Redis | 6379 | — |
| XXL-Job Admin | 8082 | http://localhost:8082/xxl-job-admin |
| Backend (Spring Boot) | 8081 | http://localhost:8081 |
| Frontend (Vite) | 3000 | http://localhost:3000 |

## 使用方法

### 启动

```powershell
# 启动全部服务（含构建后端 JAR）
.\start-all.ps1

# 跳过后端 mvn package，直接启动
.\start-all.ps1 -SkipBuild

# MySQL/Redis 已在运行时跳过服务启动
.\start-all.ps1 -SkipServices
```

启动流程：依次检测端口 → 跳过已运行服务 → 启动未运行服务 → 等待端口就绪 → 输出汇总状态。

- MySQL / Redis 作为 Windows 服务启动（`net start`）
- XXL-Job Admin、Backend、Frontend 各自在独立 PowerShell 窗口中运行，方便查看日志
- Backend 优先使用打包后的 JAR，未找到时 fallback 到 `mvn spring-boot:run`

### 停止

```powershell
# 停止应用服务，保留 MySQL/Redis
.\stop-all.ps1

# 全部停止（含 MySQL/Redis）
.\stop-all.ps1 -All
```

通过 `netstat -ano` 查找占用端口的进程并终止，无需手动查 PID。

### 状态查看

```powershell
.\status.ps1
```

输出示例：

```
==========================================
  BlogHub Service Status
==========================================
  [ON]  MySQL           PID: 5436
  [ON]  Redis           PID: 5136
  [OFF] XXL-Job Admin   PID: -
  [OFF] Backend         PID: -
  [OFF] Frontend        PID: -
==========================================
```

### .bat 快捷方式

双击 `.bat` 文件即可运行对应的 PowerShell 脚本，无需手动设置执行策略：

```
start-all.bat    # 双击启动
stop-all.bat     # 双击停止
status.bat       # 双击查看状态
```

## 注意事项

- 需要 PowerShell 5.1+（Windows 10/11 自带）
- 启动 MySQL/Redis 服务需要管理员权限（`net start`），建议以管理员身份运行 `start-all`
- 首次启动 Backend 会执行 `mvn package -DskipTests`，耗时较长，可用 `-SkipBuild` 跳过
- 每个长驻服务在独立窗口运行，关闭对应窗口即可单独停止该服务
