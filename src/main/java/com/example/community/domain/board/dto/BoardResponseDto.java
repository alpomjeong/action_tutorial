package com.example.community.domain.board.dto;

import com.example.community.domain.board.Board;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardResponseDto {

    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;

    public static BoardResponseDto from(Board board) {
        return new BoardResponseDto(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getUser().getId(),
                board.getUser().getName(),
                board.getCreatedAt()
        );
    }
}
