package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MetadataResponse {
    private List<String> categories;
    private List<String> industries;
} 