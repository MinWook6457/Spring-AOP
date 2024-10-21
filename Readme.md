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
```
import java.lang.annation.*;
 
@Documented
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 어노테이션 사용가능(리플렉션)
@Target( ElementType.TYPE, ElementType.FILED )
public @interface FunctionalInterface{...}
```




