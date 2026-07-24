package com.rbac.dto.classroom;

import lombok.Data;

@Data
public class UpdatePermissionsRequest {
    private Boolean canSpeak;
    private Boolean canChat;
    private Boolean canScreenShare;
}