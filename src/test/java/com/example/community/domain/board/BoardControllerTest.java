package com.example.community.domain.board;

import com.example.community.domain.board.dto.BoardCreateDto;
import com.example.community.domain.board.dto.BoardUpdateDto;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserRepository;
import com.example.community.util.CrudControllerTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Board 컨트롤러 테스트
 * CrudControllerTest를 상속받아 공통 CRUD 테스트를 자동으로 수행한다.
 */
class BoardControllerTest extends CrudControllerTest<BoardCreateDto, BoardUpdateDto> {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;

    @Override
    protected String getBaseUrl() {
        return "/boards";
    }

    @Override
    protected BoardCreateDto createSampleDto() {
        // 게시글 생성에는 userId가 필요하므로 먼저 유저 생성
        if (testUserId == null) {
            User user = userRepository.save(
                    User.builder().name("작성자").email("writer@example.com").build()
            );
            testUserId = user.getId();
        }
        return new BoardCreateDto("테스트 제목", "테스트 내용", testUserId);
    }

    @Override
    protected BoardUpdateDto updateSampleDto() {
        return new BoardUpdateDto("수정된 제목", "수정된 내용");
    }

    @Override
    protected Long setupTestData() {
        User user = userRepository.save(
                User.builder().name("테스트유저").email("boardtest@example.com").build()
        );
        testUserId = user.getId();

        Board board = Board.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다")
                .user(user)
                .build();
        return boardRepository.save(board).getId();
    }

    // 필요시 Board 전용 추가 테스트 작성 가능
    // @Test
    // void 게시글_검색() { ... }
}
