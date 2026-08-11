package com.rbac.dto.admin;

import com.rbac.dto.classroom.ParticipantResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSessionWatchResponse {

    private String sessionId;
    private String token;
    private String url;
    private String roomName;
    private String identity;

    private AdminLiveSessionResponse session;
    private List<ParticipantResponse> participants;
}