package com.blog.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TimelineResponse {

    private String babyName;
    private LocalDate birthDate;
    private List<TimelineGroup> groups;

    @Data
    public static class TimelineGroup {
        private int monthAge;
        private String label;
        private String dateRange;
        private List<PhotoDTO> photos;
    }
}
