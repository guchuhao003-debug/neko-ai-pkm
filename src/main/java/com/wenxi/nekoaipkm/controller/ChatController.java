package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.dto.ChatRequest;
import com.wenxi.nekoaipkm.model.vo.ChatResponse;
import com.wenxi.nekoaipkm.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * RAG 问答对话接口，负责提供基于 RAG 的问答对话能力
 *
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 基于 RAG 本地知识库提问
     *
     * @param request   用户问题消息
     * @return  AI 基于本地知识库回答
     */
    @PostMapping
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chatService.ask(request.message()));
    }
}
