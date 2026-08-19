package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    private Long id;
    private Long employeeId;
    private LocalDate workDate;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
