package com.example.community.domain.board;

import com.example.community.domain.board.dto.BoardCreateDto;
import com.example.community.domain.board.dto.BoardResponseDto;
import com.example.community.domain.board.dto.BoardUpdateDto;
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
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public List<BoardResponseDto> findAll() {
        return boardRepository.findAll().stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    public BoardResponseDto findById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + id));
        return BoardResponseDto.from(board);
    }

    @Transactional
    public BoardResponseDto create(BoardCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + dto.getUserId()));

        Board board = Board.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .user(user)
                .build();

        Board saved = boardRepository.save(board);
        return BoardResponseDto.from(saved);
    }

    @Transactional
    public BoardResponseDto update(Long id, BoardUpdateDto dto) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + id));
        board.update(dto.getTitle(), dto.getContent());
        return BoardResponseDto.from(board);
    }

    @Transactional
    public void delete(Long id) {
        if (!boardRepository.existsById(id)) {
            throw new IllegalArgumentException("Board not found: " + id);
        }
        boardRepository.deleteById(id);
    }
}
