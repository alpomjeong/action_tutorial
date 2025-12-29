package com.example.community.domain.comment;

import com.example.community.domain.board.Board;
import com.example.community.domain.board.BoardRepository;
import com.example.community.domain.comment.dto.CommentCreateDto;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserRepository;
import com.example.community.util.CrudControllerTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Comment 컨트롤러 테스트
 * CrudControllerTest를 상속받아 공통 CRUD 테스트를 자동으로 수행한다.
 * Comment는 수정 기능이 없으므로 updateSampleDto는 null 반환.
 */
class CommentControllerTest extends CrudControllerTest<CommentCreateDto, Void> {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    private Long testUserId;
    private Long testBoardId;

    @Override
    protected String getBaseUrl() {
        return "/comments";
    }

    @Override
    protected CommentCreateDto createSampleDto() {
        // 댓글 생성에는 userId와 boardId가 필요
        if (testUserId == null || testBoardId == null) {
            setupTestData();
        }
        return new CommentCreateDto("테스트 댓글", testUserId, testBoardId);
    }

    @Override
    protected Void updateSampleDto() {
        // Comment는 수정 기능 없음
        return null;
    }

    @Override
    protected Long setupTestData() {
        User user = userRepository.save(
                User.builder().name("댓글작성자").email("commenter@example.com").build()
        );
        testUserId = user.getId();

        Board board = boardRepository.save(
                Board.builder().title("게시글").content("내용").user(user).build()
        );
        testBoardId = board.getId();

        Comment comment = Comment.builder()
                .content("테스트 댓글입니다")
                .user(user)
                .board(board)
                .build();
        return commentRepository.save(comment).getId();
    }
}
