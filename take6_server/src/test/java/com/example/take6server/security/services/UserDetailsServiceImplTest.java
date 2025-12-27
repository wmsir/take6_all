package com.example.take6server.security.services;

import com.example.take6server.model.User;
import com.example.take6server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    public void testLoadUserByUsername_Success() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        user.setId(1L);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).findByWechatOpenid(anyString());
    }

    @Test
    public void testLoadUserByOpenid_Success() {
        String openid = "oTKf94hxsbzghhsbn12SJMvA3EcQ";
        User user = new User();
        user.setUsername(null);
        user.setWechatOpenid(openid);
        user.setId(2L);

        when(userRepository.findByUsername(openid)).thenReturn(Optional.empty());
        when(userRepository.findByWechatOpenid(openid)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(openid);

        assertNotNull(userDetails);
        // Ensure UserDetailsImpl uses openid as username when username is null
        assertEquals(openid, userDetails.getUsername());
        verify(userRepository, times(1)).findByUsername(openid);
        verify(userRepository, times(1)).findByWechatOpenid(openid);
    }

    @Test
    public void testLoadUser_NotFound() {
        String input = "nonexistent";

        when(userRepository.findByUsername(input)).thenReturn(Optional.empty());
        when(userRepository.findByWechatOpenid(input)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(input);
        });
    }
}
