package com.rbac.dto.assignment;

import com.rbac.model.assignment.AttachmentRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private String fileName;
    private String fileUrl;
    private long fileSize;

    public AttachmentRef toEntity() {
        return new AttachmentRef(fileName, fileUrl, fileSize);
    }

    public static AttachmentDto fromEntity(AttachmentRef ref) {
        return AttachmentDto.builder()
                .fileName(ref.getFileName())
                .fileUrl(ref.getFileUrl())
                .fileSize(ref.getFileSize())
                .build();
    }
}