package com.wenxi.nekoaipkm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wenxi.nekoaipkm.mapper.NoteMapper;
import com.wenxi.nekoaipkm.mapper.WeeklyDigestMapper;
import com.wenxi.nekoaipkm.model.entity.Note;
import com.wenxi.nekoaipkm.model.entity.WeeklyDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 周报生成服务，用于回顾一周新增或更新的笔记
 */
@Service
@RequiredArgsConstructor
public class WeeklyDigestService {

    private static final ZoneId DIGEST_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String PROMPT_VERSION = "weekly_digest_v1";
    private static final int WEEKLY_DIGEST_NOTE_LIMIT = 80;

    private final NoteMapper noteMapper;
    private final WeeklyDigestMapper weeklyDigestMapper;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 生成当前自然周知识周报。
     *
     * @return 保存后的周报记录
     */
    public WeeklyDigest generateCurrentWeekDigest() {
        LocalDate weekStart = currentWeekStart();
        return generateDigest(weekStart);
    }

    /**
     * 按指定周开始日期生成周报（）
     *
     * @param weekStart 周开始日期，建议传入周一
     * @return 保存后的周报记录
     */
    public WeeklyDigest generateDigest(LocalDate weekStart) {

        LocalDate fixedWeekStart = weekStart.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = fixedWeekStart.plusWeeks(1);
        List<Note> notes = listWeeklyNotes(fixedWeekStart, weekEnd);

        saveGeneratingDigest(fixedWeekStart, weekEnd, notes.size());

        try {
            String prompt = buildDigestPrompt(fixedWeekStart, weekEnd, notes);

            String digest = callDigestModel(prompt);

            return saveGeneratedDigest(fixedWeekStart, weekEnd, notes.size(), digest);
        } catch (Exception e) {
            saveFailedDigest(fixedWeekStart, weekEnd, e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("周报生成失败", e);
        }

    }

    /**
     * 查询当前自然周内新增或更新的笔记
     *
     * @param weekStart 周开始日期，包含
     * @param weekEnd   周结束日期，不包含
     * @return  本周笔记列表
     */
    private List<Note> listWeeklyNotes(LocalDate weekStart, LocalDate weekEnd) {

        LocalDateTime startTime = weekStart.atStartOfDay();
        LocalDateTime endTime = weekEnd.atStartOfDay();

        return noteMapper.selectList(
                Wrappers.lambdaQuery(Note.class)
                        .ge(Note::getUpdatedAt, startTime)
                        .lt(Note::getUpdatedAt, endTime)
                        .orderByDesc(Note::getUpdatedAt)
                        .last("limit " + WEEKLY_DIGEST_NOTE_LIMIT)
        );
    }

    /**
     * 写入生成中状态。同一周重复触发时更新原记录。
     *
     * @param weekStart 周开始日期
     * @param weekEnd 周结束日期
     * @param noteCount 笔记数量
     */
    private void saveGeneratingDigest(LocalDate weekStart, LocalDate weekEnd, int noteCount) {
        WeeklyDigest weeklyDigest = findByWeekRange(weekStart, weekEnd);
        if (weeklyDigest == null) {
            weeklyDigest = new WeeklyDigest();
            weeklyDigest.setWeekStart(weekStart);
            weeklyDigest.setWeekEnd(weekEnd);
            weeklyDigest.setCreatedAt(LocalDateTime.now(DIGEST_ZONE));
        }

        weeklyDigest.setSentStatus("generating");
        weeklyDigest.setNoteCount(noteCount);
        weeklyDigest.setPromptVersion(PROMPT_VERSION);
        weeklyDigest.setErrorMessage(null);

        saveOrUpdate(weeklyDigest);
    }

    /**
     * 保存生成成功的周报结果。
     *
     * @param weekStart 周开始日期
     * @param weekEnd 周结束日期
     * @param noteCount 笔记数量
     * @param digest 周报正文
     * @return 保存后的周报记录
     */
    private WeeklyDigest saveGeneratedDigest(LocalDate weekStart, LocalDate weekEnd, int noteCount, String digest) {
        WeeklyDigest weeklyDigest = findByWeekRange(weekStart, weekEnd);
        if (weeklyDigest == null) {
            weeklyDigest = new WeeklyDigest();
            weeklyDigest.setWeekStart(weekStart);
            weeklyDigest.setWeekEnd(weekEnd);
            weeklyDigest.setCreatedAt(LocalDateTime.now(DIGEST_ZONE));
        }

        weeklyDigest.setContent(digest);
        weeklyDigest.setSentStatus("generated");
        weeklyDigest.setGeneratedAt(LocalDateTime.now(DIGEST_ZONE));
        weeklyDigest.setNoteCount(noteCount);
        weeklyDigest.setPromptVersion(PROMPT_VERSION);
        weeklyDigest.setErrorMessage(null);

        saveOrUpdate(weeklyDigest);
        return weeklyDigest;
    }


    /**
     * 保存失败状态，避免接口只返回异常但数据库没有痕迹。
     *
     * @param weekStart 周开始日期
     * @param weekEnd 周结束日期
     * @param e 生成异常
     */
    private void saveFailedDigest(LocalDate weekStart, LocalDate weekEnd, Exception e) {

        WeeklyDigest weeklyDigest = findByWeekRange(weekStart, weekEnd);

        if (weeklyDigest == null) {
            weeklyDigest = new WeeklyDigest();
            weeklyDigest.setWeekStart(weekStart);
            weeklyDigest.setWeekEnd(weekEnd);
            weeklyDigest.setCreatedAt(LocalDateTime.now(DIGEST_ZONE));
        }

        int retryCount = weeklyDigest.getRetryCount() == null ? 1 : weeklyDigest.getRetryCount() + 1;

        weeklyDigest.setSentStatus("field");
        weeklyDigest.setPromptVersion(PROMPT_VERSION);
        weeklyDigest.setErrorMessage(e.getClass().getSimpleName() + ": 周报生成失败");
        weeklyDigest.setRetryCount(retryCount);

        saveOrUpdate(weeklyDigest);

    }

    /**
     * 根据周范围查询周报记录
     *
     * @param weekStart 周开始日期
     * @param weekEnd   周结束日期
     * @return  周报记录，不存在则返回 null
     */
    private WeeklyDigest findByWeekRange(LocalDate weekStart, LocalDate weekEnd) {
        return weeklyDigestMapper.selectOne(
                Wrappers.lambdaQuery(WeeklyDigest.class)
                        .eq(WeeklyDigest::getWeekStart, weekStart)
                        .eq(WeeklyDigest::getWeekEnd, weekEnd)
        );
    }

    /**
     * 根据主键是否存在决定新增或更新
     *
     * @param weeklyDigest  周报记录
     */
    private void saveOrUpdate(WeeklyDigest weeklyDigest) {
        if (weeklyDigest.getId() == null) {
            weeklyDigestMapper.insert(weeklyDigest);
            return;
        }
        weeklyDigestMapper.updateById(weeklyDigest);
    }

    /**
     * 调用大模型生成周报
     *
     * @param prompt
     * @return
     */
    private String callDigestModel(String prompt) {
        return chatClientBuilder.build()
                .prompt()
                .system("你是一个知识管理助手，负责生成简洁、有层次的知识周报")
                .user(prompt)
                .call()
                .content();
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
            return "本周没有新增或更新笔记，请生成一句简短提醒，并给出下周复习建议。";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("请生成个人知识管理周报。\n\n");
        builder.append("时间范围: ")
               .append(weekStart)
               .append("-")
               .append(weekEnd.minusDays(1))
               .append("\n\n");

        builder.append("输出结构：\n");
        builder.append("1. 本周学习概览\n");
        builder.append("2. 高频主题\n");
        builder.append("3. 关键概念\n\n");
        builder.append("4. 值得复习的笔记\n\n");
        builder.append("5. 下周建议关注\n\n");

        builder.append("要求: \n");
        builder.append("- 不要编造笔记中不存在的内容。 \n");
        builder.append("- 每条建议尽量关联具体笔记标题。 \n");
        builder.append("- 输出使用 Markdown。\n\n");

        builder.append("笔记列表：\n");
        for (Note note : notes) {
            builder.append("- 标题: ")
                    .append(note.getTitle())
                    .append("; 来源类型: ")
                    .append(note.getSourceType())
                    .append("; 更新时间: ")
                    .append(note.getUpdatedAt())
                    .append("\n");
        }

        return builder.toString();
    }

    /**
     * 获取当前自然周周一。
     *
     * @return 当前周开始日期
     */
    private LocalDate currentWeekStart() {
        return LocalDate.now(DIGEST_ZONE)
                .with(DayOfWeek.MONDAY);
    }



}
