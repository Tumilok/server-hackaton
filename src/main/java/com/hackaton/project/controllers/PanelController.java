package com.hackaton.project.controllers;

import com.hackaton.project.models.Project;
import com.hackaton.project.models.Task;
import com.hackaton.project.models.User;
import com.hackaton.project.payload.request.AddProjectRequest;
import com.hackaton.project.payload.request.TaskRequest;
import com.hackaton.project.payload.response.AddProjectResponse;
import com.hackaton.project.payload.response.MessageResponse;
import com.hackaton.project.payload.response.ProjectResponse;
import com.hackaton.project.payload.response.TasksResponse;
import com.hackaton.project.repository.ProjectRepository;
import com.hackaton.project.repository.TaskRepository;
import com.hackaton.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RestController
@RequestMapping("api/panel")
public class PanelController {
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/tasks/{projectId}")
    public List<TasksResponse> getProjectTasks(@PathVariable String projectId) {
        Optional<Project> project = projectRepository.findById(Long.valueOf(projectId));
        if (project.isPresent()) {
            List<TasksResponse> taskResponses = new ArrayList<>();
            List<Task> tasks= taskRepository.findByProject(project.get());
            for(Task t:tasks){
                taskResponses.add(new TasksResponse(t));
            }
            return taskResponses;
        } else {
            return null;
        }
    }

    @GetMapping("/projects/{userId}")
    public List<ProjectResponse> getUserProjects(@PathVariable Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            List<Project> projects = projectRepository.findAll();
            List<ProjectResponse> returnList = new ArrayList<>();
            for (Project p: projects) {
                if (p.getUsers().contains(user.get())) {
                    returnList.add(new ProjectResponse(p.getId(), p.getTitle(), p.getDescription(), p.getDeadline()));
                }
            }
            if (returnList.size() > 0) {
                return returnList;
            }
        }
        return null;
    }

    @PostMapping("/project")
    public ResponseEntity<?> addProject(@Valid @RequestBody AddProjectRequest addProjectRequest) {
        // Create new project
        Project project = new Project(
                addProjectRequest.getTitle(),
                addProjectRequest.getDescription(),
                LocalDateTime.parse(
                        addProjectRequest.getDeadline(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))        // "2016-03-04 11:30";
        );
        projectRepository.save(project);

        Set<String> strUsers = addProjectRequest.getUsers();
        Set<User> users = new HashSet<>();

        if (strUsers == null) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Invalid number of users."));
        } else {
            strUsers.forEach(userStr -> {
                User user = userRepository.findByUsername(userStr)
                        .orElseThrow(() -> new RuntimeException("Error: User is not found."));
                user.getProjects().add(project);
                users.add(user);
            });
        }
        project.setUsers(users);
        userRepository.saveAll(users);

        Set<TaskRequest> taskRequests = addProjectRequest.getTasksRequests();

        if (taskRequests != null) {
            Set<Task> tasks = new HashSet<>();
            taskRequests.forEach(tr -> tasks.add(new Task(
                    tr.getTaskName(),
                    tr.getDescription(),
                    LocalDateTime.parse(
                            tr.getDeadline(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")        // "2016-03-04 11:30";
                    ),
                    Integer.parseInt(tr.getTimeLimit())
            )));
            project.setTasks(tasks);
            taskRepository.saveAll(tasks);
        }

        projectRepository.save(project);

        return ResponseEntity.ok(new AddProjectResponse(project));
    }

    @PostMapping("/project/addUser/{projectId}/{userId}")
    public ResponseEntity<?>  addUserToProject(@PathVariable String projectId, @PathVariable String userId) {
        Optional<User> user = userRepository.findById(Long.valueOf(userId));
        if (user.isPresent()) {
            Optional<Project> project = projectRepository.findById(Long.valueOf(projectId));
            if(project.isPresent()) {
                Set<User> users = project.get().getUsers();
                users.add(user.get());
                project.get().setUsers(users);
                return ResponseEntity.ok(new MessageResponse("Member added successfully!"));
            }
            else{
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid project data."));
            }
        }
        else{
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid user data."));
        }
    }
}
