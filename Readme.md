### 바이트코드 조작(리플렉션, 다이나믹 프록시, 애노테이션 프로세서)

```
Spring에서 다양한 설정 기능들(@Autowired) 등은 코드 없이 객체를 주입하고, 조작하며 Lombok같은 어노테이션 기반 라이브러리는 어떻게 동작할까?
```

### 이를 알기 위해 선 지식으로 필요한 것이 바이트코드이다.
바이트코드란 JVM이 이해할 수 있는 언어로 변환된 자바 소스 코드이다. 자바 컴파일러에 의해 변환되는 코드 명령어의 크기가 1바이트라서 자바 바이트 코드라 불린다. 이런 자바 바이트 코드의 확장자는 .class 이다.

### 리플렉션
Class 정보를 담는 객체를 이용해 해당 클래스의 정보를 분석해내는 프로그래밍 기법
즉, 구체적인 클래스 타입, 정보를 알지 못하더라도 객체 이름으로 런타임 시점에서 리플렉션 객체의 바이트코드를 이용해 원하는 객체와 메서드를 사용할 수 있다.

동적 바인딩이 제공되지 않던 자바에서 리플렉션 기법을 이용해 런타임에 동적인 바인딩을 제공한 것이다.
자바의 Object 객체는 Object.Class 라는 리플렉션 객체를 가지고 있는데, 컴파일 타임에 클래스의 이름을 알 수 있다면 Object.Class 객체를 어들 수 있다.

```
    /**
     * Returns the runtime class of this {@code Object}. The returned
     * {@code Class} object is the object that is locked by {@code
     * static synchronized} methods of the represented class.
     *
     * <p><b>The actual result type is {@code Class<? extends |X|>}
     * where {@code |X|} is the erasure of the static type of the
     * expression on which {@code getClass} is called.</b> For
     * example, no cast is required in this code fragment:</p>
     *
     * <p>
     * {@code Number n = 0;                             }<br>
     * {@code Class<? extends Number> c = n.getClass(); }
     * </p>
     *
     * @return The {@code Class} object that represents the runtime
     *         class of this object.
     * @jls 15.8.2 Class Literals
     */
    public final native Class<?> getClass();
```

Object에 선언된 getClass의 정의는 위와 같다. GPT에게 던져보자.
```
getClass() 메서드는 메서드가 호출된 객체의 런타임 클래스를 반환합니다.
반환되는 값은 해당 객체의 런타임 클래스를 나타내는 Class<?> 객체입니다.
동기화 관련 설명:

주석에서는 getClass()로 반환된 Class 객체가 해당 클래스의 static synchronized 메서드에서 잠금(락)을 걸 때 사용된다고 설명합니다. 이는 Java의 동기화 메커니즘을 설명하는 것으로, static synchronized 메서드는 클래스의 Class 객체를 기준으로 동기화가 이루어집니다.
결과 타입:

실제 반환되는 타입은 Class<? extends X>로, 여기서 X는 getClass()가 호출된 객체의 정적 타입의 소거(erasure)입니다.

즉, 반환된 값은 런타임 시점의 하위 클래스의 Class 객체입니다. 객체가 상위 클래스나 인터페이스 타입으로 선언되어 있어도 런타임에 해당하는 실제 클래스의 Class 객체가 반환됩니다.

예시에서 Number n = 0;이라는 코드에서 n은 정적으로 Number 타입으로 선언되었지만, 실제로는 런타임에 Integer 타입입니다. 이때 n.getClass()를 호출하면 런타임 클래스인 Integer.class가 반환되며, 캐스트(cast)가 필요하지 않습니다.
... (생략)
```

이를 사용하는 응용하는 코드는 아래와 같다.
```java
public class App {
    public static void main(String[] args) {
        Class<User> userClass = User.class;
        User user = new User("test", "minwook, "1234"); // 테스트용 인스턴스
        
        Arrays.stream(userClass.getDeclaredFields())
                .forEach(filed -> {// User.class에 정의된 필드를 모두 가져온다.
                    try {
                        filed.setAccessible(true); // private 접근제어자 무시
                        // filed.get(인스턴스)로 해당 인스턴스의 데이터를 읽어서 반환함.
                        // 만약 .get(null)을 한다면 static인 클래스 데이터를 반환함.
                        System.out.println(filed + " " + filed.get(user));
                        
                    } catch (IllegalAccessException e) {
                        // 잘못된 접근, 해당 메서드가 없는 경우
                    }
                });
    }
}
```

위 코드를 통해서 해당 클래스의 정보를 얻거나, 컴파일 이후 런타임에 동적으로 인스턴스를 만들 수 있다.

### 리플렉션 && 어노테이션
Class<T>에는 getAnnotation() 이라는 메소드가 존재한다. 해당 메소드를 활용하여 콘솔을 찍거나 해봐도 아무것도 찍히지 않을 것이다.
그 이유는 어노테이션은 '주석'과 동일한 취급을 받기 때문이다. 어노테이션에 대한 정보는 자바 가상 머신이 읽어들인다.
다시 말해, 자바 코드와 바이트코드까지는 남아있지만 해당 바이트코드가 읽혀진 후 런타임 메모리에는 어노테이션에 대한 그 어떠한 정보도 남아있지 않는다.

다만 자바에서는 코드에서 리플렉션을 사용할 수 있도록 어노테이션을 정의할 때 생명주기를 런타임까지 남아있도록 설정할 수 있다. 만약 Class.getAnnotation()을 사용하고 싶다면 아뢔와 같이 어노테이션을 생성한다.
```java
import java.lang.annation.*;
 
@Documented
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 어노테이션 사용가능(리플렉션)
@Target( ElementType.TYPE, ElementType.FILED )
public @interface FunctionalInterface{...}
```

### 리플렉션 API - 인스턴스 생성, 클래스 수정

스프링 부트 메인 클래스에서 테스트 하기위해 아래와 같이 코드를 작성해보았다.
```java
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

        SpringApplication.run(SpringAopApplication.class, args); // Spring 애플리케이션 실행
    }
}

```

그 결과 아래와 같은 이슈가 발생하였다.
```md
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "o" is null
	at java.base/jdk.internal.reflect.UnsafeFieldAccessorImpl.ensureObj(UnsafeFieldAccessorImpl.java:57)
	at java.base/jdk.internal.reflect.UnsafeObjectFieldAccessorImpl.get(UnsafeObjectFieldAccessorImpl.java:36)
```

NullPointerException 발생 원인은 UnsafeFieldAccessorImpl에서 Object.getClass()를 호출하려고 할 때, 해당 객체(o)가 null인 경우이다.
이 문제는 반사(reflection)로 필드에 접근할 때 객체가 null이어서 발생한다.

#### 아래 코드에서 발생
```java
String classMember = (String) nameField.get(null); // static 필드 값 가져옴

```
nameField.get(null)이 null인 이유는 name 필드가 static 필드가 아니기 때문이다. get(null)은 static 필드에만 적용된다. 그러나 name 필드는 User 인스턴스의 필드이므로, 인스턴스에서 값을 가져와야 한다.

#### 해결 방법
name 필드가 static이 아닌 인스턴스 필드이므로, 아래처럼 인스턴스(user)에서 필드 값을 가져와야 한다.

```java
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

        // 인스턴스 필드일 경우, 생성한 'user' 인스턴스를 사용하여 값 가져오기
        String name = (String) nameField.get(user); // 'user' 인스턴스에서 'name' 값 가져오기

        SpringApplication.run(SpringAopApplication.class, args); // Spring 애플리케이션 실행
    }
}

```

### 커스텀 어노테이션 생성 과 AOP
리플렉션까지 맛을 봤으니 나만의 어노테이션을 생성하고 AOP를 주입해보자.

```java
package com.minwook.springaop.annotation;


import java.lang.annotation.*;

// 어노테이션은 메서드에 적용되고 런타임까지 유지됨
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationTest {
    //
}
```
* Documented : JavaDoc 같은 문서화 도구에 포함되도록 함, @Documented가 없으면 어노테이션 자체는 코드에 적용되지만, 문서화할 때는 표시되지 않는다.
* Retention : 어노테이션의 생명 주기 결정
  * SOURCE : 컴파일 단계까지만 어노테이션이 유지
  * CLASS : 컴파일된 .class 파일에는 포함되지만, 런타임 시에는 JVM에 의해 읽을 수 없다: 기본값
  * RUNTIME : 어노테이션이 컴파일된 .class 파일에 포함되며, 런타임에도 JVM에 의해 유지되고 반영
    * 리플렉션을 사용하여 어노테이션 정보를 런타임에 접근할 수 있음

어노테이션을 생성하고 나서 로킹 클래스를 정의할 것이다.

### 그전에 AOP에 정확히 뭔지 알아보자 (공식문서 발췌)
```
스프링의 AOP(Aspect Oriented Programming)

AOP(관점 지향 프로그래밍)는 OOP(객체 지향 프로그래밍)를 보완하며 프로그램 구조를 생각하는 또 다른 방법을 제공합니다. 

OOP에서 핵심적인 모듈 단위는 클래스인 반면, AOP에서는 모듈 단위가 관점(aspect)입니다.
 
관점은 여러 타입과 객체에 걸쳐 공통으로 적용되는 관심사(예: 트랜잭션 관리)를 모듈화할 수 있게 해줍니다. (이러한 관심사를 AOP 문헌에서는 "횡단 관심사"라고 부릅니다.)

스프링의 핵심 구성 요소 중 하나가 바로 AOP 프레임워크입니다. 

스프링 IoC 컨테이너는 AOP에 의존하지 않기 때문에, AOP를 사용하지 않아도 스프링을 사용할 수 있습니다. 

하지만 AOP는 스프링 IoC를 보완하여 매우 강력한 미들웨어 솔루션을 제공합니다.
```

스프링에서는 어노테이션을 통해 AOP 구현을 쉽게 할 수 있도록 해준다. 

```java
package com.minwook.springaop.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    // LogExecutionTime 어노테이션이 붙은 메서드에 대해 AOP 처리
    @Around("@annotation(com.minwook.springaop.annotation.AnnotationTest)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object proceed = joinPoint.proceed(); // 실제 메서드 실행

        long executionTime = System.currentTimeMillis() - start;

        System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
        return proceed;
    }
}
```

위 코드를 단계 별로 분석해보자.

#### Aspect && Component
* @Aspect: 이 클래스가 AOP에서 사용할 Aspect임을 나타낸다.  Aspect는 공통 관심사를 모듈화하여 특정 타겟 메서드에 적용하는 역할을 한다.
* @Component: 해당 클래스를 Spring 빈으로 등록

* @Around 어노테이션 
  * @Around: AOP에서 타겟 메서드가 호출되기 전후로 특정 로직을 실행할 수 있게 한다 
  * @Around는 메서드 실행의 시작과 끝을 감싸서 실행되므로, 메서드의 실행 전과 후에 각각 로직을 추가할 수 있다.
  * @annotation(com.minwook.springaop.annotation.AnnotationTest)
    * @AnnotationTest라는 어노테이션이 붙은 메서드들에 대해 logExecutionTime 메서드를 적용하겠다는 의미이다.
    * 즉, @AnnotationTest 어노테이션이 선언된 메서드에 대해서만 이 AOP 로직이 실행된다.

#### ProceedingJoinPoint && 메서드
* ProceedingJoinPoint: AOP에서 현재 실행되는 메서드에 대한 정보(메서드 이름, 매개변수, 대상 객체 등)를 제공하는 객체이다.  proceed() 메서드를 호출하면 실제 타겟 메서드가 실행된다.
* long start = System.currentTimeMillis(); : 메서드 실행이 시작되기 직전의 시간을 기록한다.
* Object proceed = joinPoint.proceed(); : 타겟 메서드를 실행하고 이때 타겟 메서드의 반환 값을 proceed에 저장한다.
* return proceed; : 타겟 메서드 값 반환, 해당 부분이 없다면 타겟 메서드의 반환값이 호출자에게 전달되지 않기 때문에 반드시 거쳐야 하는 작업이다.

이를 테스트하기 위해 메인 클래스를 다음과 같이 정의하였다.
```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // JDBC 자동 설정 제외
public class SpringAopApplication {

    public static void main(String[] args)  {
        SpringApplication.run(SpringAopApplication.class, args); // Spring 애플리케이션 실행
    }

    @Bean
    CommandLineRunner run(AopService aopService) {
        return args -> {
            try {
                aopService.executeTest();  // AOP가 적용된 메서드 호출
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

}
```

어플리케이션을 실행하고 나서 결과는 아래와 같다.
```
Executing test method...
void com.minwook.springaop.aop.service.AopService.executeTest() executed in 1007ms
```

### AOP에서 사용되는 용어 파악하기
* Aspect : 공통 관심사(Cross-cutting corncern)을 모듈화 한 것
* Target : Aspect(공통관심사)를 적용할 핵심로직을 가진 객체
* Weaving - Aspect 를 대상 객체(Target)에 연결시켜 AOP 객체로 만드는 바이트 코드 및 객체 조작과정

* Advice : Aspect(공통관심사)의 동작을 적은 것. Aspect의 구현체 (객체의 메서드조작)
  * Join Point : Advice가 적용되는 시점. (메서드 실행, 생성자 호출, 필드 값 변경같은 특수한 실행 시점)    
    * return joinPoint.proceed();
  * Pointcut : [Join Point]의 정규식 => 실행 스펙, Pointcut이 일치하는 객체들에게 Aspect 적용(조건문)
    * @Pointcut("execution(* hello(..))") 

