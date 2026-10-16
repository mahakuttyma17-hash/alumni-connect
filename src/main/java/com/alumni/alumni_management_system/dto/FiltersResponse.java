package com.alumni.alumni_management_system.dto;

import java.util.List;

public record FiltersResponse(List<String> departments, List<Integer> graduationYears) {
}