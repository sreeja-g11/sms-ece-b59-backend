package com.miniclub.controller;

import com.miniclub.dto.Dashboard;
import com.miniclub.dto.MyActivity;
import com.miniclub.dto.StudentView;
import com.miniclub.model.User;
import com.miniclub.service.DashboardService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal User user) {
        return dashboardService.dashboard(user);
    }

    @GetMapping("/me/activity")
    @PreAuthorize("hasRole('STUDENT')")
    public MyActivity activity(@AuthenticationPrincipal User user) {
        return dashboardService.activity(user);
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentView> students() {
        return dashboardService.students();
    }
}
