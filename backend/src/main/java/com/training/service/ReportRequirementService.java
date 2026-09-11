package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.PageResult;
import com.training.common.Result;
import com.training.entity.Course;
import com.training.entity.Notification;
import com.training.entity.ReportRequirement;
import com.training.entity.StudentCourse;
import com.training.entity.EvaluationCriterion;
import com.training.mapper.CourseMapper;
import com.training.mapper.NotificationMapper;
import com.training.mapper.ReportRequirementMapper;
import com.training.mapper.StudentCourseMapper;
import com.training.mapper.EvaluationCriterionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportRequirementService extends ServiceImpl<ReportRequirementMapper, ReportRequirement> {

    private final ReportRequirementMapper requirementMapper;
    private final NotificationMapper notificationMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final CourseMapper courseMapper;
    private final EvaluationCriterionMapper criterionMapper; // 寄宿新表的持久化操作

    /**
     * 分页查询报告要求（扩展：支持级联查询出各作业是否属于自定义标准）
     */
    public PageResult<ReportRequirement> pageList(Integer pageNum, Integer pageSize, Long courseId) {
        Page<ReportRequirement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportRequirement> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(ReportRequirement::getCourseId, courseId);
        }
        wrapper.orderByDesc(ReportRequirement::getCreateTime);
        IPage<ReportRequirement> result = requirementMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /**
     * 获取单个报告要求
     */
    public ReportRequirement getById(Long id) {
        return requirementMapper.selectById(id);
    }

    /**
     * 【重构级联发布作业】创建报告要求、关联存储自定义指标细则并发送通知
     */
    @Transactional
    public Result<Void> createWithCriteria(ReportRequirement requirement, List<EvaluationCriterion> criteria) {
        requirement.setStatus(1);
        requirement.setCreateTime(LocalDateTime.now());
        // 如果传入了自定义标准，打上标记口袋 1
        if (criteria != null && !criteria.isEmpty()) {
            requirement.setHasCustomCriterion(1);
        } else {
            requirement.setHasCustomCriterion(0);
        }
        requirementMapper.insert(requirement);

        // 如果启用了自定义标准，批量级联落库至指标明细表中
        if (requirement.getHasCustomCriterion() == 1 && criteria != null) {
            for (EvaluationCriterion criterion : criteria) {
                criterion.setRequirementId(requirement.getId()); // 绑定刚刚生成的作业ID
                criterion.setCreateTime(LocalDateTime.now());
                criterionMapper.insert(criterion);
            }
        }

        // 获取课程信息
        Course course = courseMapper.selectById(requirement.getCourseId());
        if (course == null) {
            return Result.error("课程不存在");
        }

        // 获取选课学生列表
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getCourseId, requirement.getCourseId());
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(wrapper);

        // 给每个选课学生发送通知
        for (StudentCourse sc : studentCourses) {
            Notification notification = new Notification();
            notification.setUserId(sc.getStudentId());
            notification.setType(1); // 报告通知
            notification.setTitle("新报告要求发布");
            notification.setContent(String.format("课程《%s》发布了新的报告要求：%s，截止时间：%s",
                    course.getCourseName(),
                    requirement.getTitle(),
                    requirement.getDeadline() != null ? requirement.getDeadline().toString() : "未设置"));
            notification.setRelatedId(requirement.getId());
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
        }

        return Result.success();
    }

    /**
     * 更新报告要求
     */
    public Result<Void> update(ReportRequirement requirement) {
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);
        return Result.success();
    }

    /**
     * 删除报告要求（增加：级联同步清空绑定的自定义评分标准指标，杜绝产生垃圾碎片）
     */
    @Transactional
    public Result<Void> delete(Long id) {
        // 先斩断外键关联，清空对应的动态评分标准明细
        LambdaQueryWrapper<EvaluationCriterion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationCriterion::getRequirementId, id);
        criterionMapper.delete(wrapper);

        // 再删除作业要求主体
        requirementMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 更新状态
     */
    public Result<Void> updateStatus(Long id, Integer status) {
        ReportRequirement requirement = requirementMapper.selectById(id);
        if (requirement == null) {
            return Result.error("报告要求不存在");
        }
        requirement.setStatus(status);
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);
        return Result.success();
    }

    /**
     * 获取课程的报告要求
     */
    public List<ReportRequirement> getByCourseId(Long courseId) {
        LambdaQueryWrapper<ReportRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportRequirement::getCourseId, courseId);
        wrapper.eq(ReportRequirement::getStatus, 1); // 只获取进行中的
        wrapper.orderByDesc(ReportRequirement::getCreateTime);
        return requirementMapper.selectList(wrapper);
    }

    /**
     * 获取学生未查看的报告要求
     */
    public List<ReportRequirement> getNewRequirementsForStudent(Long studentId) {
        LambdaQueryWrapper<StudentCourse> scWrapper = new LambdaQueryWrapper<>();
        scWrapper.eq(StudentCourse::getStudentId, studentId);
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(scWrapper);

        if (studentCourses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = studentCourses.stream()
                .map(StudentCourse::getCourseId)
                .toList();

        LambdaQueryWrapper<ReportRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ReportRequirement::getCourseId, courseIds);
        wrapper.eq(ReportRequirement::getStatus, 1);
        wrapper.orderByDesc(ReportRequirement::getCreateTime);

        return requirementMapper.selectList(wrapper);
    }
}