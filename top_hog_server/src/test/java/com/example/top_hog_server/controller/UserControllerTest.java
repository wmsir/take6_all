package com.example.top_hog_server.controller;

import com.example.top_hog_server.payload.dto.request.UserUpdateRequest;
import com.example.top_hog_server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    public void testUpdateUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("New Nickname");
        request.setAvatarUrl("/new/avatar.png");

        Map<String, Object> updatedInfo = new HashMap<>();
        updatedInfo.put("nickname", "New Nickname");
        updatedInfo.put("avatarUrl", "/new/avatar.png");

        when(userService.updateUser(any(UserUpdateRequest.class))).thenReturn(updatedInfo);

        mockMvc.perform(post("/api/user/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"New Nickname\",\"avatarUrl\":\"/new/avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("New Nickname"));
    }
}
