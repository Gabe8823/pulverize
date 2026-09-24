package com.run.module.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserDTO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private Double heightCm;
    private Double weightKg;
    private Integer maxHeartRate;
    private Integer restHeartRate;
}
