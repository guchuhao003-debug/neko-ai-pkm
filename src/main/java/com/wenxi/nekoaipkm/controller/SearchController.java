package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.dto.SearchRequestDto;
import com.wenxi.nekoaipkm.model.vo.SearchResultResponse;
import com.wenxi.nekoaipkm.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 语义检索接口，用于调试 PgVector 的检索效果
 *
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 根据自然语言查询相关笔记文本块
     *
     * @param request   检索请求
     * @return  相关笔记文本块列表
     */
    @PostMapping
    public List<SearchResultResponse> search(@Valid @RequestBody SearchRequestDto request) {
        return searchService.search(request.query(),request.topK());
    }

}
