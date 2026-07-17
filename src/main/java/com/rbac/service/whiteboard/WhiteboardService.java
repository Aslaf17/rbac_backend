package com.rbac.service.whiteboard;

import com.rbac.dto.whiteboard.SaveWhiteboardRequest;
import com.rbac.dto.whiteboard.UpdateWhiteboardRequest;
import com.rbac.dto.whiteboard.WhiteboardResponse;
import com.rbac.exception.chat.InvalidRequestException;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.whiteboard.WhiteboardElement;
import com.rbac.repository.WhiteboardRepository;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.service.chat.SessionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

public interface WhiteboardService {

    WhiteboardResponse saveDrawing(SaveWhiteboardRequest request, AuthenticatedUser user);

    List<WhiteboardResponse> getSessionWhiteboard(String sessionId, AuthenticatedUser user);

    WhiteboardResponse updateDrawing(String sessionId, UpdateWhiteboardRequest request, AuthenticatedUser user);

    void clearWhiteboard(String sessionId, AuthenticatedUser user);
}

@Slf4j
@Service
@RequiredArgsConstructor
class WhiteboardServiceImpl implements WhiteboardService {

    private static final double MAX_STROKE_WIDTH = 200.0;

    private final WhiteboardRepository whiteboardRepository;
    private final SessionValidationService sessionValidationService;

    @Override
    public WhiteboardResponse saveDrawing(SaveWhiteboardRequest request, AuthenticatedUser user) {
        validateSavePayload(request);
        validateSessionAndMembership(request.getSessionId(), user.getUserId());

        WhiteboardElement element = WhiteboardElement.builder()
                .sessionId(request.getSessionId())
                .userId(user.getUserId())
                .drawingData(request.getDrawingData())
                .toolType(request.getToolType())
                .color(request.getColor())
                .strokeWidth(request.getStrokeWidth())
                .timestamp(LocalDateTime.now())
                .deleted(false)
                .build();

        WhiteboardElement saved = whiteboardRepository.save(element);
        log.info("Whiteboard element {} saved for session {} by user {}", saved.getId(), saved.getSessionId(), user.getUserId());

        return WhiteboardResponse.fromEntity(saved);
    }

    @Override
    public List<WhiteboardResponse> getSessionWhiteboard(String sessionId, AuthenticatedUser user) {
        if (!StringUtils.hasText(sessionId)) {
            throw new InvalidRequestException("sessionId is required");
        }
        validateSessionAndMembership(sessionId, user.getUserId());

        return whiteboardRepository.findBySessionIdAndDeletedFalseOrderByTimestampAsc(sessionId)
                .stream()
                .map(WhiteboardResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public WhiteboardResponse updateDrawing(String sessionId, UpdateWhiteboardRequest request, AuthenticatedUser user) {
        if (!StringUtils.hasText(sessionId)) {
            throw new InvalidRequestException("sessionId is required");
        }
        if (!StringUtils.hasText(request.getWhiteboardId())) {
            throw new InvalidRequestException("whiteboardId is required");
        }
        if (request.getDrawingData() == null && request.getToolType() == null
                && request.getColor() == null && request.getStrokeWidth() == null) {
            throw new InvalidRequestException("At least one field must be provided to update");
        }
        if (request.getStrokeWidth() != null && (request.getStrokeWidth() <= 0 || request.getStrokeWidth() > MAX_STROKE_WIDTH)) {
            throw new InvalidRequestException("strokeWidth must be between 0 and " + MAX_STROKE_WIDTH);
        }
        if (request.getDrawingData() != null && request.getDrawingData().isEmpty()) {
            throw new InvalidRequestException("drawingData cannot be empty");
        }

        validateSessionAndMembership(sessionId, user.getUserId());

        WhiteboardElement element = whiteboardRepository.findByIdAndDeletedFalse(request.getWhiteboardId())
                .orElseThrow(() -> new ResourceNotFoundException("Whiteboard element not found: " + request.getWhiteboardId()));

        if (!element.getSessionId().equals(sessionId)) {
            throw new InvalidRequestException("Whiteboard element does not belong to session: " + sessionId);
        }

        boolean isOwner = element.getUserId().equals(user.getUserId());
        if (!isOwner && !user.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the creator, trainer, or admin can update this element");
        }

        if (request.getDrawingData() != null) {
            element.setDrawingData(request.getDrawingData());
        }
        if (request.getToolType() != null) {
            element.setToolType(request.getToolType());
        }
        if (request.getColor() != null) {
            element.setColor(request.getColor());
        }
        if (request.getStrokeWidth() != null) {
            element.setStrokeWidth(request.getStrokeWidth());
        }
        element.setUpdatedAt(LocalDateTime.now());

        WhiteboardElement saved = whiteboardRepository.save(element);
        log.info("Whiteboard element {} updated for session {} by user {}", saved.getId(), sessionId, user.getUserId());

        return WhiteboardResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void clearWhiteboard(String sessionId, AuthenticatedUser user) {
        if (!StringUtils.hasText(sessionId)) {
            throw new InvalidRequestException("sessionId is required");
        }
        if (!user.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the trainer or admin can clear the whiteboard");
        }
        if (!sessionValidationService.sessionExists(sessionId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        if (!sessionValidationService.isUserPartOfSession(sessionId, user.getUserId())) {
            throw new UnauthorizedActionException("User is not a participant of this session");
        }

        List<WhiteboardElement> elements = whiteboardRepository.findBySessionIdAndDeletedFalse(sessionId);
        if (CollectionUtils.isEmpty(elements)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        elements.forEach(element -> {
            element.setDeleted(true);
            element.setDeletedBy(user.getUserId());
            element.setDeletedAt(now);
        });
        whiteboardRepository.saveAll(elements);

        log.info("Whiteboard cleared for session {} by {} ({} elements removed)", sessionId, user.getUserId(), elements.size());
    }

    private void validateSavePayload(SaveWhiteboardRequest request) {
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new InvalidRequestException("sessionId is required");
        }
        if (CollectionUtils.isEmpty(request.getDrawingData())) {
            throw new InvalidRequestException("drawingData cannot be empty");
        }
        if (request.getToolType() == null) {
            throw new InvalidRequestException("toolType is required");
        }
        if (!StringUtils.hasText(request.getColor())) {
            throw new InvalidRequestException("color is required");
        }
        if (request.getStrokeWidth() == null || request.getStrokeWidth() <= 0 || request.getStrokeWidth() > MAX_STROKE_WIDTH) {
            throw new InvalidRequestException("strokeWidth must be between 0 and " + MAX_STROKE_WIDTH);
        }
    }

    private void validateSessionAndMembership(String sessionId, String userId) {
        if (!sessionValidationService.sessionExists(sessionId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        if (!sessionValidationService.isUserPartOfSession(sessionId, userId)) {
            throw new UnauthorizedActionException("User is not a participant of this session");
        }
    }
}
