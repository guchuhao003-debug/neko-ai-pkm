# Spring AI Alibaba 知识点总结

## 一、Spring AI Alibaba 概述

### 问题1：什么是 Spring AI Alibaba？
**答案：**
Spring AI Alibaba 是阿里巴巴开源的 AI 应用开发框架，基于 Spring AI 构建，是阿里云通义系列模型及服务在 Java AI 应用开发领域的最佳实践。

核心特点：
- 基于 Spring AI 框架，完全兼容 Spring 生态
- 深度集成阿里云百炼大模型服务平台
- 支持通义千问（Qwen）系列模型
- 提供 Agent 框架和多 Agent 编排能力
- 内置 RAG、Function Calling 等能力
- 最新版本：1.1.2.0，适配 Spring AI 1.1.2

---

### 问题2：Spring AI Alibaba 的生态系统包含哪些组件？
**答案：**
```
Spring AI Alibaba 生态系统
├── 核心框架
│   ├── Spring AI Alibaba Agent Framework - Agent 和多 Agent 应用框架
│   ├── Spring AI Alibaba Graph - 有状态 Agent 编排框架
│   ├── Spring AI Alibaba Studio - Agent 可视化调试工具
│   └── Spring AI Alibaba Admin - Agent 管理和评估工具
├── 扩展组件
│   └── Spring AI Extensions - DashScopeChatModel、Tool、MCP 注册中心等
└── 生产级应用
    ├── JManus - Manus 的 Java 实现
    ├── Copilot - AI 编程助手
    ├── DataAgent - 自然语言转 SQL
    └── DeepResearch - 深度研究工具
```

---

### 问题3：Spring AI Alibaba 与 Spring AI 的关系？
**答案：**
- **Spring AI** 是 Spring 官方的 AI 框架，提供统一的 API 抽象
- **Spring AI Alibaba** 是 Spring AI 的扩展实现，专注于阿里云生态
- Spring AI Alibaba 实现了 Spring AI 的核心接口（ChatModel、EmbeddingModel 等）
- 提供了 DashScopeChatModel 实现，对接阿里云百炼平台
- 扩展了 Agent Framework、Graph 编排等高级能力

---

## 二、快速开始

### 问题4：如何创建 Spring AI Alibaba 项目？
**答案：**
**1. 添加依赖：**
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.1.2.0</version>
</dependency>
```

**2. 配置 application.yml：**
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-turbo
          temperature: 0.8
```

**3. 使用 ChatClient：**
```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

---

### 问题5：如何配置 DashScope API Key？
**答案：**
```yaml
# 方式1：直接配置
spring:
  ai:
    dashscope:
      api-key: sk-xxxxxxxxxxxxxxxxxxxxxxxx

# 方式2：环境变量
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}

# 方式3：自定义配置
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com
```

---

## 三、模型使用

### 问题6：Spring AI Alibaba 支持哪些通义千问模型？
**答案：**
| 模型名称 | 特点 | 适用场景 |
|----------|------|----------|
| qwen-turbo | 速度快，成本低 | 简单对话、文本处理 |
| qwen-plus | 性能均衡 | 通用场景 |
| qwen-max | 最强能力 | 复杂推理、创意写作 |
| qwen-max-longcontext | 长上下文 | 长文档处理 |
| qwen-vl | 多模态 | 图片理解 |

```yaml
spring:
  ai:
    dashscope:
      chat:
        options:
          model: qwen-max
          temperature: 0.7
          max-tokens: 2000
```

---

### 问题7：如何使用 Spring AI Alibaba 进行流式对话？
**答案：**
```java
// 方式1：使用 ChatClient
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String message) {
    return chatClient.prompt()
        .user(message)
        .stream()
        .content();
}

// 方式2：使用 ChatModel
@GetMapping(value = "/stream2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ChatResponse> stream2(@RequestParam String message) {
    return chatModel.stream(new Prompt(new UserMessage(message)));
}

// 方式3：在 WebFlux 中使用
@GetMapping(value = "/stream3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamSSE(@RequestParam String message) {
    return chatClient.prompt()
        .user(message)
        .stream()
        .content()
        .map(content -> ServerSentEvent.builder(content).build());
}
```

---

### 问题8：如何使用通义千问的多模态能力？
**答案：**
```java
// 图片理解
@GetMapping("/image")
public String analyzeImage(@RequestParam String imageUrl) {
    return chatClient.prompt()
        .user(u -> u
            .text("请描述这张图片的内容")
            .media(MimeTypeUtils.IMAGE_JPEG, new UrlResource(imageUrl))
        )
        .call()
        .content();
}

// 本地图片
@GetMapping("/local-image")
public String analyzeLocalImage() {
    return chatClient.prompt()
        .user(u -> u
            .text("这张图片里有什么？")
            .media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("test.png"))
        )
        .call()
        .content();
}
```

---

## 四、Agent 框架

### 问题9：什么是 Spring AI Alibaba Agent Framework？
**答案：**
Agent Framework 是 Spring AI Alibaba 提供的 Agent 开发框架，用于构建智能体应用。

核心概念：
- **Agent**：具备推理、规划、工具调用能力的智能体
- **Tool**：Agent 可以调用的外部工具
- **Memory**：Agent 的记忆管理
- **Planning**：任务规划和分解

```java
// 定义 Agent
@Component
public class MyAgent {

    private final ChatClient chatClient;

    public MyAgent(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultTools(new WeatherTool(), new CalculatorTool())
            .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

---

### 问题10：如何在 Spring AI Alibaba 中定义和使用 Tool？
**答案：**
```java
// 方式1：使用 Function Bean
@Bean
@Description("获取指定城市的天气信息")
public Function<WeatherRequest, WeatherResponse> getWeather() {
    return request -> {
        // 调用天气 API
        return new WeatherResponse(request.city(), "晴", 25);
    };
}

// 方式2：使用 @Tool 注解
@Component
public class WeatherTool {

    @Tool(description = "获取指定城市的天气信息")
    public WeatherResponse getWeather(@ToolParam("城市名称") String city) {
        return new WeatherResponse(city, "晴", 25);
    }
}

// 方式3：使用 ToolCallback
@Bean
public ToolCallbackProvider toolCallbackProvider() {
    return new MethodToolCallbackProvider(new WeatherTool());
}

// 使用 Tool
String response = chatClient.prompt()
    .user("北京今天天气怎么样？")
    .tools(new WeatherTool())
    .call()
    .content();
```

---

### 问题11：Spring AI Alibaba Graph 是什么？
**答案：**
Graph 是用于构建、管理和部署长期运行的有状态 Agent 的编排框架。

核心特点：
- 基于 DAG（有向无环图）的流程编排
- 支持状态管理和持久化
- 支持条件分支和循环
- 适合复杂工作流场景

```java
// 定义 Graph
Graph graph = Graph.builder()
    .addNode("start", new StartNode())
    .addNode("process", new ProcessNode())
    .addNode("end", new EndNode())
    .addEdge("start", "process")
    .addEdge("process", "end")
    .build();

// 执行 Graph
GraphResult result = graph.run(initialState);
```

---

### 问题12：什么是 Multi-Agent？Spring AI Alibaba 如何支持？
**答案：**
Multi-Agent 是多个 Agent 协作完成复杂任务的架构模式。

Spring AI Alibaba 支持的 Multi-Agent 模式：
1. **Supervisor 模式**：主管 Agent 协调多个工作 Agent
2. **Routing 模式**：根据意图路由到不同 Agent
3. **Chain 模式**：Agent 链式处理
4. **Parallel 模式**：多个 Agent 并行处理

```java
// Supervisor 模式示例
Graph supervisor = Graph.builder()
    .addNode("supervisor", new SupervisorNode())
    .addNode("researcher", new ResearchAgent())
    .addNode("coder", new CoderAgent())
    .addNode("writer", new WriterAgent())
    .addConditionalEdge("supervisor", state -> {
        // 根据任务类型选择 Agent
        return state.getTaskType();
    })
    .build();
```

---

## 五、RAG 支持

### 问题13：Spring AI Alibaba 如何实现 RAG？
**答案：**
```java
// 1. 配置向量存储
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    // 使用阿里云 AnalyticDB PostgreSQL
    return new PgVectorStore(jdbcTemplate, embeddingModel);
}

// 2. 加载文档
@Service
public class DocumentService {

    @Autowired
    private VectorStore vectorStore;

    public void loadDocuments() {
        // 从文件加载
        List<Document> documents = new TextReader("docs/").get();
        vectorStore.add(documents);
    }
}

// 3. RAG 问答
@GetMapping("/rag")
public String ragChat(@RequestParam String question) {
    return chatClient.prompt()
        .user(question)
        .advisors(new QuestionAnswerAdvisor(vectorStore))
        .call()
        .content();
}
```

---

### 问题14：Spring AI Alibaba 支持哪些向量数据库？
**答案：**
- **阿里云服务**：AnalyticDB PostgreSQL、Lindorm
- **开源方案**：Milvus、Chroma、PgVector
- **云服务**：Pinecone、MongoDB Atlas

```yaml
# 配置 AnalyticDB PostgreSQL
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
```

---

## 六、高级特性

### 问题15：Spring AI Alibaba 的 Prompt 工程最佳实践？
**答案：**
```java
// 1. 使用 PromptTemplate
PromptTemplate template = new PromptTemplate("""
    你是一个{role}专家。
    背景信息：{context}
    用户问题：{question}
    请用{style}的方式回答。
    """);

// 2. 使用 SystemMessage 定义角色
SystemMessage systemMessage = new SystemMessage("""
    你是一个专业的 Java 技术顾问。
    你需要：
    1. 使用简洁明了的语言
    2. 提供代码示例
    3. 解释原理
    """);

// 3. 结构化输出
record AnalysisResult(String summary, List<String> keyPoints, String recommendation) {}

AnalysisResult result = chatClient.prompt()
    .system(systemMessage)
    .user("分析 Spring Boot 3 的新特性")
    .call()
    .entity(AnalysisResult.class);
```

---

### 问题16：如何在 Spring AI Alibaba 中实现对话记忆？
**答案：**
```java
// 1. 配置 ChatMemory
@Bean
public ChatMemory chatMemory() {
    return new InMemoryChatMemory();
    // 或数据库存储
    // return new JdbcChatMemory(jdbcTemplate);
}

// 2. 使用 MemoryAdvisor
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
    .build();

// 3. 带 sessionId 的对话
@GetMapping("/chat/{sessionId}")
public String chat(
    @PathVariable String sessionId,
    @RequestParam String message
) {
    return chatClient.prompt()
        .user(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
        .call()
        .content();
}
```

---

### 问题17：Spring AI Alibaba 如何集成 MCP（Model Context Protocol）？
**答案：**
MCP 是 Anthropic 提出的模型上下文协议，用于标准化 AI 模型与外部工具的交互。

```java
// 1. 配置 MCP Server
@Bean
public McpServer mcpServer() {
    return McpServer.builder()
        .name("my-mcp-server")
        .version("1.0.0")
        .tools(new WeatherTool(), new DatabaseTool())
        .build();
}

// 2. 注册 MCP Tool
@Bean
public ToolCallbackProvider mcpToolCallbackProvider(McpServer mcpServer) {
    return new McpToolCallbackProvider(mcpServer);
}

// 3. 使用 MCP Tool
String response = chatClient.prompt()
    .user("查询数据库中的用户信息")
    .tools(mcpToolCallbackProvider)
    .call()
    .content();
```

---

### 问题18：Spring AI Alibaba Studio 有什么作用？
**答案：**
Spring AI Alibaba Studio 是 Agent 可视化调试工具，功能包括：
1. **实时对话**：可视化 Agent 对话过程
2. **推理链展示**：查看 Agent 的思考和决策过程
3. **工具调用追踪**：查看 Tool 调用详情
4. **性能分析**：Token 使用、响应时间等
5. **Prompt 调试**：在线调试 Prompt

---

## 七、生产实践

### 问题19：Spring AI Alibaba 在阿里巴巴内部有哪些应用？
**答案：**
1. **JManus**：Manus 的 Java 实现，用于自动化任务执行
2. **Copilot**：AI 编程助手，帮助开发者编写代码
3. **DataAgent**：自然语言转 SQL，简化数据库查询
4. **DeepResearch**：深度研究工具，用于信息收集和分析
5. **智能客服**：基于 RAG 的企业知识库问答
6. **代码审查**：自动化代码审查和建议

---

### 问题20：使用 Spring AI Alibaba 的最佳实践有哪些？
**答案：**
1. **模型选择**：根据场景选择合适的模型（turbo/plus/max）
2. **成本控制**：设置合理的 max-tokens 和 temperature
3. **异常处理**：处理限流、超时等异常
4. **缓存策略**：对相同查询缓存结果
5. **监控告警**：监控 API 调用量和延迟
6. **安全防护**：输入输出过滤，防止 Prompt 注入
7. **版本管理**：关注框架版本更新，及时升级
8. **测试策略**：使用 SimpleVectorStore 进行单元测试
