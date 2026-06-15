package com.wenxi.nekoaipkm.service;


import com.wenxi.nekoaipkm.constant.PromptConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RAG 问答服务，负责基于本地知识库回答用户问题
 *
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /**
     * AI 对话客户端 ChatClient 构建器，
     * 用于配置并创建和大模型交互的 ChatClient。
     */
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 基于 PgVector 向量存储
     */
    private final VectorStore vectorStore;

    @Value("${pkm.rag.top-k:5}")
    private Integer topK;

    @Value("${pkm.rag.similarity-threshold:0.70}")
    private Double similarityThreshold;

    /**
     * 基于本地知识库问答
     *
     * @param message   用户问题消息
     * @return  AI 回答
     */
    public String ask(String message) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        ChatClient chatClient = chatClientBuilder.defaultSystem(PromptConstant.ASK_PROMPT)
                .build();

        return chatClient.prompt()
                .user(message)
                .advisors(questionAnswerAdvisor)
                .call()
                .content();
    }
}
