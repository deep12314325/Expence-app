package com.spreetail.expenses.controller;

import com.spreetail.expenses.dto.LoginRequest;
import com.spreetail.expenses.dto.LoginResponse;
import com.spreetail.expenses.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final GroupService groupService;

    public AuthController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        var user = groupService.userByEmail(request.email());
        if (!user.getPassword().equals(request.password())) {
            throw new BadLoginException();
        }
        return new LoginResponse(user.getId(), user.getName(), user.getEmail());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    private static class BadLoginException extends RuntimeException {
    }
}
