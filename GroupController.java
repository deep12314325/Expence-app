package com.spreetail.expenses.controller;

import com.spreetail.expenses.dto.GroupSummary;
import com.spreetail.expenses.dto.MemberSummary;
import com.spreetail.expenses.service.GroupService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupSummary> groups() {
        return groupService.allGroups().stream()
                .map(g -> new GroupSummary(g.getId(), g.getName()))
                .toList();
    }

    @GetMapping("/{groupId}/members")
    public List<MemberSummary> members(@PathVariable Long groupId) {
        return groupService.members(groupId).stream()
                .map(m -> new MemberSummary(m.getUser().getId(), m.getUser().getName(), m.getJoinedOn(), m.getLeftOn()))
                .toList();
    }

    @PostMapping("/{groupId}/members")
    public MemberSummary addMember(@PathVariable Long groupId,
                                   @RequestParam String name,
                                   @RequestParam String email,
                                   @RequestParam LocalDate joinedOn,
                                   @RequestParam(required = false) LocalDate leftOn) {
        var membership = groupService.addMember(groupId, name, email, joinedOn, leftOn);
        return new MemberSummary(membership.getUser().getId(), membership.getUser().getName(),
                membership.getJoinedOn(), membership.getLeftOn());
    }
}
