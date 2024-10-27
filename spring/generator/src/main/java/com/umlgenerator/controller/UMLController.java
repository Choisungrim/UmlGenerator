package com.umlgenerator.controller;

import com.umlgenerator.annotation.PlantUML;
import com.umlgenerator.generator.UMLGenerator;
import com.umlgenerator.annotation.PlantUML;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/UML")
@Tag(name = "UML management", description = "UML 자동생성 API")
@PlantUML
public class UMLController {

    private final UMLGenerator umlGenerator;

    @Autowired
    private ApplicationContext applicationContext;

    private final String PLANT_UML_SERVER = "http://www.plantuml.com/plantuml/png/";

    public UMLController(ApplicationContext applicationContext, UMLGenerator umlGenerator) {
        this.umlGenerator = umlGenerator;
    }

    @GetMapping("/api/application-context")
    public Map<String, Object> getApplicationContextInfo() {
        Map<String, Object> contextInfo = new HashMap<>();

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Class<?> beanClass = applicationContext.getBean(beanName).getClass();

            // 필드 정보
            if (beanClass.isAnnotationPresent(PlantUML.class)) {
                Map<String, Object> beanInfo = new HashMap<>();
                beanInfo.put("className", beanClass.getName());

                // 필드 정보 수집
                Field[] fields = beanClass.getDeclaredFields();
                Map<String, String> fieldInfo = new HashMap<>();
                for (Field field : fields) {
                    fieldInfo.put(field.getName(), field.getType().getSimpleName());
                }
                beanInfo.put("fields", fieldInfo);

                // 메서드 정보 수집
                Method[] methods = beanClass.getDeclaredMethods();
                Map<String, String> methodInfo = new HashMap<>();
                for (Method method : methods) {
                    methodInfo.put(method.getName(), method.getReturnType().getSimpleName());
                }
                beanInfo.put("methods", methodInfo);

                // 인터페이스 정보 수집
                Class<?>[] interfaces = beanClass.getInterfaces();
                Map<String, String> interfaceInfo = new HashMap<>();
                for (Class<?> iface : interfaces) {
                    interfaceInfo.put(iface.getSimpleName(), iface.getName());
                }
                beanInfo.put("interfaces", interfaceInfo);

                contextInfo.put(beanName, beanInfo);
            }
        }

        return contextInfo; // 빈 정보를 JSON 형태로 반환
    }


    @GetMapping("/generate-uml/app")
    public ResponseEntity<String> generateUMLAPP(@RequestParam String appUrl) {
        String umlCode = umlGenerator.generateRestTargetUML(appUrl);
        System.out.println("Generated UML Code: \n" + umlCode); // 생성된 UML 코드 로그

        // 2. 생성된 UML 코드를 문자열 형태로 반환
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN) // MIME 타입을 텍스트로 설정
                .body(umlCode); // 생성된 UML 코드를 응답 본문으로 반환
    }
    private String encodeUml(String umlCode) {
        // UML 코드를 Base64로 인코딩
        return java.util.Base64.getEncoder().encodeToString(umlCode.getBytes());
    }


    private void generatePlantUML(String umlCode) {
        // UML 코드를 파일에 저장하거나 다른 처리를 할 수 있습니다.
        System.out.println(umlCode); // 예시로 콘솔에 출력합니다.
    }
}

