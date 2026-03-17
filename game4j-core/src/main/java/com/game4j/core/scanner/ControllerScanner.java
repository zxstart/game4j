package com.game4j.core.scanner;

import com.game4j.core.annotation.G4JController;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制器扫描器
 * <p>
 * 使用 ClassGraph 库扫描指定包下的所有带有 {@link G4JController} 注解的类。
 * </p>
 */
public class ControllerScanner {
    /**
     * 扫描包并返回控制器类列表
     *
     * @param basePackage 基础包名
     * @return 控制器类列表
     */
    public static List<Class<?>> scan(String basePackage) {
        List<Class<?>> result = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().enableAnnotationInfo().acceptPackages(basePackage).scan()) {
            ClassInfoList controllers = scanResult.getClassesWithAnnotation(G4JController.class.getName());
            for (ClassInfo classInfo : controllers) {
                result.add(classInfo.loadClass());
            }
        }
        return result;
    }

}
