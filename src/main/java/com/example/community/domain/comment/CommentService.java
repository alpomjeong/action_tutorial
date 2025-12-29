package com.example.community.domain.comment;

import com.example.community.domain.board.Board;
import com.example.community.domain.board.BoardRepository;
import com.example.community.domain.comment.dto.CommentCreateDto;
import com.example.community.domain.comment.dto.CommentResponseDto;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    public List<CommentResponseDto> findAll() {
        return commentRepository.findAll().stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    public CommentResponseDto findById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
        return CommentResponseDto.from(comment);
    }

    @Transactional
    public CommentResponseDto create(CommentCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + dto.getUserId()));
        Board board = boardRepository.findById(dto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + dto.getBoardId()));

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .user(user)
                .board(board)
                .build();

        Comment saved = commentRepository.save(comment);
        return CommentResponseDto.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new IllegalArgumentException("Comment not found: " + id);
        }
        commentRepository.deleteById(id);
    }
}
