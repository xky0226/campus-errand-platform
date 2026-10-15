# Ollama 安装与模型部署说明

> 对应讲义「5-4、对话记忆、Ollama 本地模型」的**模块 07 Ollama 本地模型**。
> 本工程已编译通过，并在本机跑通了端到端实测（见文末「九、实测结果」）。
> 下面第一～五节是本机环境要做一次的安装/部署步骤，换机器或重装时照着走一遍即可。

---

## 一、Ollama 是什么

一个开源的本地大模型运行框架，可以在自己电脑上下载并运行各种开源大模型
（DeepSeek、Qwen、Llama、Mistral 等），**数据不出本机、可离线**。

官网：https://ollama.com

---

## 二、安装（Windows）

### 方式一：默认安装

1. 到官网下载 `OllamaSetup.exe`；
2. 双击安装（默认装到 C 盘）；
3. 打开**新的**终端验证：

```bash
ollama --version
```

### 方式二：指定安装目录（不占 C 盘）

打开命令行，切换到安装包所在目录，执行：

```bat
OllamaSetup.exe /DIR=D:\Ollama
```

### 修改模型存储位置（重要）

模型体积很大（几个 GB 到几十 GB），**默认存在 C 盘**。

新建**系统环境变量**：

| 变量名 | 变量值 |
|---|---|
| `OLLAMA_MODELS` | `E:\ollama\models` |

保存后**重启终端和 Ollama**（必要时重启电脑）才生效。

> 课堂上如果老师提供了打包好的 `ollama.zip`（模型文件），
> 直接解压拷贝到 `E:\ollama\models` 即可，省去重新下载几个 GB。

---

## 三、按显存选模型

模型名里的 `b` 表示「十亿（billion）参数」。参数越大推理越强，但显存要求越高。
下面是 DeepSeek-R1 的蒸馏（量化）版本，能力比云端满血版弱，但足够学习和内网使用。

| 模型 | 大致显存需求 | 说明 |
|---|---|---|
| `deepseek-r1:1.5b` | 2–4 GB | 最轻，入门 / 集成测试（**本工程默认用这个**） |
| `deepseek-r1:7b` | 6–8 GB | 8GB 显存可跑 |
| `deepseek-r1:14b` | 10–12 GB | 机房教师机上限参考 |
| `deepseek-r1:32b` | 20 GB+ | 较高 |
| `deepseek-r1:70b` | 40 GB+ | 约需双 4090 |

**没装成功之前，先用 1.5b 把流程跑通。**

---

## 四、下载并运行模型

```bash
# 下载模型（会拉几个 GB，耐心等）
ollama pull deepseek-r1:1.5b

# 查看本地已装模型
ollama list
```

> ⚠️ **本机实际用的是魔搭（ModelScope）镜像**：官方 registry 走海外线路，
> 国内拉 1.5b 要等很久；魔搭快很多（本次 1.1 GB 几分钟下完）。命令是：
>
> ```bash
> ollama pull modelscope.cn/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF
> ```
>
> 这样拉下来的模型名是 `modelscope.cn/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF:latest`，
> 和 `application.yaml` 里写的 `deepseek-r1:1.5b` 对不上。再起个别名即可
> （`cp` 只复制 manifest，blob 是共享的，**不额外占磁盘**）：
>
> ```bash
> ollama cp modelscope.cn/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF:latest deepseek-r1:1.5b
> ```
>
> 别名建好后，`ollama list` 里两个名字都在，配置文件不用改。

```bash
# 进入交互对话（Ctrl + D 退出）
ollama run deepseek-r1:1.5b
>>> 你好

# 查看正在运行的模型
ollama ps

# 停止模型
ollama stop deepseek-r1:1.5b

# 删除模型
ollama rm deepseek-r1:1.5b

# 以 API 服务方式运行（默认 11434 端口）
ollama serve
```

## 五、验证 Ollama 服务可用（不经过 Java）

```bash
curl http://localhost:11434/api/generate -d "{\"model\":\"deepseek-r1:1.5b\",\"prompt\":\"你好\"}"
```

有 JSON 返回就说明服务正常。

---

## 六、启动本工程

### 1. 先确认 Ollama 在跑

任务栏有 Ollama 图标，或 `ollama list` 能正常输出。

### 2. 启动 Spring Boot

用 IDEA 打开 `ollama-demo` 目录，运行 `OllamaDemoApplication`，
或者命令行 `mvn spring-boot:run`。

> ⚠️ **端口占用提醒**：本工程和讲义练习工程 `ai-demo` 都用 **8080**。
> 两个**不能同时启动**。要同时跑的话，把其中一个的 `server.port` 改成 8081。

### 3. 访问接口

```
http://localhost:8080/ollama/chat?question=你是谁
```

回答完全来自本地 `deepseek-r1:1.5b` 模型，**断网也能工作**。

---

## 七、常见问题

**Q1：访问接口报连接失败？**
确认 Ollama 正在运行（`ollama list` 正常）、地址是 `http://localhost:11434`、
以及模型确实已经下载（模型名要和 `ollama list` 里显示的完全一致）。

**Q2：本地模型回答很慢？**
小参数模型在纯 CPU 上较慢属正常；有独立显卡会快很多；可以换更小的 1.5b。

**Q3：模型文件还是占 C 盘？**
确认 `OLLAMA_MODELS` 环境变量配置后**重启了 Ollama**（必要时重启电脑）。

**Q4：能同时用云端和 Ollama 吗？**
可以。配置文件里两者并存，分别声明不同的 ChatClient Bean 即可
（见讲义 5-4 模块 07 步骤 3）。

**Q5：`ollama --version` 提示不是内部或外部命令？**
安装后要**新开**一个终端，环境变量才会生效；还不行就重启电脑。

---

## 八、可视化客户端（可选）

不想用命令行的话，可以用图形工具连本地模型：

| 工具 | 说明 |
|---|---|
| PageAssist | 浏览器插件，可对接本地 Ollama |
| Chatbox AI | 跨平台桌面客户端，https://chatboxai.app/zh |
| Cherry Studio | 功能齐全，支持众多服务商与 OpenAI 兼容接入，https://cherry-ai.com |

---

## 九、实测结果

2026-10-15 在本机（Windows 11 / RTX 3060 6G / Ollama 0.33.3）跑通，记录如下。

### 环境

| 项 | 值 |
|---|---|
| Ollama | 0.33.3，装在 `D:\Ollama` |
| 模型目录 | `D:\ollama-models`（`OLLAMA_MODELS` 环境变量） |
| 服务地址 | `127.0.0.1:11434`，已在监听 |
| 对话模型 | `deepseek-r1:1.5b`（1.1 GB，由魔搭镜像拉取后 `ollama cp` 起别名） |
| 应用端口 | 8080 |

### 1. 编译

```
mvn -B compile   →   BUILD SUCCESS（4.3s）
```

### 2. 启动

```
mvn -B spring-boot:run
→ Tomcat started on port 8080 (http)
→ Started OllamaDemoApplication in 1.952 seconds
```

### 3. 直连 Ollama 验证（不经过 Java）

```bash
curl http://127.0.0.1:11434/api/generate \
     -d '{"model":"deepseek-r1:1.5b","prompt":"用一句话介绍你自己","stream":false}'
```

返回 JSON 中 `"model": "deepseek-r1:1.5b"`、`"done": true`，`response` 为中文回答 —— 本地服务正常。

### 4. 打 Spring 接口

```bash
curl "http://localhost:8080/ollama/chat?question=你是谁"     # HTTP 200，1.02s
curl "http://localhost:8080/ollama/chat"                     # 走默认值，HTTP 200，0.60s
```

返回内容（UTF-8）：

```
您好！我是由中国的深度求索（DeepSeek）公司开发的智能助手DeepSeek-R1。
如您有任何任何问题，我会尽我所能为您提供帮助。
```

回答完全来自本地模型，**断网也能工作** —— 讲义模块 07 的目标达成。

### 5. 日志

应用启动到两次请求期间无 ERROR；仅 4 条 WARN，全部是 Maven 从阿里云镜像下载
`maven-metadata.xml` 时的 checksum 校验提示，与工程本身无关。
