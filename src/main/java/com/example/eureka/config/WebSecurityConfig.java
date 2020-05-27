package com.example.eureka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * @Classname WebSecurityConfig
 * @Description TODO
 * @Date 2019/9/1 20:19
 * @Created by gumei
 * @Author: lepua
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Value("${security.enabled:true}")
    private boolean securityEnabled;
    
    @Override
    public void configure(WebSecurity web) throws Exception {
        if (!securityEnabled)
            web.ignoring().antMatchers("/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (securityEnabled)
            http.csrf().disable();
            http.authorizeRequests().anyRequest().authenticated().and().httpBasic();
    }
}
