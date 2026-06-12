package com.wenxi.nekoaipkm.assistant;

import com.wenxi.nekoaipkm.advisor.MyLoggerAdvisor;
import com.wenxi.nekoaipkm.constant.PromptConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

/**
 * 知识管理智能助手
 */
@Component
@Slf4j
public class PkmAssistant {


    private final ChatClient chatClient;

    /**
     * 构造函数
     *
     *Spring Boot 自动配置已经创建了一个 ChatClient.Builder Bean，
     * 这个 Builder 内部已经包含了 ChatModel（从配置文件读取 spring.ai.dashscope.api-key 等）。
     * @param builder
     */
    public PkmAssistant(ChatClient.Builder builder) {
        // 基于内存的对话记忆存储
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        this.chatClient = builder
                .defaultSystem(PromptConstant.SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志
                        new MyLoggerAdvisor()
                )
                .build();
    }



}
