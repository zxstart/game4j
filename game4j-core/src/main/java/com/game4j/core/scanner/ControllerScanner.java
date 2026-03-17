package com.game4j.core.scanner;

import com.game4j.core.annotation.GameController;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class ControllerScanner {
    public static List<Class<?>> scan(String basePackage) {
        List<Class<?>> result = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().enableAnnotationInfo().acceptPackages(basePackage).scan()) {
            ClassInfoList controllers = scanResult.getClassesWithAnnotation(GameController.class.getName());
            for (ClassInfo classInfo : controllers) {
                result.add(classInfo.loadClass());
            }
        }
        return result;
    }

}
