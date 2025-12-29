package com.example.community.domain.user;

import com.example.community.domain.user.dto.UserCreateDto;
import com.example.community.domain.user.dto.UserUpdateDto;
import com.example.community.util.CrudControllerTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User 컨트롤러 테스트
 * CrudControllerTest를 상속받아 공통 CRUD 테스트를 자동으로 수행한다.
 */
class UserControllerTest extends CrudControllerTest<UserCreateDto, UserUpdateDto> {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected String getBaseUrl() {
        return "/users";
    }

    @Override
    protected UserCreateDto createSampleDto() {
        return new UserCreateDto("홍길동", "hong@example.com");
    }

    @Override
    protected UserUpdateDto updateSampleDto() {
        return new UserUpdateDto("김철수", "kim@example.com");
    }

    @Override
    protected Long setupTestData() {
        User user = User.builder()
                .name("테스트유저")
                .email("test@example.com")
                .build();
        return userRepository.save(user).getId();
    }

    // 필요시 User 전용 추가 테스트 작성 가능
    // @Test
    // void 이메일_중복_체크() { ... }
}
