package com.example.take6server.security.services;


import com.example.take6server.model.User;
import com.example.take6server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 先尝试通过 username 查找
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // 如果找不到，尝试通过 wechatOpenid 查找
                    return userRepository.findByWechatOpenid(username)
                            .orElseThrow(() -> new UsernameNotFoundException("未找到用户名或OpenID: " + username));
                });

        return UserDetailsImpl.build(user);
    }
}
