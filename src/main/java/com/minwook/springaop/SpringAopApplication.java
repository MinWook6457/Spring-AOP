package com.minwook.springaop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import com.minwook.springaop.user.User;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // JDBC 자동 설정 제외
public class SpringAopApplication {

    public static void main(String[] args) throws
            ClassNotFoundException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException,
            NoSuchMethodException,
            NoSuchFieldException {

        // User 클래스를 동적으로 로드
        Class<User> userClass = (Class<User>) Class.forName("com.minwook.springaop.user.User");

        // 문자열 3개를 받는 생성자를 가져옴
        Constructor<User> constructor = userClass.getConstructor(String.class, String.class, String.class);
        // 생성자를 사용해 새로운 User 객체를 생성 (매개변수 전달)
        User user = constructor.newInstance("생성자에 매개변수 문자열값", "name", "1234");

        // 'name' 필드를 가져옴
        Field nameField = User.class.getDeclaredField("name");
        nameField.setAccessible(true); // 접근 제어자를 무시하고 필드에 접근 가능하게 설정

        // static 필드일 경우
        String classMember = (String) nameField.get(null); // static 필드 값을 가져옴

        // 인스턴스 필드일 경우, 생성한 'user' 인스턴스를 사용하여 값 가져오기
        String name = (String) nameField.get(user); // 'user' 인스턴스에서 'name' 값 가져오기

        // 첫 번째 생성자를 사용하는 경우:
        User user1 = new User("Alice", 25);

        // 두 번째 생성자를 사용하는 경우 (세 번째 문자열이 숫자를 나타냄):
        User user2 = new User("Bob", "LastName", "30");

        // 값 출력
        System.out.println(user1.getName() + ", " + user1.getAge()); // Alice, 25
        System.out.println(user2.getName() + ", " + user2.getAge()); // Bob, 30


        SpringApplication.run(SpringAopApplication.class, args); // Spring 애플리케이션 실행
    }
}
