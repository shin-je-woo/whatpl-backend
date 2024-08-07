package com.whatpl.domain.project.mapper;

import com.whatpl.domain.project.domain.ProjectComment;
import com.whatpl.domain.project.dto.ProjectCommentDto;
import com.whatpl.domain.project.dto.ProjectSubCommentDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectCommentMapper {

    public static ProjectCommentDto toProjectCommentDto(ProjectComment projectComment) {
        // 하위 댓글
        List<ProjectSubCommentDto> subComments = projectComment.getChildren().stream()
                .map(comment -> ProjectSubCommentDto.builder()
                        .commentId(comment.getId())
                        .writerId(comment.getWriter().getId())
                        .writerNickname(comment.getWriter().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .isModified(comment.getModified())
                        .isDeleted(comment.getDeleted())
                        .parentId(comment.getParent().getId())
                        .build()
                ).toList();

        return ProjectCommentDto.builder()
                .commentId(projectComment.getId())
                .writerId(projectComment.getWriter().getId())
                .writerNickname(projectComment.getWriter().getNickname())
                .content(projectComment.getContent())
                .createdAt(projectComment.getCreatedAt())
                .isModified(projectComment.getModified())
                .isDeleted(projectComment.getDeleted())
                .subComments(subComments)
                .build();
    }
}
