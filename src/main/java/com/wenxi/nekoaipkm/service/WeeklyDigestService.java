package com.wenxi.nekoaipkm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wenxi.nekoaipkm.mapper.NoteMapper;
import com.wenxi.nekoaipkm.mapper.WeeklyDigestMapper;
import com.wenxi.nekoaipkm.model.entity.Note;
import com.wenxi.nekoaipkm.model.entity.WeeklyDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 周报生成服务，用于回顾一周新增或更新的笔记
 */
@Service
@RequiredArgsConstructor
public class WeeklyDigestService {

    private final NoteMapper noteMapper;
    private final WeeklyDigestMapper weeklyDigestMapper;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 生成本周知识周报并保存
     *
     * @return  周报内容
     */
    @Transactional
    public String generateCurrentWeekDigest() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);

        LocalDateTime startTime = weekStart.atStartOfDay();
        LocalDateTime endTime = weekEnd.atTime(LocalTime.MAX);

        List<Note> notes = noteMapper.selectList(
                Wrappers.lambdaQuery(Note.class)
                        .between(Note::getUpdatedAt, startTime,endTime)
                        .orderByAsc(Note::getUpdatedAt)
        );

        String prompt = buildDigestPrompt(weekStart, weekEnd, notes);
        String digest = chatClientBuilder.build()
                .prompt()
                .system("你是一个知识管理助理，负责生成简洁、有层次的知识周报。")
                .user(prompt)
                .call()
                .content();

        WeeklyDigest weeklyDigest = new WeeklyDigest();
        weeklyDigest.setWeekStart(weekStart);
        weeklyDigest.setWeekEnd(weekEnd);
        weeklyDigest.setContent(digest);
        weeklyDigest.setSentStatus("pending");
        weeklyDigest.setCreatedAt(LocalDateTime.now());
        weeklyDigestMapper.insert(weeklyDigest);

        return digest;

    }

    /**
     * 构建周报生成 Prompt
     * @param weekStart
     * @param weekEnd
     * @param notes
     * @return
     */
    private String buildDigestPrompt(LocalDate weekStart, LocalDate weekEnd, List<Note> notes) {
        if (notes.isEmpty()) {
            return "本周没有新增或更新笔记，请生成一句简短提醒。";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("请根据以下笔记列表生成")
                .append(weekStart)
                .append("到")
                .append(weekEnd)
                .append("的知识周报：\n\n");

        builder.append("要求：\\n");
        builder.append("1. 总结本周主要学习主题。\\n");
        builder.append("2. 列出值得复习的关键笔记。\\n");
        builder.append("3. 给出下周学习建议。\\n\\n");

        builder.append("笔记列表：\\n");
        for (Note note : notes) {
            builder.append("- ")
                    .append(note.getTitle())
                    .append("，来源：")
                    .append(note.getSourcePath())
                    .append("\\n");
        }

        return builder.toString();
    }

}
