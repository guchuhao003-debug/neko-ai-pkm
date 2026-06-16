package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.vo.LinkCandidateResponse;
import com.wenxi.nekoaipkm.service.LinkRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

}
