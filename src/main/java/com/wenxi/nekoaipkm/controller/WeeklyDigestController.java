package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.vo.ChatResponse;
import com.wenxi.nekoaipkm.service.WeeklyDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 周报生成接口
 */
@RestController
@RequestMapping("/weeklyDigest")
@RequiredArgsConstructor
public class WeeklyDigestController {

    private final WeeklyDigestService weeklyDigestService;

    /**
     * 生成本周知识周报
     *
     * @return
     */
    @PostMapping("/weeklyDigest")
    public ChatResponse weeklyDigest() {
        return new ChatResponse(weeklyDigestService.generateCurrentWeekDigest());
    }
}
