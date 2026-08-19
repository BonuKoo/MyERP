package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestApplyRequest(
        @NotNull LeaveType leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 255) String reason
) {
}
