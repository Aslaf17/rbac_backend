package com.rbac.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveKitTokenResponse {
    private String token;
    private String url;
    private String roomName;
    private String identity;
}
