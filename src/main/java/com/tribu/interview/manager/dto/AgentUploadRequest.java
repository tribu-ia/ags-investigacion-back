package com.tribu.interview.manager.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentUploadRequest {
    private List<AgentDataWrapper> data;
}