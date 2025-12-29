package com.example.community.domain.user.dto;

import com.example.community.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {

    private String name;
    private String email;

    public User toEntity() {
        return User.builder()
                .name(name)
                .email(email)
                .build();
    }
}
