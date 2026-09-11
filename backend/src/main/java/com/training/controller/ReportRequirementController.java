package com.training.controller;

import com.training.common.PageResult;
import com.training.common.Result;
import com.training.dto.CustomRequirementDTO;
import com.training.entity.ReportRequirement;
import com.training.entity.EvaluationCriterion;
import com.training.service.ReportRequirementService;
import com.training.utils.AiUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/report-requirement")
@RequiredArgsConstructor
public class ReportRequirementController {

    private final ReportRequirementService requirementService;
    private final AiUtils aiUtils; // 织入大模型工具类的调用

    /**
     * 分页查询报告要求
     */
    @GetMapping("/list")
    public Result<PageResult<ReportRequirement>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long courseId) {
        return Result.success(requirementService.pageList(pageNum, pageSize, courseId));
    }

    /**
     * 获取单个报告要求
     */
    @GetMapping("/{id}")
    public Result<ReportRequirement> get(@PathVariable Long id) {
        return Result.success(requirementService.getById(id));
    }

    /**
     * 【重构升级】创建报告要求（完美包容传统三大指标与新创级联自定义评分标准）
     */
    @PostMapping
    public Result<Void> create(@RequestBody CustomRequirementDTO dto) {
        // 1. 将 DTO 传输层数据剥离还原为原生的老作业要求实体
        ReportRequirement requirement = new ReportRequirement();
        requirement.setCourseId(dto.getCourseId());
        requirement.setTitle(dto.getTitle());
        requirement.setContent(dto.getContent());
        requirement.setDeadline(dto.getDeadline());
        requirement.setStatus(dto.getStatus());
        
        // 2. 剥离提取出老师设定的自定义评分指标细则列表
        List<EvaluationCriterion> criteria = dto.getCriteria();

        // 3. 一键流转到宿主 Service，让级联事务完成安全落库
        return requirementService.createWithCriteria(requirement, criteria);
    }

    /**
     * 【硬核创新路由】上传教学大纲 PDF/Word 文件一键通过大模型智能提取评分标准
     */
    @PostMapping("/parse-pdf")
    public Result<List<EvaluationCriterion>> parseCriterionPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }
            // 1. 跨文件调用原有 FileUtils，抽取大纲正文
            String extractedText = com.training.utils.FileUtils.extractTextFromFile(file);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                return Result.error("未能成功提取文档内容，请确认文档文字非扫描版图片");
            }
            
            // 2. 将纯文本喂给大模型工具箱进行多维度的解构，返回标准指标集合给前端预览
            List<EvaluationCriterion> criteria = aiUtils.parseCriterionFromPdf(extractedText);
            return Result.success(criteria);
        } catch (Exception e) {
            return Result.error("大模型智能提取失败: " + e.getMessage());
        }
    }

    /**
     * 更新报告要求
     */
    @PutMapping
    public Result<Void> update(@RequestBody ReportRequirement requirement) {
        return requirementService.update(requirement);
    }

    /**
     * 删除报告要求
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return requirementService.delete(id);
    }

    /**
     * 更新状态
     */
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return requirementService.updateStatus(id, status);
    }

    /**
     * 获取课程的报告要求
     */
    @GetMapping("/course/{courseId}")
    public Result<?> getByCourseId(@PathVariable Long courseId) {
        return Result.success(requirementService.getByCourseId(courseId));
    }

    /**
     * 导出为Word
     */
    @GetMapping("/export/word/{id}")
    public void exportWord(@PathVariable Long id, HttpServletResponse response) {
        try {
            ReportRequirement requirement = requirementService.getById(id);
            if (requirement == null) {
                response.setStatus(404);
                return;
            }

            XWPFDocument document = new XWPFDocument();
            XWPFParagraph title = document.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setText(requirement.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.addBreak();

            if (requirement.getDeadline() != null) {
                XWPFParagraph deadline = document.createParagraph();
                XWPFRun deadlineRun = deadline.createRun();
                deadlineRun.setText("截止时间：" + requirement.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                deadlineRun.setFontSize(12);
            }

            XWPFParagraph content = document.createParagraph();
            XWPFRun contentRun = content.createRun();
            contentRun.setText(requirement.getContent() != null ? requirement.getContent() : "无详细内容");
            contentRun.setFontSize(12);

            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=" + 
                    URLEncoder.encode(requirement.getTitle() + ".docx", StandardCharsets.UTF_8));

            try (OutputStream os = response.getOutputStream()) {
                document.write(os);
            }
            document.close();
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    /**
     * 导出为PDF (生成HTML供前端打印)
     */
    @GetMapping("/export/pdf/{id}")
    public void exportPdf(@PathVariable Long id, HttpServletResponse response) {
        try {
            ReportRequirement requirement = requirementService.getById(id);
            if (requirement == null) {
                response.setStatus(404);
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            html.append("<title>").append(requirement.getTitle()).append("</title>");
            html.append("<style>body{font-family:SimSun;font-size:14px;padding:40px;}");
            html.append("h1{text-align:center;font-size:20px;}");
            html.append(".deadline{text-align:center;color:#666;margin:10px 0;}");
            html.append(".content{line-height:1.8;white-space:pre-wrap;}");
            html.append("@media print{.no-print{display:none;}}</style>");
            html.append("</head><body>");
            html.append("<h1>").append(requirement.getTitle()).append("</h1>");
            if (requirement.getDeadline() != null) {
                html.append("<p class='deadline'>截止时间：")
                    .append(requirement.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("</p>");
            }
            html.append("<div class='content'>")
                .append(requirement.getContent() != null ? requirement.getContent().replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") : "无详细内容")
                .append("</div>");
            html.append("<button class='no-print' onclick='window.print()' style='position:fixed;top:20px;right:20px;padding:10px 20px;'>打印/导出PDF</button>");
            html.append("</body></html>");

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(html.toString());
        } catch (Exception e) {
            response.setStatus(500);
        }
    }
}