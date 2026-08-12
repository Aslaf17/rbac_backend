package com.rbac.service.course;

import com.rbac.dto.course.*;
import com.rbac.exception.course.DuplicateCourseException;
import com.rbac.exception.course.InvalidRequestException;
import com.rbac.exception.course.ResourceNotFoundException;
import com.rbac.exception.course.UnauthorizedActionException;
import com.rbac.model.course.Course;
import com.rbac.model.course.CourseStatus;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.CourseRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public interface CourseService {

    CourseResponse createCourse(CreateCourseRequest request, AuthenticatedUser admin);

    CourseResponse updateCourse(String courseId, UpdateCourseRequest request, AuthenticatedUser admin);

    CourseResponse getCourse(String courseId);

    List<CourseResponse> getAllCourses(CourseStatus status);

    void deleteCourse(String courseId, AuthenticatedUser admin);

    CourseResponse assignTrainer(String courseId, AssignTrainerRequest request, AuthenticatedUser admin);

    CourseResponse archiveCourse(String courseId, AuthenticatedUser admin);

    CourseStatisticsResponse getStatistics();
}

@Slf4j
@Service
@RequiredArgsConstructor
class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    public CourseResponse createCourse(CreateCourseRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "create a course");

        String courseId = StringUtils.hasText(request.getCourseId())
                ? request.getCourseId().trim()
                : generateCourseId(request.getTitle());

        Course course = new Course();
        course.setId(courseId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setDurationWeeks(request.getDurationWeeks());
        course.setStatus(CourseStatus.ACTIVE);
        course.setCreatedBy(admin.getUserId());
        course.setCreatedByName(admin.getUserName());
        course.setCreatedAt(Instant.now());
        course.setUpdatedAt(Instant.now());

        Course saved;
        try {
            saved = courseRepository.insert(course);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateCourseException("A course with id '" + courseId + "' already exists");
        }

        log.info("Course {} created by {}", saved.getId(), admin.getUserId());
        return CourseResponse.fromEntity(saved);
    }

    @Override
    public CourseResponse updateCourse(String courseId, UpdateCourseRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "update a course");
        Course course = findOrThrow(courseId);

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setDurationWeeks(request.getDurationWeeks());
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        course.setUpdatedAt(Instant.now());

        Course saved = courseRepository.save(course);
        log.info("Course {} updated by {}", courseId, admin.getUserId());
        return CourseResponse.fromEntity(saved);
    }

    @Override
    public CourseResponse getCourse(String courseId) {
        return CourseResponse.fromEntity(findOrThrow(courseId));
    }

    @Override
    public List<CourseResponse> getAllCourses(CourseStatus status) {
        List<Course> courses = status != null ? courseRepository.findByStatus(status) : courseRepository.findAll();
        return courses.stream().map(CourseResponse::fromEntity).toList();
    }

    @Override
    public void deleteCourse(String courseId, AuthenticatedUser admin) {
        requireAdmin(admin, "delete a course");
        Course course = findOrThrow(courseId);

        if (course.getStatus() == CourseStatus.ACTIVE) {
            throw new InvalidRequestException(
                    "Cannot delete an ACTIVE course '" + courseId + "'. Archive it first, then delete.");
        }

        courseRepository.deleteById(courseId);
        log.info("Course {} deleted by {}", courseId, admin.getUserId());
    }

    @Override
    public CourseResponse assignTrainer(String courseId, AssignTrainerRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "assign a trainer to a course");
        Course course = findOrThrow(courseId);

        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new InvalidRequestException("No such user: " + request.getTrainerId()));
        if (trainer.getRole() != Role.TEACHER) {
            throw new InvalidRequestException("User '" + trainer.getUsername() + "' is not a trainer (TEACHER role)");
        }

        course.setTrainerId(trainer.getId());
        course.setTrainerName(trainer.getUsername());
        course.setUpdatedAt(Instant.now());

        Course saved = courseRepository.save(course);
        log.info("Trainer {} assigned to course {} by {}", trainer.getId(), courseId, admin.getUserId());
        return CourseResponse.fromEntity(saved);
    }

    @Override
    public CourseResponse archiveCourse(String courseId, AuthenticatedUser admin) {
        requireAdmin(admin, "archive a course");
        Course course = findOrThrow(courseId);

        course.setStatus(CourseStatus.ARCHIVED);
        course.setUpdatedAt(Instant.now());

        Course saved = courseRepository.save(course);
        log.info("Course {} archived by {}", courseId, admin.getUserId());
        return CourseResponse.fromEntity(saved);
    }

    @Override
    public CourseStatisticsResponse getStatistics() {
        return CourseStatisticsResponse.builder()
                .totalCourses(courseRepository.count())
                .activeCourses(courseRepository.countByStatus(CourseStatus.ACTIVE))
                .inactiveCourses(courseRepository.countByStatus(CourseStatus.INACTIVE))
                .archivedCourses(courseRepository.countByStatus(CourseStatus.ARCHIVED))
                .coursesWithTrainer(courseRepository.countByTrainerIdIsNotNull())
                .coursesWithoutTrainer(courseRepository.countByTrainerIdIsNull())
                .build();
    }

    private String generateCourseId(String title) {
        String base = title == null ? "COURSE" : title.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "COURSE";
        String candidate = base;
        int suffix = 1;
        while (courseRepository.existsById(candidate)) {
            candidate = base + "-" + (++suffix);
        }
        return candidate;
    }

    private Course findOrThrow(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private void requireAdmin(AuthenticatedUser user, String action) {
        if (!user.hasRole(Role.ADMIN) && !user.hasRole(Role.TEACHER)) {
            throw new UnauthorizedActionException("Only an admin can " + action);
        }
    }
}