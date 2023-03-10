package com.freeboard02.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freeboard02.domain.user.UserEntity;
import com.freeboard02.domain.user.UserMapper;
import com.freeboard02.domain.user.enums.UserExceptionType;
import com.freeboard02.util.exception.FreeBoardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/applicationContext.xml", "file:src/main/webapp/WEB-INF/dispatcher-servlet.xml"})
@Transactional
@WebAppConfiguration
public class UserApiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserMapper userMapper;

    private MockMvc mvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void initMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    private String randomId() {
        String id = "";
        for (int i = 0; i < 10; i++) {
            double dValue = Math.random();
            if (i % 2 == 0) {
                id += (char) ((dValue * 26) + 65);   // ?????????
                continue;
            }
            id += (char) ((dValue * 26) + 97); // ?????????
        }
        return id;
    }

    @Test
    @DisplayName("????????? ???????????? ?????? ????????? ????????? ????????? ????????????.")
    public void joinTest1() throws Exception {
        UserForm userForm = UserForm.builder().accountId(randomId()).password("password").build();
        mvc.perform(post("/api/users")
                .content(objectMapper.writeValueAsString(userForm))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("????????? ???????????? ?????? ????????? ????????? ????????? ????????????.")
    public void joinTest2() throws Exception {
        UserEntity userEntity = userMapper.findAll().get(0);
        UserForm userForm = UserForm.builder().accountId(userEntity.getAccountId()).password("password").build();
        mvc.perform(post("/api/users")
                .content(objectMapper.writeValueAsString(userForm))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(result -> assertEquals(result.getResolvedException().getClass().getCanonicalName(), FreeBoardException.class.getCanonicalName()))
                .andExpect(result -> assertEquals(result.getResolvedException().getMessage(), UserExceptionType.DUPLICATED_USER.getErrorMessage()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("??????????????? ???????????? ???????????? ???????????????, ????????? ????????????.")
    public void loginTest1() throws Exception {
        UserEntity userEntity = userMapper.findAll().get(0);
        UserForm userForm = UserForm.builder().accountId(userEntity.getAccountId()).password(userEntity.getPassword()).build();

        mvc.perform(post("/api/users?type=LOGIN")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(userForm)))
                .andExpect(request().sessionAttribute("USER", notNullValue()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("??????????????? ???????????? ???????????? ????????? ????????? ????????????.")
    public void loginTest2() throws Exception {
        UserEntity userEntity = userMapper.findAll().get(0);
        UserForm userForm = UserForm.builder().accountId(userEntity.getAccountId()).password(userEntity.getPassword() + "wrongPass").build();

        mvc.perform(post("/api/users?type=LOGIN")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(userForm)))
                .andExpect(request().sessionAttribute("USER", nullValue()))
                .andExpect(result -> assertEquals(result.getResolvedException().getClass().getCanonicalName(), FreeBoardException.class.getCanonicalName()))
                .andExpect(result -> assertEquals(result.getResolvedException().getMessage(), UserExceptionType.WRONG_PASSWORD.getErrorMessage()))
                .andExpect(status().isOk());
    }


}

