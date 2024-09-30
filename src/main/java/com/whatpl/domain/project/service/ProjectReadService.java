package com.whatpl.domain.project.service;

import com.whatpl.domain.member.domain.Member;
import com.whatpl.domain.project.domain.Project;
import com.whatpl.domain.project.dto.*;
import com.whatpl.domain.project.mapper.ProjectMapper;
import com.whatpl.domain.project.repository.like.ProjectLikeRepository;
import com.whatpl.domain.project.repository.project.ProjectRepository;
import com.whatpl.global.exception.BizException;
import com.whatpl.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ProjectReadService {

    private final ProjectRepository projectRepository;
    private final ProjectReadAsyncService projectReadAsyncService;
    private final ProjectLikeRepository projectLikeRepository;

    @Transactional
    public ProjectReadResponse readProject(final long projectId, final long memberId) {
        Project project = projectRepository.findProjectWithParticipantsById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND_PROJECT));
        boolean myLike = projectLikeRepository.existsByProjectIdAndMemberId(projectId, memberId);

        long likes = projectLikeRepository.countByProject(project);
        project.increaseViews();

        return ProjectMapper.toProjectReadResponse(project, likes, myLike);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "projectList",
            key = "T(com.whatpl.domain.project.util.ProjectCacheUtils).buildListCacheKey(#searchCondition)",
            condition = "T(com.whatpl.domain.project.util.ProjectCacheUtils).isCacheable(#pageable, #searchCondition)")
    public List<ProjectInfo> searchProjectList(Pageable pageable, ProjectSearchCondition searchCondition) {
        List<ProjectInfo> projectInfos = projectRepository.findProjectInfos(pageable, searchCondition);
        CompletableFuture.allOf(
                projectReadAsyncService.mergeSkills(projectInfos),
                projectReadAsyncService.mergeRemainedJobs(projectInfos),
                projectReadAsyncService.mergeLikes(projectInfos),
                projectReadAsyncService.mergeComments(projectInfos)
        ).join();

        return projectInfos;
    }

    @Transactional(readOnly = true)
    public List<ParticipatedProject> readParticipatedProject(Member member) {
        ArrayList<ParticipatedProject> result = new ArrayList<>();
        projectRepository.findParticipatedProjects(member).forEach(project ->
                result.add(ProjectMapper.toParticipatedProject(member, project))
        );
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectInfo> readRecruitedProjects(Member member) {
        ProjectSearchCondition searchCondition = ProjectSearchCondition.builder()
                .memberId(member.getId())
                .recruiterId(member.getId())
                .build();
        List<ProjectInfo> projectInfos = projectRepository.findProjectInfos(PageRequest.of(0, 30), searchCondition);
        CompletableFuture.allOf(
                projectReadAsyncService.mergeSkills(projectInfos),
                projectReadAsyncService.mergeRemainedJobs(projectInfos),
                projectReadAsyncService.mergeLikes(projectInfos),
                projectReadAsyncService.mergeComments(projectInfos)
        ).join();
        return projectInfos;
    }
}
