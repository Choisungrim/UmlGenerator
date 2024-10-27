package com.umlgenerator.generator;

import com.umlgenerator.entity.ClassInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class UMLGenerator {

    private final RestTemplate restTemplate;

    public UMLGenerator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateUML(ApplicationContext applicationContext, String basePackage) {
        StringBuilder umlCode = new StringBuilder();
        umlCode.append("@startuml\n");

        Set<String> classNames = new HashSet<>();

        // 빈으로 등록된 클래스 가져오기
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();

            // 클래스가 지정된 패키지에 속하는지 확인하고 @SpringBootApplication 어노테이션이 없는지 확인
            if (clazz.getPackageName().startsWith(basePackage) && !isSpringBootApplication(clazz)) {
                // UML 코드에 클래스 선언 추가
                umlCode.append("class ").append(clazz.getSimpleName());

                // 클래스에 속한 인터페이스가 있는 경우
                if (clazz.getInterfaces().length > 0) {
                    umlCode.append(" <<").append(clazz.getInterfaces()[0].getSimpleName()).append(">>");
                }

                umlCode.append(" {\n");

                // 필드 추가
                for (Field field : clazz.getDeclaredFields()) {
                    String accessModifier = getAccessModifier(field.getModifiers());
                    umlCode.append("    ").append(accessModifier).append(field.getType().getSimpleName()).append(" ").append(field.getName()).append("\n");
                }

                // 클래스의 모든 메서드 추가
                addMethodsToUML(umlCode, clazz);

                umlCode.append("}\n");
                classNames.add(clazz.getSimpleName());
            }
        }

        // 인터페이스 메서드 별도 추가
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();

            if (clazz.getPackageName().startsWith(basePackage) && !isSpringBootApplication(clazz)) {
                for (Class<?> iface : clazz.getInterfaces()) {
                    umlCode.append("interface ").append(iface.getSimpleName()).append(" {\n");
                    addMethodsToInterfaceUML(umlCode, iface);
                    umlCode.append("}\n");
                }
            }
        }

        // 관계 설정
        umlCode.append(generateRelationships(applicationContext, basePackage, classNames));

        umlCode.append("@enduml\n");
        return umlCode.toString();
    }

    public String generateRestTargetUML(String applicationAUrl) {
        // REST API 호출로 애플리케이션 컨텍스트 정보를 가져옴
        Map<String, Object> contextInfo = getApplicationContextInfo(applicationAUrl);

        StringBuilder umlCode = new StringBuilder();
        umlCode.append("@startuml\n");

        // 클래스 정보를 저장하기 위한 Map
        Map<String, ClassInfo> classInfoMap = new HashMap<>();

        contextInfo.forEach((beanName, beanInfo) -> {
            Map<String, String> fields = (Map<String, String>) ((Map<?, ?>) beanInfo).get("fields");
            Map<String, String> methods = (Map<String, String>) ((Map<?, ?>) beanInfo).get("methods");
            Map<String, String> interfaces = (Map<String, String>) ((Map<?, ?>) beanInfo).get("interfaces");

            ClassInfo classInfo = new ClassInfo(beanName, fields, methods, interfaces);
            classInfoMap.put(beanName, classInfo);
        });

        // UML 코드 생성
        classInfoMap.forEach((beanName, classInfo) -> {
            umlCode.append("class ").append(classInfo.getName());

            // 인터페이스가 있는 경우
            if (!classInfo.getInterfaces().isEmpty()) {
                umlCode.append(" <<").append(classInfo.getInterfaces().keySet().iterator().next()).append(">>");
            }

            umlCode.append(" {\n");

            // 필드 추가
            classInfo.getFields().forEach((fieldName, fieldType) -> {
                umlCode.append("    ").append(fieldType).append(" ").append(fieldName).append("\n");
            });

            // 메서드 추가
            classInfo.getMethods().forEach((methodName, returnType) -> {
                umlCode.append("    ").append(returnType).append(" ").append(methodName).append("()\n");
            });

            umlCode.append("}\n");
        });

        // 관계 설정
        classInfoMap.forEach((beanName, classInfo) -> {
            classInfo.getFields().forEach((fieldName, fieldType) -> {
                umlCode.append(beanName).append(" --> ").append(fieldType).append("\n"); // 의존성 관계
            });
        });

        umlCode.append("@enduml\n");
        return umlCode.toString(); // 생성된 UML 코드 반환
    }

    private Map<String, Object> getApplicationContextInfo(String applicationAUrl) {
        return restTemplate.getForObject(applicationAUrl, Map.class);
    }


    public String generateUMLFromHtml(String html) {
        StringBuilder umlCode = new StringBuilder();
        umlCode.append("@startuml\n");

        // HTML을 파싱하여 UML 코드 생성
        // 예: div, button 등 HTML 요소를 클래스로 변환
        Document doc = Jsoup.parse(html);
        doc.select("*").forEach(element -> {
            umlCode.append("class ").append(element.tagName()).append(" {\n");

            // 요소의 속성 추가
            element.attributes().forEach(attr -> {
                umlCode.append("    ").append(attr.getKey()).append(": ").append(attr.getValue()).append("\n");
            });

            umlCode.append("}\n");
        });

        umlCode.append("@enduml\n");
        return umlCode.toString();
    }

    private void addMethodsToUML(StringBuilder umlCode, Class<?> clazz) {
        // 클래스의 메서드 추가
        for (Method method : clazz.getDeclaredMethods()) {
            // 접근 제어자만 추가
            String accessModifier = getAccessModifier(method.getModifiers());

            // 메서드 이름 처리: '-lambda$' 제거
            String methodName = method.getName();
            if (methodName.startsWith("lambda$")) {
                continue;
            }

            StringBuilder params = new StringBuilder();
            for (Class<?> paramType : method.getParameterTypes()) {
                if (params.length() > 0) {
                    params.append(", ");
                }
                params.append(paramType.getSimpleName());
            }

            // 반환 타입 처리
            String returnType = method.getReturnType().getSimpleName();
            if (returnType.equals("Map")) {
                // Map의 generic 타입 추출
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericReturnType;
                    Type[] actualTypeArguments = paramType.getActualTypeArguments();
                    String keyType = actualTypeArguments[0].getTypeName();
                    String valueType = actualTypeArguments[1].getTypeName();

                    // 마지막 '.' 이후의 값만 표시
                    keyType = keyType.substring(keyType.lastIndexOf('.') + 1);
                    valueType = valueType.substring(valueType.lastIndexOf('.') + 1);
                    returnType = "Map<" + keyType + ", " + valueType + ">";
                }
            }

            // 접근 제어자와 수정된 메서드명, 매개변수, 반환 타입 추가
            umlCode.append("    ").append(accessModifier).append(methodName)
                    .append("(").append(params).append("): ").append(returnType).append("\n");
        }
    }


    private void addMethodsToInterfaceUML(StringBuilder umlCode, Class<?> iface) {
        // 인터페이스의 메서드 추가
        for (Method method : iface.getDeclaredMethods()) {
            // 접근 제어자만 추가
            String accessModifier = getAccessModifier(method.getModifiers());

            // 메서드 이름
            String methodName = method.getName();

            // 매개변수 처리
            StringBuilder params = new StringBuilder();
            for (Class<?> paramType : method.getParameterTypes()) {
                if (params.length() > 0) {
                    params.append(", ");
                }
                params.append(paramType.getSimpleName());
            }

            // 반환 타입 처리
            String returnType = method.getReturnType().getSimpleName();
            if (returnType.equals("Map")) {
                // Map의 generic 타입 추출
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericReturnType;
                    Type[] actualTypeArguments = paramType.getActualTypeArguments();
                    String keyType = actualTypeArguments[0].getTypeName();
                    String valueType = actualTypeArguments[1].getTypeName();

                    // 마지막 '.' 이후의 값만 표시
                    keyType = keyType.substring(keyType.lastIndexOf('.') + 1);
                    valueType = valueType.substring(valueType.lastIndexOf('.') + 1);
                    returnType = "Map<" + keyType + ", " + valueType + ">";
                }
            }

            // 접근 제어자와 수정된 메서드명, 매개변수, 반환 타입 추가
            umlCode.append("    ").append(accessModifier).append(methodName)
                    .append("(").append(params).append("): ").append(returnType).append("\n");
        }
    }


    private String generateRelationships(ApplicationContext applicationContext, String basePackage, Set<String> classNames) {
        StringBuilder relationshipCode = new StringBuilder();

        // 관계 설정 (상속 및 구현)
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();

            if (clazz.getPackageName().startsWith(basePackage) && !isSpringBootApplication(clazz)) {
                // 상속 관계
                Class<?> superclass = clazz.getSuperclass();
                if (superclass != null && classNames.contains(superclass.getSimpleName())) {
                    relationshipCode.append(superclass.getSimpleName()).append(" <|-- ").append(clazz.getSimpleName()).append("\n");
                }

                // 인터페이스 구현 관계
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> iface : interfaces) {
                    if (iface.getPackageName().startsWith(basePackage)) {
                        relationshipCode.append(clazz.getSimpleName()).append(" ..|> ").append(iface.getSimpleName()).append("\n");
                    }
                }

                // 의존 관계 추가
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getType().getPackageName().startsWith(basePackage)) {
                        relationshipCode.append(clazz.getSimpleName()).append(" --> ").append(field.getType().getSimpleName()).append("\n");
                    }
                }
            }
        }

        return relationshipCode.toString();
    }

    private boolean isSpringBootApplication(Class<?> clazz) {
        // @SpringBootApplication 어노테이션 확인
        return clazz.isAnnotationPresent(SpringBootApplication.class);
    }

    private String getAccessModifier(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "+";
        } else if (Modifier.isProtected(modifiers)) {
            return "#";
        } else if (Modifier.isPrivate(modifiers)) {
            return "-";
        } else {
            return "~"; // package-private
        }
    }
}
