package com.rbac.model.assignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRef {
    private String fileName;
    private String fileUrl;
    private long fileSize;
}