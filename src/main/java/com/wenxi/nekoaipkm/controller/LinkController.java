package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.vo.LinkCandidateResponse;
import com.wenxi.nekoaipkm.service.LinkRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 笔记链接推荐接口
 *
 */
@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkRecommendationService linkRecommendationService;

    /**
     * 为指定笔记推荐相关笔记
     *
     * @param noteId    源笔记 ID
     * @return  推荐的相关笔记列表
     */
    @PostMapping("/recommend/{noteId}")
    public List<LinkCandidateResponse> recommend(@PathVariable String noteId) {
        return linkRecommendationService.recommendLinks(noteId);
    }

    /**
     * 提交后台推荐任务，接口立即返回。
     *
     * @param noteId 源笔记 ID
     * @return 任务提交结果
     */
    @PostMapping("/recommend/{noteId}/tasks")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> recommendInBackground(@PathVariable String noteId) {
        linkRecommendationService.recommendLinksInBackground(noteId);
        return Map.of("message","链接推荐任务已提交");
    }

}
