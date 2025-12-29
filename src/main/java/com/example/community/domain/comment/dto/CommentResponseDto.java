package com.example.community.domain.comment.dto;

import com.example.community.domain.comment.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentResponseDto {

    private Long id;
    private String content;
    private Long userId;
    private String userName;
    private Long boardId;
    private LocalDateTime createdAt;

    public static CommentResponseDto from(Comment comment) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getId(),
                comment.getUser().getName(),
                comment.getBoard().getId(),
                comment.getCreatedAt()
        );
    }
}
