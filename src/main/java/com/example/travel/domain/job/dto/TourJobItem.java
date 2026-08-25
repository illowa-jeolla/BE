package com.example.travel.domain.job.dto;

public record TourJobItem(
        String employmentInfoNo,
        String companyName,
        String companyLogoUrl,
        String title,
        String upperRecruitJobCode,
        String middleRecruitJobCode,
        String lowerRecruitJobCode,
        String workplaceAddress,
        String regionCode,
        String districtCode,
        String salaryTypeCode,
        String wageAmount,
        String regularEmployment,
        String receiptDeadlineDate,
        String careerDivisionCode,
        String educationCode,
        String employmentTypeCode1,
        String employmentTypeCode2,
        String modifiedAt,
        String registeredAt
) {
}
