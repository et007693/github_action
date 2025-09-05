package com.example.userIntegration.controller;

import com.example.userIntegration.model.User;
import com.example.userIntegration.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    @Autowired
    private TestRestTemplate restTemplate; // HTTP 요청을 보내고 응답을 확인하는 도구

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser_success() {
        User user = new User("송창용2", "1234");

        ResponseEntity<User> response = restTemplate.postForEntity("/users", user, User.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("송창용2", response.getBody().getUsername());
    }

    @Test
    void testCreateUser_EmptyUsername() {
        User user = new User("", "1234");

        ResponseEntity<String> response = restTemplate.postForEntity("/users", user, String.class);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Username cannot be empty", response.getBody());
    }

    @Test
    void testCreateUser_duplicateUsername() {
        // 첫 번째 사용자 등록
        User user = new User("송창용", "1234");
        restTemplate.postForEntity("/users", user, User.class);

        // 같은 이름으로 두번재 등록 시도
        User user2 = new User("송창용", "1234");
        ResponseEntity<String> response = restTemplate.postForEntity("/users", user2, String.class);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Username already exists", response.getBody());
    }

    /// Read1: getAllUser
    @Test
    void testGetAllUsers_withUsers() {
        // 사용자 두 명 등록
        restTemplate.postForEntity("/users", new User("송창용", "1234"), User.class);
        restTemplate.postForEntity("/users", new User("유재석", "1234"), User.class);

        // 전체 조회
        ResponseEntity<User[]> response = restTemplate.getForEntity("/users", User[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
        assertEquals("송창용", response.getBody()[0].getUsername());
        assertEquals("유재석", response.getBody()[1].getUsername());
    }

    @Test
    void testGetAllUsers_empty() {
        // 전체 조회
        ResponseEntity<User[]> response = restTemplate.getForEntity("/users", User[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
    }

    /// Read2: getUserById
    @Test
    void testGetUserById_success() {
        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", new User("송창용", "1234"), User.class);
        Long id = createResponse.getBody().getId();

        // 전체 조회
        ResponseEntity<User> response = restTemplate.getForEntity("/users/" + id, User.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("송창용", response.getBody().getUsername());
    }
    @Test
    void testGetUserById_notFound() {
        ResponseEntity<String> response = restTemplate.getForEntity("/users/999", String.class);
        assertEquals(404, response.getStatusCode().value());
        assertEquals("User not found with id: 999", response.getBody());
    }

    /// update
    @Test
    void testUpdateUser_success() {
        // 사용자 등록
        User user = new User("송창용", "1234");
        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", user, User.class);
        Long id = createResponse.getBody().getId();

        // 사용자 수정
        User updatedUser = new User("송창용", "5678");
        HttpEntity<User> request = new HttpEntity<>(updatedUser);
        ResponseEntity<User> response = restTemplate.exchange("/users/" + id, HttpMethod.PUT, request, User.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("송창용", response.getBody().getUsername());
        assertEquals("5678", response.getBody().getPassword());

    }

    @Test
    void testUpdateUser_notFound() { // 업데이트 하려고 하는데 해당 id의 유저가 없을 때
        // todo - 업데이트 할 유저 생성
        // todo - 없데이트 api 호출: id 999로 호출
        // todo - 400 응답 검증
        // todo "User not found width id: 999" 검증
        User user = new User("송창용", "1234");
        HttpEntity<User> request = new HttpEntity<>(user);
        ResponseEntity<User> response = restTemplate.exchange("/users/999", HttpMethod.PUT, request, User.class);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found with id: 999", response.getBody());
    }

    @Test
    void testUpdateUser_duplicateUsername() {
        // 사용자 두 명 등록
        restTemplate.postForEntity("/users", new User("user1", "1234"), User.class);
        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", new User("user2", "5678"), User.class);
        Long id = createResponse.getBody().getId();

        // user2의 username을 user1으로 변경 시도
        User updatedUser = new User("user1", "9999");
        HttpEntity<User> request = new HttpEntity<>(updatedUser);
        ResponseEntity<String> response = restTemplate.exchange("/users/" + id, HttpMethod.PUT, request, String.class);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Username already exists", response.getBody());
    }

    /// delete
    @Test
    void testDeleteUserById_success() {
        // 사용자 등록
        User user = new User("testuser", "1234");
        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", user, User.class);
        Long id = createResponse.getBody().getId();

        // 사용자 삭제
        ResponseEntity<Void> response = restTemplate.exchange("/users/" + id, HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatusCode().value());

        // 삭제 후 조회 시 404 확인
        ResponseEntity<String> getResponse = restTemplate.getForEntity("/users/" + id, String.class);
        assertEquals(404, getResponse.getStatusCode().value());
    }

    @Test
    void testDeleteUserById_notFound() {
        ResponseEntity<String> response = restTemplate.exchange("/users/999", HttpMethod.DELETE, null, String.class);
        assertEquals(404, response.getStatusCode().value());
        assertEquals("User not found with id: 999", response.getBody());
    }

    @Test
    void fail() {
        fail();
    }
}